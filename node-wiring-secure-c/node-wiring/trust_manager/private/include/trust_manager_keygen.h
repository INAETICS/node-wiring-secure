//
// Created by Martin Gaida on 12/8/15.
//

#ifndef CACLIENT_CACLIENT_CSR_GENERATOR_H
#define CACLIENT_CACLIENT_CSR_GENERATOR_H

#include <mbedtls/pk.h>


/**
 * Generates rsa mbedtls pk context keypair.
 */
extern int generate_keypair(mbedtls_pk_context* key);

/**
 * Writes a cfssl api compatible json csr. Returns 0 on success.
 */
extern int generate_csr(mbedtls_pk_context* key, char* csr);

/**
 * Public key in standard pem char[] representation. Returns 0 on success.
 */
extern int get_public_key(mbedtls_pk_context* key, char* key_pair);

/**
 * private key in standard pem char[] representation. Returns 0 on success.
 */
extern int get_private_key(mbedtls_pk_context* key, char* private_key);

#endif //CACLIENT_CACLIENT_CSR_GENERATOR_H