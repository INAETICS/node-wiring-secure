/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include <stdlib.h>
#include <string.h>

#include "bundle_activator.h"
#include "constants.h"
#include "service_registration.h"

#include "remote_service_admin_inaetics_impl.h"
#include "export_registration_dfi.h"
#include "import_registration_dfi.h"

#include "wiring_topology_manager.h"
#include "wiring_endpoint_listener.h"

static celix_status_t bundleActivator_createEPLTracker(struct activator *activator, service_tracker_pt *tracker);
static celix_status_t bundleActivator_createWTMTracker(struct activator *activator, service_tracker_pt *tracker);

celix_status_t bundleActivator_create(bundle_context_pt context, void **userData) {
    celix_status_t status = CELIX_SUCCESS;
    struct activator *activator;

    activator = calloc(1, sizeof(*activator));
    if (!activator) {
        status = CELIX_ENOMEM;
    } else {
        activator->admin = NULL;
        activator->registration = NULL;
        activator->eplTracker = NULL;
        activator->wtmTracker = NULL;

        *userData = activator;
    }

    return status;
}

celix_status_t bundleActivator_start(void * userData, bundle_context_pt context) {
    celix_status_t status;
    struct activator *activator = userData;
    remote_service_admin_service_pt remoteServiceAdminService = NULL;

    status = remoteServiceAdmin_create(context, &activator->admin);
    if (status == CELIX_SUCCESS) {
        remoteServiceAdminService = calloc(1, sizeof(*remoteServiceAdminService));

        wiring_endpoint_listener_pt wEndpointListener = (wiring_endpoint_listener_pt) calloc(1, sizeof(*wEndpointListener));

        if (!remoteServiceAdminService || !wEndpointListener) {

            if (wEndpointListener) {
                free(wEndpointListener);
            }
            if (remoteServiceAdminService) {
                free(remoteServiceAdminService);
            }

            status = CELIX_ENOMEM;
        } else {
            status = bundleActivator_createEPLTracker(activator, &activator->eplTracker);

            if (status != CELIX_SUCCESS) {
                printf("RSA: Creation of EPLTracker failed\n");
            } else {
                status = bundleActivator_createWTMTracker(activator, &activator->wtmTracker);
            }

            if (status != CELIX_SUCCESS) {
                printf("RSA: Creation of WTMTracker failed\n");
            } else {

                /* the rsa also has a wiringEndpointListener because it needs to be informed, when a
                 * wiring endpoint has been sucessfully exported/imported and therefore it
                 * the according services can be exported/imported.
                 */
                wEndpointListener->handle = (void*) activator->admin;
                wEndpointListener->wiringEndpointAdded = remoteServiceAdmin_addWiringEndpoint;
                wEndpointListener->wiringEndpointRemoved = remoteServiceAdmin_removeWiringEndpoint;

                activator->wEndpointListener = wEndpointListener;
                activator->wEndpointListenerRegistration = NULL;

                remoteServiceAdminService->admin = activator->admin;

                remoteServiceAdminService->exportService = remoteServiceAdmin_exportService;
                remoteServiceAdminService->getExportedServices = remoteServiceAdmin_getExportedServices;
                remoteServiceAdminService->getImportedEndpoints = remoteServiceAdmin_getImportedEndpoints;
                remoteServiceAdminService->importService = remoteServiceAdmin_importService;

                remoteServiceAdminService->exportReference_getExportedEndpoint = exportReference_getExportedEndpoint;
                remoteServiceAdminService->exportReference_getExportedService = exportReference_getExportedService;

                remoteServiceAdminService->exportRegistration_close = remoteServiceAdmin_removeExportedService;
                remoteServiceAdminService->exportRegistration_getException = exportRegistration_getException;
                remoteServiceAdminService->exportRegistration_getExportReference = exportRegistration_getExportReference;

                remoteServiceAdminService->importReference_getImportedEndpoint = importReference_getImportedEndpoint;
                remoteServiceAdminService->importReference_getImportedService = importReference_getImportedService;

                remoteServiceAdminService->importRegistration_close = remoteServiceAdmin_removeImportedService;
                remoteServiceAdminService->importRegistration_getException = importRegistration_getException;
                remoteServiceAdminService->importRegistration_getImportReference = importRegistration_getImportReference;
                char *uuid = NULL;

                properties_pt props = properties_create();
                bundleContext_getProperty(context, OSGI_FRAMEWORK_FRAMEWORK_UUID, &uuid);
                size_t len = 11 + strlen(OSGI_FRAMEWORK_OBJECTCLASS) + strlen(OSGI_RSA_ENDPOINT_FRAMEWORK_UUID) + strlen(uuid);

                char scope[len + 1];
                // check that we are not informed by node_discovery
                sprintf(scope, "(%s=%s)", OSGI_RSA_ENDPOINT_FRAMEWORK_UUID, uuid);
                properties_set(props, (char *) INAETICS_WIRING_ENDPOINT_LISTENER_SCOPE, scope);
                properties_set(props, "RSA", "true");

                activator->adminService = remoteServiceAdminService;

                serviceTracker_open(activator->eplTracker);

                // wiring endpoint listener needs to be before wtm tracker, otherwise rsa will not be informed about wiring endpoints
                status = bundleContext_registerService(context, (char *) INAETICS_WIRING_ENDPOINT_LISTENER_SERVICE, wEndpointListener, props, &activator->wEndpointListenerRegistration);

                if (status != CELIX_SUCCESS) {
                    properties_destroy(props);
                    printf("RSA: service registration of wiring endpoint listener failed\n");
                }

                serviceTracker_open(activator->wtmTracker);


            }
        }
    }

    return status;
}

