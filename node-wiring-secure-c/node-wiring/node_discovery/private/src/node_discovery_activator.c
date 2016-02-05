/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "bundle_activator.h"
#include "service_tracker.h"
#include "service_registration.h"
#include "constants.h"
#include "remote_constants.h"
#include "celix_log.h"

#include "node_discovery.h"
#include "node_discovery_impl.h"
#include "wiring_endpoint_listener.h"
#include "wiring_endpoint_description.h"

struct activator {
	bundle_context_pt context;
	node_discovery_pt node_discovery;

	service_tracker_pt wiringEndpointListenerTracker;
	wiring_endpoint_listener_pt wiringEndpointListener;
	service_registration_pt wiringEndpointListenerService;
};

static celix_status_t createWiringEndpointListenerTracker(struct activator *activator, service_tracker_pt *tracker) {
	celix_status_t status = CELIX_SUCCESS;

	service_tracker_customizer_pt customizer = NULL;

	status = serviceTrackerCustomizer_create(activator->node_discovery, node_discovery_wiringEndpointListenerAdding, node_discovery_wiringEndpointListenerAdded, node_discovery_wiringEndpointListenerModified,
			node_discovery_wiringEndpointListenerRemoved, &customizer);

	if (status == CELIX_SUCCESS) {
		status = serviceTracker_create(activator->context, (char *) INAETICS_WIRING_ENDPOINT_LISTENER_SERVICE, customizer, tracker);
	}

	return status;
}

celix_status_t bundleActivator_create(bundle_context_pt context, void **userData) {
	celix_status_t status = CELIX_SUCCESS;

	struct activator* activator = calloc(1, sizeof(*activator));

	if (activator) {
		activator->context = context;
		activator->wiringEndpointListenerTracker = NULL;
		activator->wiringEndpointListener = NULL;
		activator->wiringEndpointListenerService = NULL;

		status = node_discovery_create(context, &activator->node_discovery);

		if (status == CELIX_SUCCESS) {
			status = createWiringEndpointListenerTracker(activator, &(activator->wiringEndpointListenerTracker));
		}

		if (status == CELIX_SUCCESS) {
			*userData = activator;
		} else {
			free(activator);
		}
	} else {
		status = CELIX_ENOMEM;
	}

	return status;

}

celix_status_t bundleActivator_start(void * userData, bundle_context_pt context) {
	celix_status_t status = CELIX_SUCCESS;

	struct activator *activator = userData;
	char *uuid = NULL;

	status = bundleContext_getProperty(context, OSGI_FRAMEWORK_FRAMEWORK_UUID, &uuid);

	if (status != CELIX_SUCCESS) {
		printf("NODE_DISCOVERY: no framework UUID defined?!\n");
	} else {
		size_t len = 11 + strlen(OSGI_FRAMEWORK_OBJECTCLASS) + strlen(OSGI_RSA_ENDPOINT_FRAMEWORK_UUID) + strlen(uuid);
		wiring_endpoint_listener_pt wEndpointListener = calloc(1, sizeof(*wEndpointListener));

		if (wEndpointListener) {
			char scope[len + 1];

			sprintf(scope, "(%s=%s)", OSGI_RSA_ENDPOINT_FRAMEWORK_UUID, uuid);

			wEndpointListener->handle = activator->node_discovery;
			wEndpointListener->wiringEndpointAdded = node_discovery_wiringEndpointAdded;
			wEndpointListener->wiringEndpointRemoved = node_discovery_wiringEndpointRemoved;
			activator->wiringEndpointListener = wEndpointListener;

			properties_pt props = properties_create();
			properties_set(props, "NODE_DISCOVERY", "true");
			properties_set(props, (char *) INAETICS_WIRING_ENDPOINT_LISTENER_SCOPE, scope);

			// node_discovery_start needs to be first to initalize the propert etcd_watcher values
			if (status == CELIX_SUCCESS) {
			    status = node_discovery_start(activator->node_discovery);
			}
            if (status == CELIX_SUCCESS) {
                status = serviceTracker_open(activator->wiringEndpointListenerTracker);
            }

            if (status == CELIX_SUCCESS) {
                status = bundleContext_registerService(context, (char *) INAETICS_WIRING_ENDPOINT_LISTENER_SERVICE, wEndpointListener, props, &activator->wiringEndpointListenerService);
            }


		} else {
			status = CELIX_ENOMEM;
		}
	}

	return status;
}

celix_status_t bundleActivator_stop(void * userData, bundle_context_pt context) {
	celix_status_t status = CELIX_SUCCESS;
	struct activator *activator = userData;

	node_discovery_stop(activator->node_discovery);

	serviceTracker_close(activator->wiringEndpointListenerTracker);

	serviceRegistration_unregister(activator->wiringEndpointListenerService);

	if (status == CELIX_SUCCESS) {
		free(activator->wiringEndpointListener);
	}

	return status;
}

celix_status_t bundleActivator_destroy(void * userData, bundle_context_pt context) {
	celix_status_t status = CELIX_SUCCESS;
	struct activator *activator = userData;

	serviceTracker_destroy(activator->wiringEndpointListenerTracker);
	node_discovery_destroy(activator->node_discovery);

	activator->wiringEndpointListener = NULL;
	activator->wiringEndpointListenerService = NULL;
	activator->wiringEndpointListenerTracker = NULL;
	activator->node_discovery = NULL;
	activator->context = NULL;

	free(activator);

	return status;
}
