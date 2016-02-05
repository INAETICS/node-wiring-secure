/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef WIRING_ENDPOINT_LISTENER_H_
#define WIRING_ENDPOINT_LISTENER_H_

#include "wiring_endpoint_description.h"

static const char * const INAETICS_WIRING_ENDPOINT_LISTENER_SERVICE = "wiring_endpoint_listener";

static const char * const INAETICS_WIRING_ENDPOINT_LISTENER_SCOPE = "wiring.endpoint.listener.scope";

struct wiring_endpoint_listener {
	void *handle;
	celix_status_t (*wiringEndpointAdded)(void *handle, wiring_endpoint_description_pt wEndpoint, char *matchedFilter);
	celix_status_t (*wiringEndpointRemoved)(void *handle, wiring_endpoint_description_pt wEndpoint, char *matchedFilter);
};

typedef struct wiring_endpoint_listener *wiring_endpoint_listener_pt;


#endif /* WIRING_ENDPOINT_LISTENER_H_ */
