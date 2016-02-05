/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include <stdlib.h>
#include <string.h>

#include "bundle_activator.h"
#include "service_registration.h"

#include "wiring_admin_impl.h"

struct activator {
	bundle_context_pt context;
	wiring_admin_pt admin;
	wiring_admin_service_pt wiringAdminService;
	service_registration_pt registration;
};

celix_status_t bundleActivator_create(bundle_context_pt context, void **userData) {
	celix_status_t status = CELIX_SUCCESS;
	struct activator *activator;

	activator = calloc(1, sizeof(*activator));
	if (!activator) {
		status = CELIX_ENOMEM;
	} else {
		activator->context = context;
		activator->admin = NULL;
		activator->registration = NULL;
		activator->wiringAdminService = NULL;

		*userData = activator;
	}

	return status;
}

celix_status_t bundleActivator_start(void * userData, bundle_context_pt context) {
	celix_status_t status;
	struct activator *activator = userData;

	status = wiringAdmin_create(context, &activator->admin);
	if (status == CELIX_SUCCESS) {
		activator->wiringAdminService = calloc(1, sizeof(struct wiring_admin_service));
		if (!activator->wiringAdminService) {
			status = CELIX_ENOMEM;
		} else {
			activator->wiringAdminService->admin = activator->admin;

			activator->wiringAdminService->exportWiringEndpoint = wiringAdmin_exportWiringEndpoint;
			activator->wiringAdminService->removeExportedWiringEndpoint = wiringAdmin_removeExportedWiringEndpoint;
			activator->wiringAdminService->getWiringAdminProperties = wiringAdmin_getWiringAdminProperties;

			activator->wiringAdminService->importWiringEndpoint = wiringAdmin_importWiringEndpoint;
			activator->wiringAdminService->removeImportedWiringEndpoint = wiringAdmin_removeImportedWiringEndpoint;

			char *uuid = NULL;
			status = bundleContext_getProperty(activator->context, (char *) OSGI_FRAMEWORK_FRAMEWORK_UUID, &uuid);

			if (status != CELIX_SUCCESS) {
				printf("%s: no framework UUID defined?!\n", TAG);
			} else {
				size_t len = 14 + strlen(OSGI_FRAMEWORK_OBJECTCLASS) + strlen(OSGI_RSA_ENDPOINT_FRAMEWORK_UUID) + strlen(uuid);
				char scope[len + 1];

				snprintf(scope, len, "(%s=%s)", OSGI_RSA_ENDPOINT_FRAMEWORK_UUID, uuid);

				properties_pt props = properties_create();
				properties_set(props, (char *) INAETICS_WIRING_ADMIN_SCOPE, scope);

				status = bundleContext_registerService(context, (char*) INAETICS_WIRING_ADMIN, activator->wiringAdminService, props, &activator->registration);
			}
		}
	}

	return status;
}

celix_status_t bundleActivator_stop(void * userData, bundle_context_pt context) {
	celix_status_t status;
	struct activator* activator = (struct activator*) userData;

	status = wiringAdmin_stop(activator->admin);

	if (status == CELIX_SUCCESS) {
		serviceRegistration_unregister(activator->registration);
	}

	if (status == CELIX_SUCCESS) {
		activator->registration = NULL;
		free(activator->wiringAdminService);
		activator->wiringAdminService = NULL;
	}

	return status;
}

celix_status_t bundleActivator_destroy(void * userData, bundle_context_pt context) {
	celix_status_t status;
	struct activator *activator = userData;

	status = wiringAdmin_destroy(&activator->admin);

	if (status == CELIX_SUCCESS) {
		free(activator);
	}

	return status;
}

