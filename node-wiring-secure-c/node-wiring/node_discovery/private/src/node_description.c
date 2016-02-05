/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <celix_errno.h>

#include "node_description_impl.h"
#include "wiring_endpoint_description.h"

celix_status_t nodeDescription_create(char* nodeId, char* zoneId,  properties_pt properties, node_description_pt *nodeDescription) {
	celix_status_t status = CELIX_SUCCESS;

	*nodeDescription = calloc(1, sizeof(struct node_description));

	if (properties != NULL) {
		(*nodeDescription)->properties = properties;
	} else {
		(*nodeDescription)->properties = properties_create();
	}

	if (nodeId != NULL) {
		(*nodeDescription)->nodeId = strdup(nodeId);
	} else {
		(*nodeDescription)->nodeId = NULL;
	}

	if (zoneId != NULL) {
		(*nodeDescription)->zoneId= strdup(zoneId);
	} else {
		(*nodeDescription)->zoneId  = NULL;
	}

	arrayList_create(&((*nodeDescription)->wiring_ep_descriptions_list));
	celixThreadMutex_create(&((*nodeDescription)->wiring_ep_desc_list_lock), NULL);

	return status;
}

celix_status_t nodeDescription_destroy(node_description_pt nodeDescription, bool destroyWEPDs) {
	celix_status_t status = CELIX_SUCCESS;

	if (nodeDescription->nodeId != NULL) {
		free(nodeDescription->nodeId);
	}

	if (nodeDescription->zoneId != NULL) {
		free(nodeDescription->zoneId);
	}

	if (nodeDescription->properties != NULL) {
		properties_destroy(nodeDescription->properties);
	}

	celixThreadMutex_lock(&nodeDescription->wiring_ep_desc_list_lock);

	if (nodeDescription->wiring_ep_descriptions_list != NULL) {
		/* Our own WiringEndpointDescriptions are destroyed by the owning WiringAmdin... No need to destroy them twice...*/

		if (destroyWEPDs == true) {
			array_list_iterator_pt ep_it = arrayListIterator_create(nodeDescription->wiring_ep_descriptions_list);

			while (arrayListIterator_hasNext(ep_it)) {
				wiring_endpoint_description_pt wep_desc = arrayListIterator_next(ep_it);
				wiringEndpointDescription_destroy(&wep_desc);
			}

			arrayListIterator_destroy(ep_it);
		}

		arrayList_destroy(nodeDescription->wiring_ep_descriptions_list);
	}

	celixThreadMutex_unlock(&nodeDescription->wiring_ep_desc_list_lock);

	celixThreadMutex_destroy(&nodeDescription->wiring_ep_desc_list_lock);

	free(nodeDescription);

	return status;
}

void dump_node_description(node_description_pt node_desc) {

	printf("\tNode Description Dump for Node %s\n", node_desc->nodeId);

	hash_map_iterator_pt node_props_it = hashMapIterator_create(node_desc->properties);

	while (hashMapIterator_hasNext(node_props_it)) {
		hash_map_entry_pt node_props_entry = hashMapIterator_nextEntry(node_props_it);
		char* key = (char*) hashMapEntry_getKey(node_props_entry);
		char* value = (char*) hashMapEntry_getValue(node_props_entry);
		printf("\t<%s=%s>\n", key, value);
	}

	hashMapIterator_destroy(node_props_it);

	printf("\t## Wiring Endpoints List ##\n");

	array_list_iterator_pt ep_it = arrayListIterator_create(node_desc->wiring_ep_descriptions_list);

	while (arrayListIterator_hasNext(ep_it)) {
		wiring_endpoint_description_pt wep_desc = arrayListIterator_next(ep_it);
		wiringEndpointDescription_dump(wep_desc);

	}

	arrayListIterator_destroy(ep_it);

	printf("\n");

}
