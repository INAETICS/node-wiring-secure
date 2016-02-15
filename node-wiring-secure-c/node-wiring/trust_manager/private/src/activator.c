/*
 * activator.c
 *
 *  \date       Feb 12, 2016
 *  \author    	<a href="mailto:dev@celix.apache.org">Inaetics Project Team</a>
 *  \copyright	Apache License, Version 2.0
 */

#include <sys/cdefs.h>
#include <stdlib.h>

#include "bundle_activator.h"
#include "trust_manager_impl.h"
#include "trust_manager_worker.h"

struct trust_managerActivator {
    trust_worker_pt trust_worker;

	service_registration_pt reg;
	trust_manager_service_pt trust_managerService;
};

typedef struct trust_managerActivator *trust_manager_activator_pt;

celix_status_t bundleActivator_create(bundle_context_pt  __attribute__((unused)) context, void **userData) {
	celix_status_t status = CELIX_SUCCESS;
	trust_manager_activator_pt activator;
	*userData = calloc(1, sizeof(struct trust_managerActivator));
	if (*userData) {
		activator = *userData;
		activator->reg = NULL;
	} else {
		status = CELIX_ENOMEM;
	}
	return status;
}

celix_status_t bundleActivator_start(void * userData, bundle_context_pt context) {
	celix_status_t status;

	trust_manager_activator_pt act = (trust_manager_activator_pt) userData;

	act->trust_managerService = calloc(1, sizeof(*act->trust_managerService));

	if (act->trust_managerService) {
		act->trust_managerService->instance = calloc(1, sizeof(*act->trust_managerService->instance));
		if (act->trust_managerService->instance) {
			act->trust_managerService->instance->name = TRUST_MANAGER_SERVICE_NAME;
			act->trust_managerService->trust_manager_getCertificate = trust_manager_getCertificate;

			// run the worker thread
			trustWorker_create(context, &act->trust_worker);

			status = bundleContext_registerService(context, TRUST_MANAGER_SERVICE_NAME, act->trust_managerService, NULL, &act->reg);
		} else {
			status = CELIX_ENOMEM;
		}
	} else {
		status = CELIX_ENOMEM;
	}
	return status;
}

celix_status_t bundleActivator_stop(void * userData, bundle_context_pt  __attribute__((unused)) context) {
	celix_status_t status = CELIX_SUCCESS;

	trust_manager_activator_pt act = (trust_manager_activator_pt) userData;

    trustWorker_destroy(act->trust_worker);

	serviceRegistration_unregister(act->reg);
	act->reg = NULL;

	free(act->trust_managerService->instance);
	free(act->trust_managerService);

	return status;
}

celix_status_t bundleActivator_destroy(void * userData, bundle_context_pt  __attribute__((unused)) context) {
	free(userData);
	return CELIX_SUCCESS;
}
