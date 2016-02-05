/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef WIRING_TOPOLOGY_MANAGER_H_
#define WIRING_TOPOLOGY_MANAGER_H_

#include "wiring_endpoint_listener.h"
#include "wiring_admin.h"
#include "celix_errno.h"

static const char * const INAETICS_WIRING_TOPOLOGY_MANAGER_SERVICE = "wiring_topology_manager";

static const char * const INAETICS_WIRING_TOPOLOGY_MANAGER_SCOPE = "wiring.topology_manager.scope";

typedef struct wiring_topology_manager* wiring_topology_manager_pt;

struct wiring_topology_manager_service {
	wiring_topology_manager_pt manager;

	celix_status_t (*exportWiringEndpoint)(wiring_topology_manager_pt manager, properties_pt properties);
	celix_status_t (*removeExportedWiringEndpoint)(wiring_topology_manager_pt manager, properties_pt properties);

	celix_status_t (*importWiringEndpoint)(wiring_topology_manager_pt manager, properties_pt properties);
	celix_status_t (*removeImportedWiringEndpoint)(wiring_topology_manager_pt manager, properties_pt properties);

};

typedef struct wiring_topology_manager_service *wiring_topology_manager_service_pt;

#endif /* WIRING_TOPOLOGY_MANAGER_H_ */
