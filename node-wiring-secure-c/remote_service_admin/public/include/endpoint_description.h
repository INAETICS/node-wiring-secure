/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef ENDPOINT_DESCRIPTION_H_
#define ENDPOINT_DESCRIPTION_H_

#include "properties.h"
#include "array_list.h"

struct endpoint_description {
    char *frameworkUUID;
    char *id;
    // array_list_pt intents;
    char *service;
    // HASH_MAP packageVersions;
    properties_pt properties;
    long serviceId;
};

typedef struct endpoint_description *endpoint_description_pt;

celix_status_t endpointDescription_create(properties_pt properties, endpoint_description_pt *endpointDescription);
celix_status_t endpointDescription_destroy(endpoint_description_pt description);


#endif /* ENDPOINT_DESCRIPTION_H_ */
