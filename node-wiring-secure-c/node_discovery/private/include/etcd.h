/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#ifndef ETCD_H_
#define ETCD_H_

#include <stdbool.h>


#define MAX_WIRES			256
#define MAX_NODES			256
#define MAX_ZONES			64

#define MAX_KEY_LENGTH		256
#define MAX_VALUE_LENGTH	4096
#define MAX_ACTION_LENGTH	64

#define MAX_URL_LENGTH		256
#define MAX_CONTENT_LENGTH	16384

#define ETCD_JSON_NODE			"node"
#define ETCD_JSON_PREVNODE		"prevNode"
#define ETCD_JSON_NODES			"nodes"
#define ETCD_JSON_ACTION		"action"
#define ETCD_JSON_KEY			"key"
#define ETCD_JSON_VALUE			"value"
#define ETCD_JSON_MODIFIEDINDEX	"modifiedIndex"




bool etcd_init(char* server, int port);
bool etcd_get(char* key, char* value, char*action, int* modifiedIndex);
bool etcd_getEndpoints(char* directory, char** endpoints, int* size);
bool etcd_set(char* key, char* value, int ttl, bool prevExist);
bool etcd_del(char* key);
bool etcd_watch(char* key, int index, char* action, char* prevValue, char* value, char* rkey, int *modifiedIndex);

#endif /* ETCD_H_ */
