/*
 * trust_manager_impl.h
 *
 *  \date       Feb 12, 2016
 *  \author    	<a href="mailto:dev@celix.apache.org">Inaetics Project Team</a>
 *  \copyright	Apache License, Version 2.0
 */
#include <stdio.h>
#include "trust_manager_impl.h"

void trust_manager_getCertificate(trust_manager_pt instance){
	printf("Getting certificate from  %s\n", instance->name);
}

