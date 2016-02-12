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
	void (*trust_manager_getCertificate)(trust_manager_pt instance);
};

#endif /* TRUST_MANAGER_H_ */
