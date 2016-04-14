/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include "wiring_common_utils.h"

celix_status_t wiring_getIpAddress(char* interface, char** ip) {
	celix_status_t status = CELIX_BUNDLE_EXCEPTION;

	struct ifaddrs *ifaddr, *ifa;

	if (getifaddrs(&ifaddr) != -1)
	{
		for (ifa = ifaddr; ifa != NULL && status != CELIX_SUCCESS; ifa = ifa->ifa_next)
		{
			char host[NI_MAXHOST];

			if (ifa->ifa_addr == NULL)
				continue;

			if ((getnameinfo(ifa->ifa_addr,sizeof(struct sockaddr_in), host, NI_MAXHOST, NULL, 0, NI_NUMERICHOST) == 0) && (ifa->ifa_addr->sa_family == AF_INET)) {
				if (interface == NULL) {
					*ip = strdup(host);
					status = CELIX_SUCCESS;
				}
				else if (strcmp(ifa->ifa_name, interface) == 0) {
					*ip = strdup(host);
					status = CELIX_SUCCESS;
				}
			}
		}

		freeifaddrs(ifaddr);
	}

	return status;
}
