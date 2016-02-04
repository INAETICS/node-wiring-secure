/*
 * Some things taken from:
 * http://curl.haxx.se/libcurl/c/http-post.html
 * http://stackoverflow.com/questions/2329571/c-libcurl-get-output-into-a-string
 */
#include "../include/caclient_certificaterequester.h"
#include <stdio.h>
#include <string.h>
#include <curl/curl.h>
#include "../include/caclient_caresponse.h"
#include "../include/caclient_csr_generator.h"

/**
 * String init.
 */
void init_string(struct string *s) {
    s->len = 0;
    s->ptr = malloc(s->len+1);
    if (s->ptr == NULL) {
        fprintf(stderr, "malloc() failed\n");
        exit(EXIT_FAILURE);
    }
    s->ptr[0] = '\0';
}

/**
 * The write function for the curl response.
 */
size_t writefunc(void *ptr, size_t size, size_t nmemb, struct string *s)
{
    size_t new_len = s->len + size*nmemb;
    s->ptr = realloc(s->ptr, new_len+1);
    if (s->ptr == NULL) {
        fprintf(stderr, "realloc() failed\n");
        exit(EXIT_FAILURE);
    }
    memcpy(s->ptr+s->len, ptr, size*nmemb);
    s->ptr[new_len] = '\0';
    s->len = new_len;

    return size*nmemb;
}


/**
 * Main function.
 */
int request_certificate(void)
{
    CURL *curl;
    CURLcode res;

    /* In windows, this will init the winsock stuff */
    curl_global_init(CURL_GLOBAL_ALL);

    /* get a curl handle */
    curl = curl_easy_init();
    if(curl) {
        struct string s;
        init_string(&s);

        /* First set the URL that is about to receive our POST. This URL can
           just as well be a https:// URL if that is what should receive the
           data. */
        curl_easy_setopt(curl, CURLOPT_URL, "http://localhost:8888/api/v1/cfssl/sign");
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writefunc);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, &s);

        unsigned char* csr = (unsigned char*) generate_csr();

        /* Now specify the POST data */
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, csr);

        /* Perform the request, res will get the return code */
        res = curl_easy_perform(curl);

        /* Check for errors
         * TODO: NOT WORKING CURRENTLY*/
        if(res != CURLE_OK) {
            fprintf(stderr, "curl_easy_perform() failed: %s\n", curl_easy_strerror(res));
        } else {
            //Get the response
            certresponse* cert = parse_certificate_response(s.ptr);

            //Use the certificate
            printf("%s", cert->certificate);
            fflush(stdout);

            //Clean up!
            free(csr);
            free(cert);
            free(s.ptr);
        }

        /* always cleanup */
        curl_easy_cleanup(curl);
    }
    curl_global_cleanup();
    return 0;
}