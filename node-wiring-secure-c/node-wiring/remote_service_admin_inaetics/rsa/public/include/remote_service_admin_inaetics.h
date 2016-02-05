/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef REMOTE_SERVICE_ADMIN_INAETICS_H_
#define REMOTE_SERVICE_ADMIN_INAETICS_H_

#include "celix_errno.h"

static const char * const INAETICS_WIRING_RECEIVE_SERVICE = "wiring_receive";

struct wiring_receive_service {
	char* wireId;
	void* handle;
	celix_status_t (*receive)(void* handle, char* data, char** response);
};

typedef struct wiring_receive_service *wiring_receive_service_pt;


#endif /* REMOTE_SERVICE_ADMIN_HTTP_IMPL_H_ */
