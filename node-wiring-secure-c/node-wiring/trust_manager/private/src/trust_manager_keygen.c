//
// Created by Martin Gaida on 12/8/15.
//
#include <mbedtls/rsa.h>

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

/*
 * global options
 */
struct options
{
    const char *filename;       /* filename of the key file             */
    int debug_level;            /* level of debugging                   */
    const char *output_file;    /* where to store the constructed key file  */
    const char *subject_name;   /* subject name for certificate request */
    unsigned char key_usage;    /* key usage flags                      */
    unsigned char ns_cert_type; /* NS cert type                         */
} opt;

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

//    opt.filename            = "keyfile.key";
    opt.debug_level         = 0;
//    opt.output_file         = "cert.req";
    opt.subject_name        = "CN=Cert,O=INAETICS,C=NL";
    opt.key_usage           = MBEDTLS_X509_KU_KEY_ENCIPHERMENT;
    opt.ns_cert_type        = MBEDTLS_X509_NS_CERT_TYPE_SSL_CLIENT;

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
 * Public key in standard pem char[] representation. Returns 0 on success.
 */
int get_public_key(mbedtls_pk_context* key, char* key_pair)
{
    int ret;
    unsigned char buffer_priv[4096];
    memset( buffer_priv, 0, 4096 );
    ret = mbedtls_pk_write_key_pem( key, buffer_priv, sizeof(buffer_priv) );
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
    ret = mbedtls_pk_write_pubkey_pem( key, buffer_priv, sizeof(buffer_priv) );
    if( ret != 0 )
    {
        mbedtls_printf( " failed\n  !  mbedtls_pk_write_key_pem returned %d", ret );
    } else {
        memcpy(private_key, buffer_priv, sizeof(buffer_priv));
    }
    return ret;
}

void removeChar(char *str, char garbage) {

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
int write_pem_to_file(char* pem, char filename[])
{
    FILE *file = fopen(filename, "w");

    int results = fputs(pem, file);
    if (results == EOF) {
        return 1;
    }
    fclose(file);
}

/**
 * Writes a cfssl api compatible json csr. Returns 0 on success.
 */
int generate_csr(mbedtls_pk_context* key, char* csr)
{
    int res = 0;

    mbedtls_x509write_csr req;
    mbedtls_x509write_csr_init( &req );
    mbedtls_x509write_csr_set_md_alg( &req, MBEDTLS_MD_SHA256 );
    mbedtls_x509write_csr_set_key( &req, key );
    mbedtls_x509write_csr_set_key_usage( &req, opt.key_usage);
    mbedtls_x509write_csr_set_ns_cert_type( &req, opt.ns_cert_type);
    mbedtls_x509write_csr_set_subject_name( &req, opt.subject_name );

    unsigned char buffer_csr[4096];
    res = mbedtls_x509write_csr_pem(&req, buffer_csr, 4096, mbedtls_ctr_drbg_random, &ctr_drbg );

    if( res != 0 )
    {
        mbedtls_printf( " failed\n  !  mbedtls_x509write_csr_pem returned %d", res );
    }

    char* temp_csr[4096];
    char* csr_body[4102];
    strncpy((char *) temp_csr, (const char*) &buffer_csr[36], 923);
    removeChar((char *) temp_csr, '\n');
    sprintf((char *) csr_body, "-----BEGIN CERTIFICATE REQUEST-----\\n%s\\n-----END CERTIFICATE REQUEST-----", (char*) temp_csr);
    sprintf(csr, "{\"certificate_request\":\"%s\"}", (char *) csr_body);

    mbedtls_x509write_csr_free( &req );
    mbedtls_ctr_drbg_free( &ctr_drbg );
    return res;
}