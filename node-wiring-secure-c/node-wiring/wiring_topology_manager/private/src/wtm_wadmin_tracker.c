/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include "service_tracker.h"
#include "wiring_topology_manager_impl.h"

celix_status_t wiringTopologyManager_createWaTracker(wiring_topology_manager_pt manager, service_tracker_pt *tracker) {
    celix_status_t status = CELIX_SUCCESS;

    service_tracker_customizer_pt customizer = NULL;

    status = serviceTrackerCustomizer_create(manager, wiringTopologyManager_waAdding, wiringTopologyManager_waAdded, wiringTopologyManager_waModified, wiringTopologyManager_waRemoved, &customizer);

    if (status == CELIX_SUCCESS) {
        status = serviceTracker_create(manager->context, (char*) INAETICS_WIRING_ADMIN, customizer, tracker);
    }

    return status;
}

celix_status_t wiringTopologyManager_waAdding(void * handle, service_reference_pt reference, void **service) {
    celix_status_t status = CELIX_SUCCESS;

    wiring_topology_manager_pt manager = handle;

    status = bundleContext_getService(manager->context, reference, service);

    return status;
}

celix_status_t wiringTopologyManager_waAdded(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status = CELIX_SUCCESS;
    wiring_topology_manager_pt manager = handle;
    wiring_admin_service_pt wiringAdminService = (wiring_admin_service_pt) service;

    celixThreadMutex_lock(&manager->waListLock);
    arrayList_add(manager->waList, wiringAdminService);
    celixThreadMutex_unlock(&manager->waListLock);

    /* check whether one of the exported Wires can be exported via the newly available wiringAdmin */
    celixThreadMutex_lock(&manager->exportedWiringEndpointsLock);
    hash_map_iterator_pt iter = hashMapIterator_create(manager->exportedWiringEndpoints);

    // is the properties_match missing here>
    while (hashMapIterator_hasNext(iter)) {
        hash_map_entry_pt entry = hashMapIterator_nextEntry(iter);
        properties_pt exportedWireProperties = hashMapEntry_getKey(entry);
        hash_map_pt wiringAdminList = hashMapEntry_getValue(entry);
        wiring_endpoint_description_pt wEndpoint = NULL;

        char* serviceId = properties_get(exportedWireProperties, "service.id");

        printf("WTM: wiringTopologyManager_waAdded export Wire for %s \n", serviceId);

        status = wiringTopologyManager_WiringAdminServiceExportWiringEndpoint(manager, wiringAdminService, exportedWireProperties, &wEndpoint);

        if (status == CELIX_SUCCESS) {
            hashMap_put(wiringAdminList, wiringAdminService, wEndpoint);
        } else {
            printf("WTM: Could not export wire with new WA\n");
        }
    }
    hashMapIterator_destroy(iter);

    /* check whether the waiting exports can be exported */

    array_list_iterator_pt waitList = arrayListIterator_create(manager->waitingForExport);

    while (arrayListIterator_hasNext(waitList)) {
        properties_pt srvcProperties = arrayListIterator_next(waitList);
        char* serviceId = properties_get(srvcProperties, "service.id");

        printf("WTM: wiringTopologyManager_waAdded export Wire for %s \n", serviceId);
        wiring_endpoint_description_pt wEndpoint = NULL;

        status = wiringTopologyManager_WiringAdminServiceExportWiringEndpoint(manager, wiringAdminService, srvcProperties, &wEndpoint);

        if (status == CELIX_SUCCESS) {
            arrayListIterator_remove(waitList);

            arrayListIterator_destroy(waitList);
            waitList = arrayListIterator_create(manager->waitingForExport);

            hash_map_pt wiringAdminList = hashMap_create(NULL, NULL, NULL, NULL);
            hashMap_put(wiringAdminList, wiringAdminService, wEndpoint);
            hashMap_put(manager->exportedWiringEndpoints, srvcProperties, wiringAdminList);
        } else {
            printf("WTM: Could not export wire with new WA\n");
        }
    }

    arrayListIterator_destroy(waitList);

    celixThreadMutex_unlock(&manager->exportedWiringEndpointsLock);

    /* Check if the added WA can match one of the imported WiringEndpoints */
    celixThreadMutex_lock(&manager->importedWiringEndpointsLock);
    iter = hashMapIterator_create(manager->importedWiringEndpoints);
    while (hashMapIterator_hasNext(iter)) {
        hash_map_entry_pt entry = hashMapIterator_nextEntry(iter);
        wiring_endpoint_description_pt wEndpoint = hashMapEntry_getKey(entry);
        array_list_pt wiringAdminList = hashMapEntry_getValue(entry);

        status = wiringTopologyManager_checkWiringAdminForImportWiringEndpoint(manager, wiringAdminService, wEndpoint);

        if (status == CELIX_SUCCESS) {
            arrayList_add(wiringAdminList, wiringAdminService);
        }

    }
    hashMapIterator_destroy(iter);


    /* check wether waiting service can be exported */
    wiringTopologyManager_checkWaitingForImportServices(manager);

    celixThreadMutex_unlock(&manager->importedWiringEndpointsLock);

    printf("WTM: Added WA\n");

    return status;
}

