//
// Created by Martin Gaida on 12/8/15.
//

#ifndef CACLIENT_CACLIENT_CSR_GENERATOR_H
#define CACLIENT_CACLIENT_CSR_GENERATOR_H

#include <mbedtls/pk.h>

extern int generate_keypair(mbedtls_pk_context* key);
extern int generate_csr(mbedtls_pk_context* key, char* csr);
extern int get_public_key(mbedtls_pk_context* key, char* key_pair);
extern int get_private_key(mbedtls_pk_context* key, char* private_key);
extern int generate_csr(mbedtls_pk_context* key, char* csr);

#endif //CACLIENT_CACLIENT_CSR_GENERATOR_H