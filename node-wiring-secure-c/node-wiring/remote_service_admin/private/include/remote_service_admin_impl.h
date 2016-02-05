/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef REMOTE_SERVICE_ADMIN_IMPL_H_
#define REMOTE_SERVICE_ADMIN_IMPL_H_

#include "remote_service_admin.h"

#define BUNDLE_STORE_PROPERTY_NAME "ENDPOINTS"
#define DEFAULT_BUNDLE_STORE "endpoints"


struct export_reference {
	endpoint_description_pt endpoint;
	service_reference_pt reference;
};

struct import_reference {
	endpoint_description_pt endpoint;
	service_reference_pt reference;
};

celix_status_t remoteServiceAdmin_create(bundle_context_pt context, remote_service_admin_pt *admin);
celix_status_t remoteServiceAdmin_destroy(remote_service_admin_pt *admin);

celix_status_t remoteServiceAdmin_send(remote_service_admin_pt rsa, endpoint_description_pt endpointDescription, char *methodSignature, char **reply, int* replyStatus);

celix_status_t remoteServiceAdmin_exportService(remote_service_admin_pt admin, char *serviceId, properties_pt properties, array_list_pt *registrations);
celix_status_t remoteServiceAdmin_removeExportedService(remote_service_admin_pt admin, export_registration_pt registration);
celix_status_t remoteServiceAdmin_getExportedServices(remote_service_admin_pt admin, array_list_pt *services);
celix_status_t remoteServiceAdmin_getImportedEndpoints(remote_service_admin_pt admin, array_list_pt *services);
celix_status_t remoteServiceAdmin_importService(remote_service_admin_pt admin, endpoint_description_pt endpoint, import_registration_pt *registration);
celix_status_t remoteServiceAdmin_removeImportedService(remote_service_admin_pt admin, import_registration_pt registration);

celix_status_t remoteServiceAdmin_destroyEndpointDescription(endpoint_description_pt *description);

#endif /* REMOTE_SERVICE_ADMIN_IMPL_H_ */
