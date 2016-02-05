/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */


#ifndef ETCD_WATCHER_H_
#define ETCD_WATCHER_H_

#include "bundle_context.h"
#include "celix_errno.h"
#include "node_discovery.h"
#include "node_description.h"

typedef struct etcd_watcher *etcd_watcher_pt;

celix_status_t etcdWatcher_create(node_discovery_pt discovery,  bundle_context_pt context, etcd_watcher_pt *watcher);
celix_status_t etcdWatcher_destroy(etcd_watcher_pt watcher);

celix_status_t etcdWatcher_getWiringEndpointFromKey(node_discovery_pt discovery, char* key, char* value, node_description_pt* nodeDescription);
celix_status_t etcdWatcher_addOwnNode(etcd_watcher_pt watcher);

#endif /* ETCD_WATCHER_H_ */
