//
// Created by Martin Gaida on 12/8/15.
//
#include <mbedtls/rsa.h>
#include <stdbool.h>
#include <sys/file.h>

#include "trust_manager_keygen.h"

#if !defined(MBEDTLS_CONFIG_FILE)
#include "mbedtls/config.h"
#else
#include MBEDTLS_CONFIG_FILE
#endif

#if defined(MBEDTLS_PLATFORM_C)
#include "mbedtls/platform.h"
#else
#include <stdio.h>
#define mbedtls_printf     printf
#endif

#if defined(MBEDTLS_BIGNUM_C) && defined(MBEDTLS_ENTROPY_C) && \
    defined(MBEDTLS_RSA_C) && defined(MBEDTLS_GENPRIME) && \
    defined(MBEDTLS_FS_IO) && defined(MBEDTLS_CTR_DRBG_C)
#include "mbedtls/entropy.h"
#include "mbedtls/ctr_drbg.h"
#include "mbedtls/x509.h"
#include "mbedtls/x509_csr.h"

#include "mbedtls/error.h"

#include <string.h>

#endif

#define KEY_SIZE 2048
#define EXPONENT 65537

#define CSR_START_DEFAULT "-----BEGIN CERTIFICATE REQUEST-----"
#define CSR_START_CFSSL "-----BEGIN CERTIFICATE REQUEST-----\\n"
#define CSR_STOP_DEFAULT "-----END CERTIFICATE REQUEST-----"
#define CSR_STOP_CFSSL "\\n-----END CERTIFICATE REQUEST-----"
#define CSR_CFSSL_JSON "{\"certificate_request\":\"%s\"}"
#define CSR_CERT_SUBJECT_NAME "CN=%s,O=INAETICS,C=NL"

mbedtls_ctr_drbg_context ctr_drbg;

/**
 * Generates rsa mbedtls pk context keypair.
 */
int generate_keypair (mbedtls_pk_context* key)
{
    //Init rsa
    mbedtls_rsa_context* rsa = malloc(sizeof(mbedtls_rsa_context));
    int ret = 0;

    char buf[1024];
    mbedtls_entropy_context entropy;
//    mbedtls_ctr_drbg_context ctr_drbg;
    const char *pers = "gen_key";
//#if defined(MBEDTLS_ECP_C)
//    const mbedtls_ecp_curve_info *curve_info;
//#endif

    int res = 0;

    // initialize request as rsa
    mbedtls_rsa_init( rsa, MBEDTLS_RSA_PKCS_V15, 0 );

    mbedtls_ctr_drbg_init( &ctr_drbg );
    memset( buf, 0, sizeof( buf ) );

    // seed para random num gen
    mbedtls_entropy_init( &entropy );
    mbedtls_ctr_drbg_seed( &ctr_drbg, mbedtls_entropy_func, &entropy, (const unsigned char *) pers, strlen( pers ) );

    // initialize rsa
    mbedtls_pk_init( key );
    res = mbedtls_pk_setup( key, mbedtls_pk_info_from_type( MBEDTLS_PK_RSA ) );
    printf("\n  . result (%d): , %d", __LINE__, res);


    mbedtls_rsa_init( rsa, MBEDTLS_RSA_PKCS_V15, 0 );

    if( ( ret = mbedtls_rsa_gen_key( rsa, mbedtls_ctr_drbg_random, &ctr_drbg, KEY_SIZE, EXPONENT ) ) != 0 )
    {
        mbedtls_printf( " failed\n  ! mbedtls_rsa_gen_key returned %d\n\n", ret );
        goto exit;
    }

    int t1 = mbedtls_rsa_check_pub_priv( rsa, rsa );
    if ( t1 != 0 ) {
        return t1;
    }

    // set rsa parms into key config
    key->pk_ctx = rsa;
    key->pk_info = mbedtls_pk_info_from_type( MBEDTLS_PK_RSA );

    // clean up and exit
    exit:
#ifdef MBEDTLS_ERROR_C
    mbedtls_strerror( res, buf, sizeof( buf ) );
    mbedtls_printf( " - %s\n", buf );
#else
    mbedtls_printf("\n");
#endif

    mbedtls_entropy_free( &entropy );
    return ret;
}

/**
 * Gets the systems primary public ip for the Certificate SN / CN.
 */
int get_primary_public_up(char *ip)
{
    FILE *fp;
    char ip_buf[256];

    fp = popen("/bin/ip route get 1 | awk '{print $NF;exit}'", "r");
    if (fp == NULL) {
        printf("Failed to run command\n" );
        exit(1);
    }

    while (fgets(ip_buf, sizeof(ip_buf)-1, fp) != NULL) {
        sprintf(ip, "%s", ip_buf);
        removeChar(ip, '\n');
    }

    pclose(fp);

    return 0;
}

/**
 * Public key in standard pem char[] representation. Returns 0 on success.
 */
