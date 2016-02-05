/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */


#ifndef NODE_DISCOVERY_IMPL_H_
#define NODE_DISCOVERY_IMPL_H_

#include "bundle_context.h"
#include "service_reference.h"
#include "node_description.h"
#include "wiring_endpoint_description.h"

#include "etcd_watcher.h"


#define NODE_DISCOVERY_DEFAULT_ZONE_IDENTIFIER	"inaetics-testing"


struct node_discovery {
	bundle_context_pt context;

	celix_thread_mutex_t ownNodeMutex;
	node_description_pt ownNode;

	celix_thread_mutex_t discoveredNodesMutex;
	hash_map_pt discoveredNodes; //key=nodeId (string), value=node_description_pt

	celix_thread_mutex_t listenerReferencesMutex;
	hash_map_pt listenerReferences; //key=serviceReference, value=nop

	etcd_watcher_pt watcher;
};


celix_status_t node_discovery_create(bundle_context_pt context, node_discovery_pt* node_discovery);
celix_status_t node_discovery_destroy(node_discovery_pt node_discovery);
celix_status_t node_discovery_start(node_discovery_pt node_discovery);
celix_status_t node_discovery_stop(node_discovery_pt node_discovery);

celix_status_t node_discovery_addNode(node_discovery_pt node_discovery, node_description_pt node_desc);
celix_status_t node_discovery_removeNode(node_discovery_pt node_discovery, node_description_pt removeRequest);

celix_status_t node_discovery_wiringEndpointListenerAdding(void * handle, service_reference_pt reference, void **service);
celix_status_t node_discovery_wiringEndpointListenerAdded(void * handle, service_reference_pt reference, void * service);
celix_status_t node_discovery_wiringEndpointListenerModified(void * handle, service_reference_pt reference, void * service);
celix_status_t node_discovery_wiringEndpointListenerRemoved(void * handle, service_reference_pt reference, void * service);

celix_status_t node_discovery_wiringEndpointAdded(void *handle, wiring_endpoint_description_pt wEndpoint, char *matchedFilter);
celix_status_t node_discovery_wiringEndpointRemoved(void *handle, wiring_endpoint_description_pt wEndpoint, char *matchedFilter);

celix_status_t node_discovery_informWiringEndpointListeners(node_discovery_pt discovery, wiring_endpoint_description_pt endpoint, bool endpointAdded);

#endif /* DISCOVERY_H_ */
