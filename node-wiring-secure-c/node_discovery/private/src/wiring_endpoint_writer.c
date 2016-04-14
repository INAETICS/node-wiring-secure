/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include <string.h>
#include <stdio.h>
#include <stdbool.h>
#include <celix_errno.h>

#include "etcd.h"
#include "wiring_endpoint_writer.h"
#include "wiring_endpoint_reader.h"

celix_status_t wiringEndpoint_properties_store(properties_pt properties, char* outStr) {

	celix_status_t status = CELIX_SUCCESS;


	char * const end = outStr + MAX_VALUE_LENGTH;

	if (hashMap_size(properties) > 0) {
		hash_map_iterator_pt iterator = hashMapIterator_create(properties);
		while (hashMapIterator_hasNext(iterator)) {
			hash_map_entry_pt entry = hashMapIterator_nextEntry(iterator);

			char key[WIRING_ENDPOINT_PROP_MAX_KEY_LENGTH];
			char value[WIRING_ENDPOINT_PROP_MAX_VALUE_LENGTH];

			char* keyStr = NULL;
			char* valStr = NULL;
			int keyLen = -1;
			int valLen = -1;
			int i = 0;
			int j = 0;

			keyStr = hashMapEntry_getKey(entry);
			valStr = hashMapEntry_getValue(entry);

			keyLen = strlen(keyStr);
			valLen = strlen(valStr);

			for (i = 0, j = 0; i < keyLen && i < WIRING_ENDPOINT_PROP_MAX_KEY_LENGTH; ++i, ++j) {
				if (keyStr[i] == '#' || keyStr[i] == '!' || keyStr[i] == '=' ) {
					key[j] = '\\';
					++j;
				}

				key[j] = keyStr[i];
			}
			key[j] = '\0';

			for (i = 0, j = 0; i < valLen; ++i, ++j) {
				if (valStr[i] == '#' || valStr[i] == '!' || valStr[i] == '=' ) {
					value[j] = '\\';
					++j;
				}

				value[j] = valStr[i];
			}
			value[j] = '\0';

			outStr += snprintf(outStr, end - outStr, "%s=%s\n", key, value);
		}
		hashMapIterator_destroy(iterator);
	}

	return status;
}
