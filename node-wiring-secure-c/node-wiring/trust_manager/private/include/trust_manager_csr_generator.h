//
// Created by Martin Gaida on 12/8/15.
//

#ifndef CACLIENT_CACLIENT_CSR_GENERATOR_H
#define CACLIENT_CACLIENT_CSR_GENERATOR_H

#include <mbedtls/pk.h>

extern mbedtls_pk_context*  generate_keypair();
extern char* generate_csr();

#endif //CACLIENT_CACLIENT_CSR_GENERATOR_H