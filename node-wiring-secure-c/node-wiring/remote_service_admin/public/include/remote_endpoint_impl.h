/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef REMOTE_ENDPOINT_IMPL_H_
#define REMOTE_ENDPOINT_IMPL_H_

#include "remote_endpoint.h"
#include "celix_threads.h"

struct remote_endpoint {
	celix_thread_mutex_t serviceLock;
	void *service;
};

#endif /* REMOTE_ENDPOINT_IMPL_H_ */
