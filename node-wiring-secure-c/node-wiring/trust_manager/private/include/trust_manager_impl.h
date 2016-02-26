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
};

extern void trust_manager_getCertificate(trust_manager_pt instance);

/**
 * Obtains the most recent certificate (absolute filepath)..
 */
extern int get_current_certificate(char* certificate_filepath);

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


#endif /* TRUST_MANAGER_IMPL_H_ */