celix_status_t bundleActivator_stop(void * userData, bundle_context_pt context) {
    celix_status_t status = CELIX_SUCCESS;
    struct activator *activator = userData;

    if (activator->wtmTracker != NULL) {
    	if (serviceTracker_close(activator->wtmTracker) == CELIX_SUCCESS) {
    		serviceTracker_destroy(activator->wtmTracker);
    	}
    }

    if (activator->eplTracker != NULL) {
    	if (serviceTracker_close(activator->eplTracker) == CELIX_SUCCESS) {
    		serviceTracker_destroy(activator->eplTracker);
    	}
    }

    if (activator->registration != NULL) {
        serviceRegistration_unregister(activator->registration);
        activator->registration = NULL;
    }

    remoteServiceAdmin_stop(activator->admin);

    remoteServiceAdmin_destroy(&activator->admin);
    free(activator->adminService);

    serviceRegistration_unregister(activator->wEndpointListenerRegistration);
    free(activator->wEndpointListener);

    return status;
}

celix_status_t bundleActivator_destroy(void * userData, bundle_context_pt context) {
    celix_status_t status = CELIX_SUCCESS;
    struct activator *activator = userData;

    free(activator);

    return status;
}

static celix_status_t bundleActivator_createEPLTracker(struct activator *activator, service_tracker_pt *tracker) {
    celix_status_t status;

    service_tracker_customizer_pt customizer = NULL;

    status = serviceTrackerCustomizer_create(activator->admin, remoteServiceAdmin_endpointListenerAdding, remoteServiceAdmin_endpointListenerAdded, remoteServiceAdmin_endpointListenerModified,
            remoteServiceAdmin_endpointListenerRemoved, &customizer);

    if (status == CELIX_SUCCESS) {
        status = serviceTracker_create(activator->admin->context, (char *) OSGI_ENDPOINT_LISTENER_SERVICE, customizer, tracker);
    }

    return status;
}

static celix_status_t bundleActivator_createWTMTracker(struct activator *activator, service_tracker_pt *tracker) {
    celix_status_t status;

    service_tracker_customizer_pt customizer = NULL;

    status = serviceTrackerCustomizer_create(activator, remoteServiceAdmin_wtmAdding, remoteServiceAdmin_wtmAdded, remoteServiceAdmin_wtmModified, remoteServiceAdmin_wtmRemoved, &customizer);

    if (status == CELIX_SUCCESS) {
        status = serviceTracker_create(activator->admin->context, (char*) INAETICS_WIRING_TOPOLOGY_MANAGER_SERVICE, customizer, tracker);
    }

    return status;
}

