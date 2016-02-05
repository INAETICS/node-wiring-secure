/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef WIRING_TOPOLOGY_MANAGER_IMPL_H_
#define WIRING_TOPOLOGY_MANAGER_IMPL_H_

#include "wiring_topology_manager.h"
#include "wiring_endpoint_listener.h"
#include "service_tracker.h"
#include "bundle_context.h"


struct wiring_topology_manager {
    bundle_context_pt context;

    celix_thread_mutex_t waListLock;
    array_list_pt waList;

    celix_thread_mutex_t listenerListLock;
    hash_map_pt listenerList;


    //  key = srvcproperties_hash, val = hashmap (key = Wa, val = endpoint)
    celix_thread_mutex_t exportedWiringEndpointsLock;
    hash_map_pt exportedWiringEndpoints;

    array_list_pt waitingForExport;
    array_list_pt waitingForImport;

    celix_thread_mutex_t importedWiringEndpointsLock;
    hash_map_pt importedWiringEndpoints;

};

celix_status_t wiringTopologyManager_create(bundle_context_pt context, wiring_topology_manager_pt *manager);
celix_status_t wiringTopologyManager_destroy(wiring_topology_manager_pt manager);

// see wtm_wadmin_tracker.c
celix_status_t wiringTopologyManager_createWaTracker(wiring_topology_manager_pt manager, service_tracker_pt *tracker);
celix_status_t wiringTopologyManager_waAdding(void *handle, service_reference_pt reference, void **service);
celix_status_t wiringTopologyManager_waAdded(void *handle, service_reference_pt reference, void *service);
celix_status_t wiringTopologyManager_waModified(void *handle, service_reference_pt reference, void *service);
celix_status_t wiringTopologyManager_waRemoved(void *handle, service_reference_pt reference, void *service);
celix_status_t wiringTopologyManager_getWAs(wiring_topology_manager_pt manager, array_list_pt *waList);


// see wtm_wendpointlistener_tracker.c
celix_status_t wiringTopologyManager_createWiringEndpointListenerTracker(wiring_topology_manager_pt manager, service_tracker_pt *tracker);
celix_status_t wiringTopologyManager_wiringEndpointListenerAdding(void *handle, service_reference_pt reference, void **service);
celix_status_t wiringTopologyManager_wiringEndpointListenerAdded(void *handle, service_reference_pt reference, void *service);
celix_status_t wiringTopologyManager_wiringEndpointListenerModified(void *handle, service_reference_pt reference, void *service);
celix_status_t wiringTopologyManager_wiringEndpointListenerRemoved(void *handle, service_reference_pt reference, void *service);

celix_status_t wiringTopologyManager_notifyListenersWiringEndpointAdded(wiring_topology_manager_pt manager, wiring_endpoint_description_pt wEndpoint);
celix_status_t wiringTopologyManager_notifyListenersWiringEndpointRemoved(wiring_topology_manager_pt manager, wiring_endpoint_description_pt wEndpoint);


celix_status_t wiringTopologyManager_WiringEndpointAdded(void *handle, wiring_endpoint_description_pt endpoint, char *matchedFilter);
celix_status_t wiringTopologyManager_WiringEndpointRemoved(void *handle, wiring_endpoint_description_pt endpoint, char *matchedFilter);

celix_status_t wiringTopologyManager_exportWiringEndpoint(wiring_topology_manager_pt manager, properties_pt properties);
celix_status_t wiringTopologyManager_removeExportedWiringEndpoint(wiring_topology_manager_pt manager, properties_pt properties);

celix_status_t wiringTopologyManager_importWiringEndpoint(wiring_topology_manager_pt manager, properties_pt properties);
celix_status_t wiringTopologyManager_removeImportedWiringEndpoint(wiring_topology_manager_pt manager, properties_pt properties);

celix_status_t wiringTopologyManager_WiringAdminServiceExportWiringEndpoint(wiring_topology_manager_pt manager, wiring_admin_service_pt wiringAdminService, properties_pt srvcProperties,  wiring_endpoint_description_pt* wEndpoint);
celix_status_t wiringTopologyManager_checkWiringAdminForImportWiringEndpoint(wiring_topology_manager_pt manager, wiring_admin_service_pt wiringAdminService, wiring_endpoint_description_pt wEndpoint);
celix_status_t wiringTopologyManager_checkWiringEndpointForImportService(wiring_topology_manager_pt manager, wiring_endpoint_description_pt wiringEndpointDesc, properties_pt requiredProperties);
celix_status_t wiringTopologyManager_checkWaitingForImportServices(wiring_topology_manager_pt manager);

#endif /* WIRING_TOPOLOGY_MANAGER_IMPL_H_ */
