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
	char *key_storage;
    char *ca_host;
    int ca_port;
    int refresh_interval;
};

/**
 * Obtains the most recent certificate (absolute filepath)..
 */
extern int trust_manager_getCurrentCertificate(trust_manager_pt instance, char* certificate_filepath);

/**
 * Obtains the most recent full certificate (absolute filepath)..
 */
extern int trust_manager_getCurrentFullCertificate(trust_manager_pt instance, char* certificate_filepath);

/**
 * Obtains the most recent ca certificate (absolute filepath)..
 */
extern int trust_manager_getCurrentCaCertificate(trust_manager_pt instance, char* ca_cert_filepath);

/**
 * Obtains the most recent private key (absolute filepath).
 */
extern int trust_manager_getCurrentPrivateKey(trust_manager_pt instance, char* key_filepath);

/**
 * Obtains the most recent public key (absolute filepath)..
 */
extern int trust_manager_getCurrentPublicKey(trust_manager_pt instance, char* key_filepath);

/**
 * Obtains the most recent content of the cert.
 */
extern int trust_manager_getCurrentCertificateContent(trust_manager_pt instance, char* content);

/**
 * Obtains the most recent content of the full cert (incl keys).
 */
extern int trust_manager_getCurrentFullCertificateContent(trust_manager_pt instance, char* content);

/**
 * Obtains the most recent content of the ca cert.
 */
extern int trust_manager_getCurrentCaCertificateContent(trust_manager_pt instance, char* content);

/**
 * Obtains the most recent content of the private key.
 */
extern int trust_manager_getCurrentPrivateKeyContent(trust_manager_pt instance, char* content);

/**
 * Obtains the most recent content of the public key.
 */
extern int trust_manager_getCurrentPublicKeyContent(trust_manager_pt instance, char* content);

#endif /* TRUST_MANAGER_IMPL_H_ */
