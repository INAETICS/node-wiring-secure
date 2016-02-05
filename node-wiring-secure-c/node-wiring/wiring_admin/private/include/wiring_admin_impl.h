/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef WIRING_ADMIN_IMPL_H_
#define WIRING_ADMIN_IMPL_H_

#include "remote_constants.h"
#include "constants.h"
#include "utils.h"
#include "bundle_context.h"
#include "bundle.h"
#include "service_reference.h"
#include "service_registration.h"
#include "celix_threads.h"

#include "wiring_admin.h"

#define MAX_URL_LENGTH 			128

//#define WIRING_ENDPOINT_DESCRIPTION_CONFIG_VALUE		"inaetics.wiring.http"

#define WIRING_ADMIN_PROPERTIES_CONFIG_VALUE		"inaetics.wiring.http"
#define WIRING_ADMIN_PROPERTIES_SECURE_VALUE 		"no"

#define TAG                                         "WIRING_ADMIN"

struct wiring_admin {
	bundle_context_pt context;

	celix_thread_mutex_t exportedWiringEndpointLock;
	celix_thread_mutex_t importedWiringEndpointLock;

	properties_pt adminProperties;

	hash_map_pt wiringSendServices; //key=wiring_endpoint_desc,  value=services
	hash_map_pt wiringSendRegistrations; //key=wiring_endpoint_desc,  value=serviceRegistrations

	hash_map_pt wiringReceiveServices; //key=wiring_endpoint_desc,  value=services
	hash_map_pt wiringReceiveTracker; //key=wiring_endpoint_desc,  value=tracker

	char url[MAX_URL_LENGTH];

	struct mg_context *ctx;
};

typedef struct wiring_proxy_registration {

	wiring_endpoint_description_pt wiringEndpointDescription;
	wiring_admin_pt wiringAdmin;

}* wiring_proxy_registration_pt;

celix_status_t wiringAdmin_create(bundle_context_pt context, wiring_admin_pt *admin);
celix_status_t wiringAdmin_destroy(wiring_admin_pt* admin);
celix_status_t wiringAdmin_stop(wiring_admin_pt admin);

celix_status_t wiringAdmin_exportWiringEndpoint(wiring_admin_pt admin, wiring_endpoint_description_pt* wEndpointDescription);
celix_status_t wiringAdmin_removeExportedWiringEndpoint(wiring_admin_pt admin, wiring_endpoint_description_pt wEndpointDescription);
celix_status_t wiringAdmin_getWiringAdminProperties(wiring_admin_pt admin, properties_pt *adminProperties);

celix_status_t wiringAdmin_importWiringEndpoint(wiring_admin_pt admin, wiring_endpoint_description_pt wEndpointDescription);
celix_status_t wiringAdmin_removeImportedWiringEndpoint(wiring_admin_pt admin, wiring_endpoint_description_pt wEndpointDescription);

#endif /* WIRING_ADMIN_IMPL_H_ */
