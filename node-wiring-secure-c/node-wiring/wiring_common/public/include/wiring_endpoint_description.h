/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef WIRING_ENDPOINT_DESCRIPTION_H_
#define WIRING_ENDPOINT_DESCRIPTION_H_

#include "properties.h"
#include "array_list.h"
#include "remote_constants.h"

#define WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY			"org.inaetics.remote.admin.wiring.wireId"
#define WIRING_ENDPOINT_DESCRIPTION_HTTP_URL_KEY		"inaetics.wiring.http.url"


struct wiring_endpoint_description {
	properties_pt properties;
};

typedef struct wiring_endpoint_description *wiring_endpoint_description_pt;

celix_status_t wiringEndpointDescription_create(char* wireId, properties_pt properties, wiring_endpoint_description_pt *wiringEndpointDescription);
celix_status_t wiringEndpointDescription_destroy(wiring_endpoint_description_pt *description);
celix_status_t wiringEndpointDescription_getWireId(wiring_endpoint_description_pt description, char* wireId);
void wiringEndpointDescription_dump(wiring_endpoint_description_pt description);
unsigned int wiringEndpointDescription_hash(void* description);
int wiringEndpointDescription_equals(void* description1, void* description2);

#endif /* WIRING_ENDPOINT_DESCRIPTION_H_ */
