/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef REMOTE_SERVICE_ADMIN_HTTP_IMPL_H_
#define REMOTE_SERVICE_ADMIN_HTTP_IMPL_H_

#include "remote_service_admin_impl.h"
#include "wiring_endpoint_description.h"
#include "wiring_endpoint_listener.h"
#include "log_helper.h"
#include "service_tracker.h"

struct activator {
    remote_service_admin_pt admin;
    remote_service_admin_service_pt adminService;
    service_registration_pt registration;

    wiring_endpoint_listener_pt wEndpointListener;
    service_registration_pt wEndpointListenerRegistration;

    service_tracker_pt eplTracker;
    service_tracker_pt wtmTracker;
};

struct remote_service_admin {
	bundle_context_pt context;
	log_helper_pt loghelper;

	celix_thread_mutex_t exportedServicesLock;
	hash_map_pt exportedServices;

	celix_thread_mutex_t importedServicesLock;
	hash_map_pt importedServices;

	hash_map_pt wiringReceiveServices;
	hash_map_pt wiringReceiveServiceRegistrations;

	array_list_pt exportedWires;

	service_tracker_pt sendServicesTracker;
	celix_thread_mutex_t sendServicesLock;
	hash_map_pt sendServices;

    celix_thread_mutex_t listenerListLock;
    hash_map_pt listenerList;

    celix_thread_mutex_t wtmListLock;
    array_list_pt wtmList;
};

celix_status_t remoteServiceAdmin_destroy(remote_service_admin_pt *admin);
celix_status_t remoteServiceAdmin_stop(remote_service_admin_pt admin);
celix_status_t remoteServiceAdmin_addWiringEndpoint(void *handle, wiring_endpoint_description_pt wEndpoint, char *matchedFilter);
celix_status_t remoteServiceAdmin_removeWiringEndpoint(void *handle, wiring_endpoint_description_pt wEndpoint, char *matchedFilter);

celix_status_t remoteServiceAdmin_endpointListenerAdding(void *handle, service_reference_pt reference, void **service);
celix_status_t remoteServiceAdmin_endpointListenerAdded(void *handle, service_reference_pt reference, void *service);
celix_status_t remoteServiceAdmin_endpointListenerModified(void *handle, service_reference_pt reference, void *service);
celix_status_t remoteServiceAdmin_endpointListenerRemoved(void *handle, service_reference_pt reference, void *service);

celix_status_t remoteServiceAdmin_wtmAdding(void *handle, service_reference_pt reference, void **service);
celix_status_t remoteServiceAdmin_wtmAdded(void *handle, service_reference_pt reference, void *service);
celix_status_t remoteServiceAdmin_wtmModified(void *handle, service_reference_pt reference, void *service);
celix_status_t remoteServiceAdmin_wtmRemoved(void *handle, service_reference_pt reference, void *service);

celix_status_t remoteServiceAdmin_getWTMs(remote_service_admin_pt admin, array_list_pt *wtmList);

#endif /* REMOTE_SERVICE_ADMIN_HTTP_IMPL_H_ */
