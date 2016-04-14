/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include <celix_errno.h>
#include <utils.h>

#include "wiring_endpoint_reader.h"

celix_status_t wiringEndpoint_properties_load(char *inStr, properties_pt properties) {

	celix_status_t status = CELIX_SUCCESS;

	char* line;
	char key[WIRING_ENDPOINT_PROP_MAX_KEY_LENGTH];
	char value[WIRING_ENDPOINT_PROP_MAX_VALUE_LENGTH];

	bool precedingCharIsBackslash = false;
	bool isComment = false;
	int linePos = 0;
	int outputPos = 0;
	char* output = NULL;

	char* delim = "\n";
	char* saveptr;

	if (properties == NULL) {
		status = CELIX_ILLEGAL_STATE;
	} else {

		line = strtok_r(inStr, delim , &saveptr);

		while (line != NULL) {
			linePos = 0;
			precedingCharIsBackslash = false;
			isComment = false;
			output = NULL;
			outputPos = 0;
			key[0] = '\0';
			value[0] = '\0';

			while (line[linePos] != '\0') {
				if (line[linePos] == ' ' || line[linePos] == '\t') {
					if (output == NULL) {
						//ignore
						linePos += 1;
						continue;
					} else {
						output[outputPos++] = line[linePos];
					}
				} else {
					if (output == NULL) {
						output = key;
					}
				}
				if (line[linePos] == '=' || line[linePos] == ':' || line[linePos] == '#' || line[linePos] == '!') {
					if (precedingCharIsBackslash) {
						//escaped special character
						output[outputPos++] = line[linePos];
						precedingCharIsBackslash = false;
					} else {
						if (line[linePos] == '#' || line[linePos] == '!') {
							if (outputPos == 0) {
								isComment = true;
								break;
							} else {
								output[outputPos++] = line[linePos];
							}
						} else { // = or :
							if (output == value) { //already have a seperator
								output[outputPos++] = line[linePos];
							} else {
								output[outputPos++] = '\0';
								output = value;
								outputPos = 0;
							}
						}
					}
				} else if (line[linePos] == '\\') {
					if (precedingCharIsBackslash) { //double backslash -> backslash
						output[outputPos++] = '\\';
					}
					precedingCharIsBackslash = true;
				} else { //normal character
					precedingCharIsBackslash = false;
					output[outputPos++] = line[linePos];
				}
				linePos += 1;
			}
			if (output != NULL) {
				output[outputPos] = '\0';
			}

			if (!isComment) {
				if (properties_get(properties, utils_stringTrim(key)) == NULL) {
					properties_set(properties, utils_stringTrim(key), utils_stringTrim(value));
				}
			}

			line = strtok_r(NULL, delim, &saveptr);
		}
	}

	return status;
}

