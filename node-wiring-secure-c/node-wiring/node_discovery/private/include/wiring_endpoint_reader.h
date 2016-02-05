/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef WIRING_ENDPOINT_READER_H_
#define WIRING_ENDPOINT_READER_H_

#include <celix_errno.h>
#include <properties.h>

#define WIRING_ENDPOINT_PROP_MAX_LINE_LENGTH 	1024
#define WIRING_ENDPOINT_PROP_MAX_KEY_LENGTH	 	1024
#define WIRING_ENDPOINT_PROP_MAX_VALUE_LENGTH 	1024

celix_status_t wiringEndpoint_properties_load(char* inStr, properties_pt properties);

#endif
