/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef WIRING_ENDPOINT_WRITER_H_
#define WIRING_ENDPOINT_WRITER_H_

#include <celix_errno.h>
#include <properties.h>

celix_status_t wiringEndpoint_properties_store(properties_pt properties, char* outStr);

#endif
