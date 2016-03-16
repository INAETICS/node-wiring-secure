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
#include "bundle_context.h"

#include "trust_manager_service.h"
#include "trust_manager_impl.h"
#include "trust_manager_worker.h"

#define TRUST_MANAGER_CA_HOST_PROPERTY_NAME				"trust.manager.ca.host"
#define DEFAULT_TRUST_MANAGER_CA_HOST					"localhost"
#define TRUST_MANAGER_CA_PORT_PROPERTY_NAME				"trust.manager.ca.port"
#define DEFAULT_TRUST_MANAGER_CA_PORT					8888
#define TRUST_MANAGER_KEY_STORAGE_PROPERTY_NAME			"trust.manager.key.storage"
#define DEFAULT_TRUST_MANAGER_KEY_STORAGE				"/tmp/inaeticstrustmanager"
#define TRUST_MANAGER_REFRESH_INTERVAL_PROPERTY_NAME	"trust.manager.refresh.interval"
#define DEFAULT_TRUST_MANAGER_REFRESH_INTERVAL			5

struct trust_managerActivator {
    trust_worker_pt trust_worker;

	service_registration_pt reg;
	trust_manager_service_pt trust_managerService;
};

struct bundle_instance {
	int refresh;
};

typedef struct bundle_instance *bundle_instance_pt;

static char *bundleActivator_getCaHost(bundle_instance_pt bi, bundle_context_pt context);
static int bundleActivator_getCaPort(bundle_instance_pt bi, bundle_context_pt context);
static char *bundleActivator_getKeyStorage(bundle_instance_pt bi, bundle_context_pt context);
static int bundleActivator_getRefreshInterval(bundle_instance_pt bi, bundle_context_pt context);
static char* bundleActivator_getProperty(bundle_instance_pt bi, bundle_context_pt context, char* propertyName, char* defaultValue);
static int bundleActivator_getPropertyInt(bundle_instance_pt bi, bundle_context_pt context, char * propertyName, int defaultValue);

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

	bundle_instance_pt bi = (bundle_instance_pt) userData;
	int refreshInterval = bundleActivator_getRefreshInterval(bi, context);
	int caPort = bundleActivator_getCaPort(bi, context);
	char* caHost = bundleActivator_getCaHost(bi, context);
	char* keyStorage = bundleActivator_getKeyStorage(bi, context);

	trust_manager_activator_pt act = (trust_manager_activator_pt) userData;

	act->trust_managerService = calloc(1, sizeof(*act->trust_managerService));

	if (act->trust_managerService) {
		act->trust_managerService->instance = calloc(1, sizeof(*act->trust_managerService->instance));
		if (act->trust_managerService->instance) {
            // set all config for trust manager bundle
			act->trust_managerService->instance->name = TRUST_MANAGER_SERVICE_NAME;
            act->trust_managerService->instance->ca_host = caHost;
            act->trust_managerService->instance->ca_port = caPort;
			act->trust_managerService->instance->key_storage = keyStorage;
            act->trust_managerService->instance->refresh_interval = refreshInterval;
			// api
			act->trust_managerService->trust_manager_getCurrentCertificate = trust_manager_getCurrentCertificate;
			act->trust_managerService->trust_manager_getCurrentFullCertificate = trust_manager_getCurrentFullCertificate;
			act->trust_managerService->trust_manager_getCurrentCaCertificate = trust_manager_getCurrentCaCertificate;
			act->trust_managerService->trust_manager_getCurrentPrivateKey = trust_manager_getCurrentPrivateKey;
			act->trust_managerService->trust_manager_getCurrentPublicKey = trust_manager_getCurrentPublicKey;
			act->trust_managerService->trust_manager_getCurrentCertificateContent = trust_manager_getCurrentCertificateContent;
			act->trust_managerService->trust_manager_getCurrentFullCertificateContent = trust_manager_getCurrentFullCertificateContent;
			act->trust_managerService->trust_manager_getCurrentCaCertificateContent = trust_manager_getCurrentCaCertificateContent;
			act->trust_managerService->trust_manager_getCurrentPrivateKeyContent = trust_manager_getCurrentPrivateKeyContent;
			act->trust_managerService->trust_manager_getCurrentPublicKeyContent = trust_manager_getCurrentPublicKeyContent;

			// run the worker thread
			trustWorker_create(context, &act->trust_worker, &act->trust_managerService->instance);

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

static char *bundleActivator_getCaHost(bundle_instance_pt bi, bundle_context_pt context) {
	return bundleActivator_getProperty(bi, context, TRUST_MANAGER_CA_HOST_PROPERTY_NAME, DEFAULT_TRUST_MANAGER_CA_HOST);
}

static int bundleActivator_getCaPort(bundle_instance_pt bi, bundle_context_pt context) {
	return bundleActivator_getPropertyInt(bi, context, TRUST_MANAGER_CA_PORT_PROPERTY_NAME, DEFAULT_TRUST_MANAGER_CA_PORT);
}

static char *bundleActivator_getKeyStorage(bundle_instance_pt bi, bundle_context_pt context) {
	return bundleActivator_getProperty(bi, context, TRUST_MANAGER_KEY_STORAGE_PROPERTY_NAME, DEFAULT_TRUST_MANAGER_KEY_STORAGE);
}

static int bundleActivator_getRefreshInterval(bundle_instance_pt bi, bundle_context_pt context) {
	return bundleActivator_getPropertyInt(bi, context, TRUST_MANAGER_REFRESH_INTERVAL_PROPERTY_NAME, DEFAULT_TRUST_MANAGER_REFRESH_INTERVAL);
}

static int bundleActivator_getPropertyInt(bundle_instance_pt bi, bundle_context_pt context, char* propertyName, int defaultValue) {
	char *strValue = NULL;
	int value;

	bundleContext_getProperty(context, propertyName, &strValue);
	if (strValue != NULL) {
		char* endptr = strValue;

		errno = 0;
		value = strtol(strValue, &endptr, 10);
		if (*endptr || errno != 0) {
//			logHelper_log(bi->loghelper, OSGI_LOGSERVICE_WARNING, "incorrect format for %s", propertyName);
			value = defaultValue;
		}
	}
	else {
		value = defaultValue;
	}

	return value;
}

static char* bundleActivator_getProperty(bundle_instance_pt bi, bundle_context_pt context, char* propertyName, char* defaultValue) {
	char *strValue = NULL;

	bundleContext_getProperty(context, propertyName, &strValue);
	if (strValue != NULL) {
		return strValue;
	}
	else {
		return defaultValue;
	}
}

