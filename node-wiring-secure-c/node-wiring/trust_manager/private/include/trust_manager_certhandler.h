/*
 * trust_manager_certificaterequester.h
 *
 *  \date       Feb, 2016
 *  \author    	<a href="mailto:dev@celix.apache.org">Inaetics Project Team</a>
 *  \copyright	Apache License, Version 2.0
 */
#ifndef CACLIENT_CACLIENT_CERTIFICATEREQUESTER_H
#define CACLIENT_CACLIENT_CERTIFICATEREQUESTER_H

#include <stdlib.h>
#include <mbedtls/pk.h>

/**
 * The string for return body.
 */
struct string {
    char *ptr;
    size_t len;
};

/**
 * Prints the info of a certificate.
 */
void print_certificate_info(mbedtls_x509_crt *certificate);

/**
 * Verifies validaty of a certificate.
 */
int verify_certificate(mbedtls_x509_crt *certificate, mbedtls_x509_crt *ca_cert, int backdate_threshold);

/**
 * Request the ca certificate.
 */
int request_ca_certificate(char *ca_certificate, char* ca_host, int ca_port);

/**
 * Perform CSR and retreive certificate.
 */
int csr_get_certificate(mbedtls_pk_context* key, char* certificate, char *ca_host, int ca_port);


#endif //CACLIENT_CACLIENT_CERTIFICATEREQUESTER_H