/*
 * trust_manager_service.h
 *
 *  \date       Feb 12, 2016
 *  \author    	<a href="mailto:dev@celix.apache.org">Inaetics Project Team</a>
 *  \copyright	Apache License, Version 2.0
 */
#ifndef TRUST_MANAGER_H_
#define TRUST_MANAGER_H_

#define TRUST_MANAGER_SERVICE_NAME "trust_manager"

typedef struct trust_manager *trust_manager_pt;
typedef struct trust_manager_service *trust_manager_service_pt;

struct trust_manager_service {
	trust_manager_pt instance;
	int (*trust_manager_getCurrentCertificate)(trust_manager_pt instance, char* certificate_filepath);
	int (*trust_manager_getCurrentFullCertificate)(trust_manager_pt instance, char* certificate_filepath);
	int (*trust_manager_getCurrentCaCertificate)(trust_manager_pt instance, char* ca_cert_filepath);
	int (*trust_manager_getCurrentPrivateKey)(trust_manager_pt instance, char* key_filepath);
	int (*trust_manager_getCurrentPublicKey)(trust_manager_pt instance, char* key_filepath);
	int (*trust_manager_getCurrentCertificateContent)(trust_manager_pt instance, char* content);
	int (*trust_manager_getCurrentFullCertificateContent)(trust_manager_pt instance, char* content);
	int (*trust_manager_getCurrentCaCertificateContent)(trust_manager_pt instance, char* content);
	int (*trust_manager_getCurrentPrivateKeyContent)(trust_manager_pt instance, char* content);
	int (*trust_manager_getCurrentPublicKeyContent)(trust_manager_pt instance, char* content);
};

#endif /* TRUST_MANAGER_H_ */