int get_public_key(mbedtls_pk_context* key, char* key_pair)
{
    int ret;
    unsigned char buffer_priv[4096];
    memset( buffer_priv, 0, 4096 );
    ret = mbedtls_pk_write_pubkey_pem( key, buffer_priv, sizeof(buffer_priv) );
    if( ret != 0 )
    {
        mbedtls_printf( " failed\n  !  mbedtls_pk_write_key_pem returned %d", ret );
    } else {
        memcpy(key_pair, buffer_priv, sizeof(buffer_priv));
    }
    return ret;
}

/**
 * private key in standard pem char[] representation. Returns 0 on success.
 */
int get_private_key(mbedtls_pk_context* key, char* private_key)
{
    int ret;
    unsigned char buffer_priv[4096];
    memset( buffer_priv, 0, 4096 );
    ret = mbedtls_pk_write_key_pem( key, buffer_priv, sizeof(buffer_priv) );
    if( ret != 0 )
    {
        mbedtls_printf( " failed\n  !  mbedtls_pk_write_key_pem returned %d", ret );
    } else {
        memcpy(private_key, buffer_priv, sizeof(buffer_priv));
    }
    return ret;
}

/**
 * Removes char in char array
 */
void removeChar(char *str, char garbage)
{
    char *src, *dst;
    for (src = dst = str; *src != '\0'; src++) {
        *dst = *src;
        if (*dst != garbage) dst++;
    }
    *dst = '\0';
}

/**
 * Write pem to file.
 */
int write_pem_to_file(char* pem, char filename[], bool append) {
    char *apnd = (append) ? "a" : "w";
    FILE *file = fopen(filename, apnd);
    flock(file, LOCK_EX);
//    if (flock(file, LOCK_SH) < 0) {
//        return 1;
//    } else {
        int results = fputs(pem, file);
        if (results == EOF) {
            return 1;
        }
//    }
    flock(file, LOCK_UN);
    fclose(file);
    return 0;
}

int rename_pem(char* old, char* new)
{
    return rename(old, new);
}

/**
 * Replaces pattern in char array.
 */
char * replace(char const * const original, char const * const pattern, char const * const replacement)
{
    size_t const replen = strlen(replacement);
    size_t const patlen = strlen(pattern);
    size_t const orilen = strlen(original);

    size_t patcnt = 0;
    const char * oriptr;
    const char * patloc;

    // find how many times the pattern occurs in the original string
    for (oriptr = original; patloc = strstr(oriptr, pattern); oriptr = patloc + patlen)
    {
        patcnt++;
    }

    {
        // allocate memory for the new string
        size_t const retlen = orilen + patcnt * (replen - patlen);
        char * const returned = (char *) malloc( sizeof(char) * (retlen + 1) );

        if (returned != NULL)
        {
            // copy the original string,
            // replacing all the instances of the pattern
            char * retptr = returned;
            for (oriptr = original; patloc = strstr(oriptr, pattern); oriptr = patloc + patlen)
            {
                size_t const skplen = patloc - oriptr;
                // copy the section until the occurence of the pattern
                strncpy(retptr, oriptr, skplen);
                retptr += skplen;
                // copy the replacement
                strncpy(retptr, replacement, replen);
                retptr += replen;
            }
            // copy the rest of the string.
            strcpy(retptr, oriptr);
        }
        return returned;
    }
}

/**
 * Writes a cfssl api compatible json csr. Returns 0 on success.
 */
int generate_csr(mbedtls_pk_context* key, char* csr)
{
    int res = 0;

    char ip[256];
    char *sn = malloc(512);
    get_primary_public_up(ip);
    sprintf(&sn, CSR_CERT_SUBJECT_NAME, ip);

    mbedtls_x509write_csr req;
    mbedtls_x509write_csr_init( &req );
    mbedtls_x509write_csr_set_md_alg( &req, MBEDTLS_MD_SHA256 );
    mbedtls_x509write_csr_set_key( &req, key );
    mbedtls_x509write_csr_set_key_usage( &req, MBEDTLS_X509_KU_KEY_ENCIPHERMENT );
    mbedtls_x509write_csr_set_ns_cert_type( &req, MBEDTLS_X509_NS_CERT_TYPE_SSL_CLIENT );
    mbedtls_x509write_csr_set_subject_name( &req, &sn );
    unsigned char buffer_csr[4096];
    res = mbedtls_x509write_csr_pem(&req, buffer_csr, 4096, mbedtls_ctr_drbg_random, &ctr_drbg );

    if( res != 0 )
    {
        mbedtls_printf( " failed\n  !  mbedtls_x509write_csr_pem returned %d", res );
    }

    char* temp_csr[4096];
    strncpy((char *) temp_csr, (const char*) &buffer_csr, 4096);
    removeChar((char *) temp_csr, '\n');

    strcpy(temp_csr, replace((char *) temp_csr, CSR_START_DEFAULT, CSR_START_CFSSL));
    strcpy(temp_csr, replace((char *) temp_csr, CSR_STOP_DEFAULT, CSR_STOP_CFSSL));
    sprintf(csr, CSR_CFSSL_JSON, (char *) temp_csr);

    mbedtls_x509write_csr_free( &req );
    mbedtls_ctr_drbg_free( &ctr_drbg );
    return res;
}

