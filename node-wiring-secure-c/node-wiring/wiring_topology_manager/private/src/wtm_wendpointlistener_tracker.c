/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include <string.h>

#include "service_tracker.h"
#include "wiring_topology_manager_impl.h"

celix_status_t wiringTopologyManager_createWiringEndpointListenerTracker(wiring_topology_manager_pt manager, service_tracker_pt *tracker) {
    celix_status_t status;

    service_tracker_customizer_pt customizer = NULL;

    status = serviceTrackerCustomizer_create(manager, wiringTopologyManager_wiringEndpointListenerAdding, wiringTopologyManager_wiringEndpointListenerAdded,
            wiringTopologyManager_wiringEndpointListenerModified, wiringTopologyManager_wiringEndpointListenerRemoved, &customizer);

    if (status == CELIX_SUCCESS) {
        status = serviceTracker_create(manager->context, (char *) INAETICS_WIRING_ENDPOINT_LISTENER_SERVICE, customizer, tracker);
    }

    return status;
}

celix_status_t wiringTopologyManager_wiringEndpointListenerAdding(void* handle, service_reference_pt reference, void** service) {
    celix_status_t status = CELIX_SUCCESS;
    wiring_topology_manager_pt manager = handle;

    bundleContext_getService(manager->context, reference, service);

    return status;
}

celix_status_t wiringTopologyManager_wiringEndpointListenerAdded(void* handle, service_reference_pt reference, void* service) {
    celix_status_t status = CELIX_SUCCESS;
    wiring_topology_manager_pt manager = handle;
    char *scope = NULL;
    char* wtm = NULL;

    serviceReference_getProperty(reference, (char *) INAETICS_WIRING_ENDPOINT_LISTENER_SCOPE, &scope);
    serviceReference_getProperty(reference, "WTM", &wtm);

    if (wtm != NULL && strcmp(wtm, "true") == 0) {
        printf("WTM: Ignoring own ENDPOINT_LISTENER\n");
    }
    else {
        status = celixThreadMutex_lock(&manager->listenerListLock);

        if (status == CELIX_SUCCESS) {
            hashMap_put(manager->listenerList, reference, NULL);
            celixThreadMutex_unlock(&manager->listenerListLock);
        }

        filter_pt filter = filter_create(scope);
        status = celixThreadMutex_lock(&manager->exportedWiringEndpointsLock);

        if (status == CELIX_SUCCESS) {
            hash_map_iterator_pt propIter = hashMapIterator_create(manager->exportedWiringEndpoints);

            while (hashMapIterator_hasNext(propIter)) {
                hash_map_pt wiringAdminList = hashMapIterator_nextValue(propIter);
                hash_map_iterator_pt waIter = hashMapIterator_create(wiringAdminList);

                while (hashMapIterator_hasNext(waIter)) {
                    wiring_endpoint_description_pt wEndpoint = hashMapIterator_nextValue(waIter);

                    bool matchResult = false;
                    filter_match(filter, wEndpoint->properties, &matchResult);

                    if (matchResult) {
                        wiring_endpoint_listener_pt listener = (wiring_endpoint_listener_pt) service;
                        status = listener->wiringEndpointAdded(listener->handle, wEndpoint, scope);
                    }
                }
                hashMapIterator_destroy(waIter);
            }
            hashMapIterator_destroy(propIter);

            celixThreadMutex_unlock(&manager->exportedWiringEndpointsLock);
        }
        filter_destroy(filter);

    }


    return status;
}

celix_status_t wiringTopologyManager_wiringEndpointListenerModified(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status;

    status = wiringTopologyManager_wiringEndpointListenerRemoved(handle, reference, service);
    if (status == CELIX_SUCCESS) {
        status = wiringTopologyManager_wiringEndpointListenerAdded(handle, reference, service);
    }

    return status;
}

celix_status_t wiringTopologyManager_wiringEndpointListenerRemoved(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status;
    wiring_topology_manager_pt manager = handle;

    status = celixThreadMutex_lock(&manager->listenerListLock);

    if (status == CELIX_SUCCESS) {
        if (hashMap_remove(manager->listenerList, reference)) {
            printf("WTM: EndpointListener Removed");
        }

        status = celixThreadMutex_unlock(&manager->listenerListLock);
    }

    return status;
}
