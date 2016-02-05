/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef WIRING_COMMON_UTILS_H_
#define WIRING_COMMON_UTILS_H_

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <arpa/inet.h>
#include <sys/socket.h>
#include <netdb.h>
#include <ifaddrs.h>

#include "celix_errno.h"

celix_status_t wiring_getIpAddress(char* interface, char** ip);

#endif /* WIRING_COMMON_UTILS_H_ */

