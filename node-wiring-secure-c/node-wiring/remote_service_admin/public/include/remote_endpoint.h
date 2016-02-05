/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef REMOTE_ENDPOINT_H_
#define REMOTE_ENDPOINT_H_

#define OSGI_RSA_REMOTE_ENDPOINT "remote_endpoint"

typedef struct remote_endpoint *remote_endpoint_pt;

struct remote_endpoint_service {
	remote_endpoint_pt endpoint;
	celix_status_t (*setService)(remote_endpoint_pt endpoint, void *service);
	celix_status_t (*handleRequest)(remote_endpoint_pt endpoint, char *data, char **reply);
};

typedef struct remote_endpoint_service *remote_endpoint_service_pt;


#endif /* REMOTE_ENDPOINT_H_ */
