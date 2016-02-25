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

void init_string(struct string *s);
size_t writefunc(void *ptr, size_t size, size_t nmemb, struct string *s);

void print_certificate_info(mbedtls_x509_crt *certificate);
int verify_certificate(mbedtls_x509_crt *certificate, mbedtls_x509_crt *ca_cert, int backdate_threshold);
int request_certificate(mbedtls_pk_context* key, char* certificate);


#endif //CACLIENT_CACLIENT_CERTIFICATEREQUESTER_H