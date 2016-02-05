/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef EXPORT_REGISTRATION_IMPL_H_
#define EXPORT_REGISTRATION_IMPL_H_

#include "remote_service_admin.h"
#include "remote_endpoint.h"
#include "service_tracker.h"
#include "log_helper.h"

struct export_registration {
	bundle_context_pt context;
	remote_service_admin_pt rsa;
	endpoint_description_pt endpointDescription;
	service_reference_pt reference;
	log_helper_pt loghelper;

	service_tracker_pt tracker;
	service_tracker_pt endpointTracker;

	remote_endpoint_service_pt endpoint;

	export_reference_pt exportReference;
	bundle_pt bundle;

	bool closed;
};

celix_status_t exportRegistration_create(log_helper_pt helper, service_reference_pt reference, endpoint_description_pt endpoint, remote_service_admin_pt rsa, bundle_context_pt context, export_registration_pt *registration);
celix_status_t exportRegistration_destroy(export_registration_pt *registration);
celix_status_t exportRegistration_open(export_registration_pt registration);
celix_status_t exportRegistration_close(export_registration_pt registration);
celix_status_t exportRegistration_getException(export_registration_pt registration);
celix_status_t exportRegistration_getExportReference(export_registration_pt registration, export_reference_pt *reference);

celix_status_t exportRegistration_setEndpointDescription(export_registration_pt registration, endpoint_description_pt endpointDescription);
celix_status_t exportRegistration_startTracking(export_registration_pt registration);
celix_status_t exportRegistration_stopTracking(export_registration_pt registration);

#endif /* EXPORT_REGISTRATION_IMPL_H_ */
