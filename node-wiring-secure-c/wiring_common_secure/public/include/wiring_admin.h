/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef WIRING_ADMIN_H_
#define WIRING_ADMIN_H_

#include "wiring_endpoint_listener.h"

#include "celix_errno.h"

static const char * const INAETICS_WIRING_ADMIN = "wiring_admin_secure";

static const char * const INAETICS_WIRING_ADMIN_SCOPE = "wiring.admin.scope";

#define DEFAULT_WA_ADDRESS 	"127.0.0.1"
#define DEFAULT_WA_PORT		"6789"

#define NODE_DISCOVERY_NODE_WA_ADDRESS	"NODE_DISCOVERY_NODE_WA_ADDRESS"
#define NODE_DISCOVERY_NODE_WA_PORT		"NODE_DISCOVERY_NODE_WA_PORT"
#define NODE_DISCOVERY_NODE_WA_ITF 		"NODE_DISCOVERY_NODE_WA_ITF"

#define WIRING_ADMIN_PROPERTIES_CONFIG_KEY 				"inaetics.wiring.config"
#define WIRING_ADMIN_PROPERTIES_SECURE_KEY 				"inaetics.wiring.secure"

typedef struct wiring_admin *wiring_admin_pt;

struct wiring_admin_service {
	wiring_admin_pt admin;

	celix_status_t (*exportWiringEndpoint)(wiring_admin_pt admin, wiring_endpoint_description_pt* wEndpoint);
	celix_status_t (*removeExportedWiringEndpoint)(wiring_admin_pt admin, wiring_endpoint_description_pt wEndpoint);
	celix_status_t (*getWiringAdminProperties)(wiring_admin_pt admin, properties_pt* admin_properties);

	celix_status_t (*importWiringEndpoint)(wiring_admin_pt admin, wiring_endpoint_description_pt wEndpoint);
	celix_status_t (*removeImportedWiringEndpoint)(wiring_admin_pt admin, wiring_endpoint_description_pt wEndpoint);

};

typedef struct wiring_admin_service *wiring_admin_service_pt;

static const char * const INAETICS_WIRING_SEND_SERVICE = "wiring_send";
static const char * const INAETICS_WIRING_WIRE_ID = "wire.id";

typedef struct wiring_send_service *wiring_send_service_pt;

struct wiring_send_service {
	wiring_admin_pt admin;
	unsigned int errorCount;
	wiring_endpoint_description_pt wiringEndpointDescription;
	celix_status_t (*send)(wiring_send_service_pt sendService, char *request, char **reply, int* replyStatus);
};

#endif /* WIRING_ADMIN_H_ */
