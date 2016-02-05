/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */


#ifndef NODE_DESCRIPTION_IMPL_H_
#define NODE_DESCRIPTION_IMPL_H_

#include "properties.h"
#include "array_list.h"
#include "node_description.h"
#include "celix_threads.h"

struct node_description {
    char *nodeId;
    char *zoneId;

    celix_thread_mutex_t wiring_ep_desc_list_lock;
    array_list_pt wiring_ep_descriptions_list;
    properties_pt properties;
};

void dump_node_description(node_description_pt node_desc);

#endif