celix_status_t wiringTopologyManager_waModified(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status = CELIX_SUCCESS;

    // Nothing to do here

    return status;
}

celix_status_t wiringTopologyManager_waRemoved(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status = CELIX_SUCCESS;
    wiring_topology_manager_pt manager = handle;
    wiring_admin_service_pt wiringAdminService = (wiring_admin_service_pt) service;

    /* check whether one of the exported Wires can be exported here via the newly available wiringAdmin*/
    celixThreadMutex_lock(&manager->exportedWiringEndpointsLock);
    hash_map_iterator_pt iter = hashMapIterator_create(manager->exportedWiringEndpoints);
    while (hashMapIterator_hasNext(iter)) {
        hash_map_entry_pt entry = hashMapIterator_nextEntry(iter);
        hash_map_pt wiringAdminMap = hashMapEntry_getValue(entry);

        if (hashMap_containsKey(wiringAdminMap, wiringAdminService)) {
            wiring_endpoint_description_pt wEndpoint = (wiring_endpoint_description_pt) hashMap_remove(wiringAdminMap, wiringAdminService);

            status = wiringTopologyManager_notifyListenersWiringEndpointRemoved(manager, wEndpoint);

            if (status == CELIX_SUCCESS) {
                status = wiringAdminService->removeExportedWiringEndpoint(wiringAdminService->admin, wEndpoint);
            } else {
                printf("WTM: failed while removing WiringAdmin.\n");
            }
        }
    }

    hashMapIterator_destroy(iter);

    celixThreadMutex_unlock(&manager->exportedWiringEndpointsLock);

    /* Check if the added WA can match one of the imported WiringEndpoints */
    celixThreadMutex_lock(&manager->importedWiringEndpointsLock);
    iter = hashMapIterator_create(manager->importedWiringEndpoints);
    while (hashMapIterator_hasNext(iter)) {
        hash_map_entry_pt entry = hashMapIterator_nextEntry(iter);
        wiring_endpoint_description_pt importedWiringEndpointDesc = hashMapEntry_getKey(entry);
        array_list_pt wiringAdminList = hashMapEntry_getValue(entry);

        if (arrayList_contains(wiringAdminList, wiringAdminService)) {
            status = wiringAdminService->removeImportedWiringEndpoint(wiringAdminService->admin, importedWiringEndpointDesc);
            arrayList_removeElement(wiringAdminList, wiringAdminService);
        }

        if (status == CELIX_SUCCESS) {
            arrayList_add(wiringAdminList, wiringAdminService);
        }

    }
    hashMapIterator_destroy(iter);
    celixThreadMutex_unlock(&manager->importedWiringEndpointsLock);

    celixThreadMutex_lock(&manager->waListLock);
    arrayList_removeElement(manager->waList, wiringAdminService);
    celixThreadMutex_unlock(&manager->waListLock);

    printf("WTM: Removed WA\n");

    return status;
}

celix_status_t wiringTopologyManager_getWAs(wiring_topology_manager_pt manager, array_list_pt *waList) {
    celix_status_t status = CELIX_SUCCESS;

    status = arrayList_create(waList);
    if (status != CELIX_SUCCESS) {
        return CELIX_ENOMEM;
    }

    status = celixThreadMutex_lock(&manager->waListLock);
    arrayList_addAll(*waList, manager->waList);
    status = celixThreadMutex_unlock(&manager->waListLock);

    return status;
}
