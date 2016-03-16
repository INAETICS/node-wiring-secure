/*
 * trust_manager_impl.h
 *
 *  \date       Feb 12, 2016
 *  \author    	<a href="mailto:dev@celix.apache.org">Inaetics Project Team</a>
 *  \copyright	Apache License, Version 2.0
 */
#ifndef TRUST_MANAGER_IMPL_H_
#define TRUST_MANAGER_IMPL_H_

#include "trust_manager_service.h"

struct trust_manager {
	char *name;
    char *ca_host;
    int ca_port;
    int refresh_interval;
};

extern void trust_manager_getCertificate(trust_manager_pt instance);

/**
 * Obtains the most recent certificate (absolute filepath)..
 */
extern int get_current_certificate(char* certificate_filepath);

/**
 * Obtains the most recent full certificate (absolute filepath)..
 */
extern int get_current_full_certificate(char* certificate_filepath);

/**
 * Obtains the most recent ca certificate (absolute filepath)..
 */
extern int get_current_ca_certificate(char* ca_cert_filepath);

/**
 * Obtains the most recent private key (absolute filepath).
 */
extern int get_current_private_key(char* key_filepath);

/**
 * Obtains the most recent public key (absolute filepath)..
 */
extern int get_current_public_key(char* key_filepath);

/**
 * Obtains the most recent content of the cert.
 */
extern int get_current_certificate_content(char* content);

/**
 * Obtains the most recent content of the full cert (incl keys).
 */
extern int get_current_full_certificate_content(char* content);

/**
 * Obtains the most recent content of the ca cert.
 */
extern int get_current_ca_certificate_content(char* content);

/**
 * Obtains the most recent content of the private key.
 */
extern int get_current_private_key_content(char* content);

/**
 * Obtains the most recent content of the public key.
 */
extern int get_current_public_key_content(char* content);

#endif /* TRUST_MANAGER_IMPL_H_ */
