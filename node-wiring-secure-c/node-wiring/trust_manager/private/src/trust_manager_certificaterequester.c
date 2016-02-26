#if defined(MBEDTLS_PLATFORM_C)
#include "mbedtls/platform.h"
#else

#include <stdio.h>

#define mbedtls_fprintf    fprintf
#define mbedtls_printf     printf
#endif

#define CFSSL_SIGN_API_URL "http://localhost:8888/api/v1/cfssl/sign"
#define CFSSL_CA_CERT_URL "http://localhost:8888/api/v1/cfssl/info"
#define REFRESH_EARLY_THRESHOLD_SECONDS 30

#define JSON_TYPE_HEADER "Content-Type: application/json"

#include "mbedtls/entropy.h"
#include "mbedtls/ctr_drbg.h"
#include "mbedtls/net.h"
#include "mbedtls/ssl.h"
#include "mbedtls/x509.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

#include <curl/curl.h>

#include "trust_manager_certificaterequester.h"
#include "trust_manager_caresponse.h"
#include "trust_manager_keygen.h"

#define TRUST_STORAGE_ROOT = "/tmp/inaetics-celix-keys/"
#define TRUST_CA_PATH = "ca-crt/"

/**
 * String init.
 */
void init_string(struct string *s) {
    s->len = 0;
    s->ptr = malloc(s->len + 1);
    if (s->ptr == NULL) {
        fprintf(stderr, "malloc() failed\n");
        exit(EXIT_FAILURE);
    }
    s->ptr[0] = '\0';
}

/**
 * The write function for the curl response.
 */
size_t writefunc(void *ptr, size_t size, size_t nmemb, struct string *s) {
    size_t new_len = s->len + size * nmemb;
    s->ptr = realloc(s->ptr, new_len + 1);
    if (s->ptr == NULL) {
        fprintf(stderr, "realloc() failed\n");
        exit(EXIT_FAILURE);
    }
    memcpy(s->ptr + s->len, ptr, size * nmemb);
    s->ptr[new_len] = '\0';
    s->len = new_len;

    return size * nmemb;
}

/**
 * Http wrapper for the cfssl api.
 */
int cfssl_ca_certget_wrapper(char* certificate, char body[], char url[])
{
    CURL *curl;
    CURLcode res;
    curl_global_init(CURL_GLOBAL_ALL);

    /* get a curl handle */
    curl = curl_easy_init();
    if (curl) {
        struct string s;
        init_string(&s);

        // init curl
        struct curl_slist *head = NULL;
        head = curl_slist_append(head, JSON_TYPE_HEADER);
        curl_easy_setopt(curl, CURLOPT_URL, url);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writefunc);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &s);
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, head);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, body);

        res = curl_easy_perform(curl);

        /* Check for errors
         * TODO: If you get a status code 0, but the code fails, check here... */
        if (res != CURLE_OK) {
            fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
        } else {
            //Get the response
            certresponse *cert = parse_certificate_response(s.ptr);
            memcpy(certificate, cert->certificate, 4096);

            //Clean up!
            free(cert);
            free(s.ptr);
        }
        curl_easy_cleanup(curl);
    }
    curl_global_cleanup();
    return res;
}

/**
 * Retreive ca cert.
 */
int request_ca_certificate(char *ca_certificate)
{
    return cfssl_ca_certget_wrapper(ca_certificate, "{}", CFSSL_CA_CERT_URL);
}

/**
 * Get signed certificate from cfssl api
 */
int csr_get_certificate(mbedtls_pk_context *key, char *certificate)
{
    int res;
    // generate the csr
    char *csr = malloc(1024);
    generate_csr(key, csr);
    res = cfssl_ca_certget_wrapper(certificate, csr, CFSSL_SIGN_API_URL);
    free(csr);
    return res;
}


/**
 * Check if date of a cert is still valid.
 * Refresh_early_threshold specifies the time in seconds, that will make the validation fail early.
 * This way a certificate can be refreshed earlier, even though the current cert is still valid.
 */
bool cert_date_still_valid(mbedtls_x509_crt *certificate, int refresh_early_threshold) {
    double sec_diff;
    time_t now = time(NULL);
    struct tm certdate;
    struct tm now_date = *localtime(&now);

    // reinit with cert date
    certdate.tm_year = certificate->valid_to.year - 1900; // tm starts there
    certdate.tm_mon = certificate->valid_to.mon - 1; // tm uses 0-11
    certdate.tm_mday = certificate->valid_to.day;
    certdate.tm_hour = certificate->valid_to.hour;
    certdate.tm_min = certificate->valid_to.min;
    certdate.tm_sec = certificate->valid_to.sec + now_date.tm_gmtoff;
    certdate.tm_isdst = now_date.tm_isdst;
    sec_diff = difftime(now, mktime(&certdate));

    printf("\ncert is valid untill: %f", sec_diff);

    return (sec_diff <= (refresh_early_threshold * -1)) ? 0 : 1;
}

/**
 * Verify the certificate using the ca's cert. Returns 0 on success. Specify backdate threshold to faster get hold of a new cert.
 */
int verify_certificate(mbedtls_x509_crt *certificate, mbedtls_x509_crt *ca_cert, int backdate_threshold) {
    int ret;
    uint32_t flags;

    ret = mbedtls_x509_crt_verify(certificate, ca_cert, NULL, NULL, &flags, NULL, NULL);
    ret += cert_date_still_valid(certificate, REFRESH_EARLY_THRESHOLD_SECONDS);
    // TODO: remove; or uncomment for debugging output.
//    if (ret != 0) {
//        if (ret == MBEDTLS_ERR_X509_CERT_VERIFY_FAILED) {
//            char vrfy_buf[512];
//
//            mbedtls_printf(">CERT VERIFY INFO: failed:");
//            mbedtls_x509_crt_verify_info(vrfy_buf, sizeof(vrfy_buf), " ", flags);
//            mbedtls_printf("%s\n", vrfy_buf);
//        }
//        else {
//            mbedtls_printf(">CERT VERIFY INFO: failed\n  !  mbedtls_x509_crt_verify returned %d\n\n", ret);
//        }
//    }

    return ret;
}

/**
 * Print info about the cert.
 */
void print_certificate_info(mbedtls_x509_crt *certificate) {
    char buf[1024];
    mbedtls_x509_crt_info(buf, 1024, ">CERT INFO: ", certificate);
    mbedtls_printf("%s", buf);
}

/**
 * Load the x509 certificate.
 */
int load_certificate(mbedtls_x509_crt *certificate, const char certfile[]) {
    int ret;
    struct mbedtls_x509_crt tmpCert;
    mbedtls_x509_crt_init(&tmpCert);

    ret = mbedtls_x509_crt_parse_file(&tmpCert, certfile);
    if (ret != 0) {
        mbedtls_printf(">CERT LOAD INFO: failed\n  !  mbedtls_x509_crt_parse_file returned %d\n\n", ret);
    } else {
        memcpy(certificate, &tmpCert, sizeof(tmpCert));
    }

    return (ret);
}