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


#endif /* TRUST_MANAGER_IMPL_H_ */
