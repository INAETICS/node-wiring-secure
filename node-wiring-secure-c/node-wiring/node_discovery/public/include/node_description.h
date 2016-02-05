/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */


#ifndef NODE_DESCRIPTION_H_
#define NODE_DESCRIPTION_H_

typedef struct node_description *node_description_pt;

celix_status_t nodeDescription_create(char* nodeId, char* zoneId, properties_pt properties,node_description_pt *nodeDescription);
celix_status_t nodeDescription_destroy(node_description_pt nodeDescription,bool destroyWEPDs);

#endif
