/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include <stdio.h>
#include <stdlib.h>

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <uuid/uuid.h>

#include <properties.h>
#include <service_tracker.h>
#include "curl/curl.h"

#include "remote_service_admin.h"
#include "remote_service_admin_inaetics.h"

#include "wiring_admin.h"
#include "wiring_admin_impl.h"
#include "wiring_common_utils.h"

#include "civetweb.h"

// defines how often the webserver is restarted (with an increased port number)
#define MAX_NUMBER_OF_RESTARTS 	5

struct post {
    const char *readptr;
    int size;
};

struct get {
    char *writeptr;
    int size;
};

static const char *data_response_headers = "HTTP/1.1 200 OK\r\n"
        "Cache: no-cache\r\n"
        "Content-Type: application/json\r\n"
        "\r\n";

static const char *no_content_response_headers = "HTTP/1.1 204 OK\r\n";

static int wiringAdmin_callback(struct mg_connection *conn);

static size_t wiringAdmin_HTTPReqReadCallback(void *ptr, size_t size, size_t nmemb, void *userp);
static size_t wiringAdmin_HTTPReqWrite(void *contents, size_t size, size_t nmemb, void *userp);

static celix_status_t wiringAdmin_wiringReceiveAdding(void * handle, service_reference_pt reference, void **service);
static celix_status_t wiringAdmin_wiringReceiveAdded(void * handle, service_reference_pt reference, void * service);
static celix_status_t wiringAdmin_wiringReceiveModified(void * handle, service_reference_pt reference, void * service);
static celix_status_t wiringAdmin_wiringReceiveRemoved(void * handle, service_reference_pt reference, void * service);

static celix_status_t wiringAdmin_send(wiring_send_service_pt sendService, char *request, char **reply, int* replyStatus);


celix_status_t wiringAdmin_create(bundle_context_pt context, wiring_admin_pt *admin) {
    celix_status_t status = CELIX_SUCCESS;

    *admin = calloc(1, sizeof(**admin));
    if (!*admin) {
        status = CELIX_ENOMEM;
    } else {
        (*admin)->context = context;

        (*admin)->wiringSendServices = hashMap_create(wiringEndpointDescription_hash, NULL, wiringEndpointDescription_equals, NULL);
        (*admin)->wiringSendRegistrations = hashMap_create(wiringEndpointDescription_hash, NULL, wiringEndpointDescription_equals, NULL);
        (*admin)->wiringReceiveServices = hashMap_create(utils_stringHash, NULL, utils_stringEquals, NULL);
        (*admin)->wiringReceiveTracker = hashMap_create(wiringEndpointDescription_hash, NULL, wiringEndpointDescription_equals, NULL);

        (*admin)->adminProperties = properties_create();

        properties_set((*admin)->adminProperties, WIRING_ADMIN_PROPERTIES_SECURE_KEY, WIRING_ADMIN_PROPERTIES_SECURE_VALUE);
        properties_set((*admin)->adminProperties, WIRING_ADMIN_PROPERTIES_CONFIG_KEY, WIRING_ADMIN_PROPERTIES_CONFIG_VALUE);

        celixThreadMutex_create(&(*admin)->exportedWiringEndpointLock, NULL);
        celixThreadMutex_create(&(*admin)->importedWiringEndpointLock, NULL);
    }

    return status;
}

celix_status_t wiringAdmin_getWiringAdminProperties(wiring_admin_pt admin, properties_pt *adminProperties) {

    celix_status_t status = CELIX_SUCCESS;

    *adminProperties = admin->adminProperties;

    return status;

}

celix_status_t wiringAdmin_startWebserver(bundle_context_pt context, wiring_admin_pt *admin) {
    celix_status_t status = CELIX_SUCCESS;

    unsigned int port_counter = 0;
    char *port = NULL;
    char *ip = NULL;
    char *detectedIp = NULL;

    bundleContext_getProperty(context, NODE_DISCOVERY_NODE_WA_PORT, &port);
    if (port == NULL) {
        port = (char *) DEFAULT_WA_PORT;
    }

    bundleContext_getProperty(context, NODE_DISCOVERY_NODE_WA_ADDRESS, &ip);
    if (ip == NULL) {
        char *interface = NULL;

        bundleContext_getProperty(context, NODE_DISCOVERY_NODE_WA_ITF, &interface);
        if ((interface != NULL) && (wiring_getIpAddress(interface, &detectedIp) != CELIX_SUCCESS)) {
            printf("%s: Could not retrieve IP adress for interface %s\n", TAG, interface);
        }

        if (ip == NULL) {
            wiring_getIpAddress(NULL, &detectedIp);
        }

        ip = detectedIp;
    }

    // Prepare callbacks structure. We have only one callback, the rest are NULL.
    struct mg_callbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.begin_request = wiringAdmin_callback;

    do {
        // secure port for civetweb config
        char securePort[10];
        snprintf(&securePort[0], 6, "%ss", port);
        char newPort[10];

        const char *options[] = {
                "listening_ports", securePort,
                "ssl_certificate", "/tmp/cinkeys/server.pem",
                "ssl_verify_peer", "yes",
//                "ssl_default_verify_paths", "no",
                "ssl_ca_file", "/tmp/cinkeys/ca.pem",
                NULL };

        (*admin)->ctx = mg_start(&callbacks, (*admin), options);

        if ((*admin)->ctx == NULL) {
            char* endptr = port;
            int currentPort = strtol(port, &endptr, 10);

            errno = 0;

            if (*endptr || errno != 0) {
                currentPort = strtol(DEFAULT_WA_PORT, NULL, 10);
            }

            port_counter++;
            snprintf(&newPort[0], 6, "%d", (currentPort + 1));

            printf("%s: Error while starting WIRING_ADMIN server on port %s - retrying on port %s...\n", TAG, port, newPort);
            port = newPort;
        }
    } while (((*admin)->ctx == NULL) && (port_counter < MAX_NUMBER_OF_RESTARTS));

    if (ip != NULL) {
        snprintf((*admin)->url, MAX_URL_LENGTH, "https://%s:%s", ip, port);
    } else {
        printf("%s: No IP address for HTTP Wiring Endpint set. Using %s\n", TAG, DEFAULT_WA_ADDRESS);
        snprintf((*admin)->url, MAX_URL_LENGTH, "https://%s:%s", (char*) DEFAULT_WA_ADDRESS, port);
    }

    if (detectedIp != NULL) {
        free(detectedIp);
    }

    return status;
}

celix_status_t wiringAdmin_stopWebserver(wiring_admin_pt admin) {
    celix_status_t status = CELIX_SUCCESS;

    if (admin->ctx != NULL) {
        printf("%s: Stopping HTTP Wiring Endpoint running at %s ...\n", TAG, admin->url);
        mg_stop(admin->ctx);
        admin->ctx = NULL;
    }

    return status;
}

celix_status_t wiringAdmin_destroy(wiring_admin_pt* admin) {
    celix_status_t status;

    status = wiringAdmin_stopWebserver(*admin);

    if (status == CELIX_SUCCESS) {
		celixThreadMutex_lock(&((*admin)->exportedWiringEndpointLock));
		hashMap_destroy((*admin)->wiringReceiveServices, false, false);
		hashMap_destroy((*admin)->wiringReceiveTracker, false, false);
		celixThreadMutex_unlock(&((*admin)->exportedWiringEndpointLock));
		celixThreadMutex_destroy(&((*admin)->exportedWiringEndpointLock));

		celixThreadMutex_lock(&((*admin)->importedWiringEndpointLock));
		hashMap_destroy((*admin)->wiringSendServices, false, false);
		hashMap_destroy((*admin)->wiringSendRegistrations, false, false);
		celixThreadMutex_unlock(&((*admin)->importedWiringEndpointLock));
		celixThreadMutex_destroy(&((*admin)->importedWiringEndpointLock));

		properties_destroy((*admin)->adminProperties);

		free(*admin);
		*admin = NULL;
    }

    return status;
}

celix_status_t wiringAdmin_stop(wiring_admin_pt admin) {
    celix_status_t status = CELIX_SUCCESS;

    celixThreadMutex_lock(&admin->exportedWiringEndpointLock);

    // stop tracker
    hash_map_iterator_pt iter = hashMapIterator_create(admin->wiringReceiveTracker);

    while (hashMapIterator_hasNext(iter)) {
        service_tracker_pt wiringReceiveTracker = (service_tracker_pt) hashMapIterator_nextValue(iter);

        if (serviceTracker_close(wiringReceiveTracker) == CELIX_SUCCESS) {
            serviceTracker_destroy(wiringReceiveTracker);
        }
    }
    hashMapIterator_destroy(iter);

    hashMap_clear(admin->wiringReceiveTracker, false, false);

    wiringAdmin_stopWebserver(admin);

    iter = hashMapIterator_create(admin->wiringReceiveServices);

    while (hashMapIterator_hasNext(iter)) {
        array_list_pt wiringReceiveServiceList = hashMapIterator_nextValue(iter);
        arrayList_destroy(wiringReceiveServiceList);
    }

    hashMapIterator_destroy(iter);
    hashMap_clear(admin->wiringReceiveServices, false, false);

    celixThreadMutex_unlock(&admin->exportedWiringEndpointLock);

    return status;
}

static int wiringAdmin_callback(struct mg_connection *conn) {
    int result = 0; // zero means: let civetweb handle it further, any non-zero value means it is handled by us...

    const struct mg_request_info *request_info = mg_get_request_info(conn);

    if (request_info->uri != NULL) {
        wiring_admin_pt admin = request_info->user_data;

        if (hashMap_size(admin->wiringReceiveServices) == 0) {
            printf("%s: No wiringReceiveServices available\n", TAG);
        }

        if (strcmp("POST", request_info->request_method) == 0) {

            celixThreadMutex_lock(&admin->exportedWiringEndpointLock);

            uint64_t datalength = request_info->content_length;
            char* data = malloc(datalength + 1);
            mg_read(conn, data, datalength);
            data[datalength] = '\0';

            char *response = NULL;

            hash_map_iterator_pt iter = hashMapIterator_create(admin->wiringReceiveServices);
            while (hashMapIterator_hasNext(iter)) {
                array_list_pt wiringReceiveServiceList = hashMapIterator_nextValue(iter);

                if (arrayList_size(wiringReceiveServiceList) > 0) {
                    //		printf("WIRING_ADMIN: size of wiringReceiveServiceList is %d\n", arrayList_size(wiringReceiveServiceList));
                    // TODO: we do not support mulitple wiringReceivers?
                    wiring_receive_service_pt wiringReceiveService = (wiring_receive_service_pt) arrayList_get(wiringReceiveServiceList, 0);
                    if (wiringReceiveService->receive(wiringReceiveService->handle, data, &response) != CELIX_SUCCESS) {
                        response = NULL;
                    }

                    break;
                } else {
                    printf("%s: wiringReceiveServiceList is empty\n", TAG);
                }
            }
            hashMapIterator_destroy(iter);

            if (response != NULL) {
                mg_write(conn, data_response_headers, strlen(data_response_headers));
                mg_write(conn, response, strlen(response));

                free(response);
            } else {
                mg_write(conn, no_content_response_headers, strlen(no_content_response_headers));
            }
            result = 1;

            free(data);
        } else {
            printf("%s: Received HTTP Request, but no RSA_Inaetics callback is installed. Discarding request.\n", TAG);
        }

        celixThreadMutex_unlock(&admin->exportedWiringEndpointLock);
    } else {
        printf("%s: Received URI is NULL\n", TAG);
    }

    return result;
}

static celix_status_t wiringAdmin_createWiringReceiveTracker(wiring_admin_pt admin, service_tracker_pt *tracker, char* wireId) {
    celix_status_t status;

    service_tracker_customizer_pt customizer = NULL;

    status = serviceTrackerCustomizer_create(admin, wiringAdmin_wiringReceiveAdding, wiringAdmin_wiringReceiveAdded, wiringAdmin_wiringReceiveModified, wiringAdmin_wiringReceiveRemoved, &customizer);

    if (status == CELIX_SUCCESS) {
        char filter[512];
        snprintf(filter, 512, "(&(%s=%s)(%s=%s))", (char*) OSGI_FRAMEWORK_OBJECTCLASS, (char*) INAETICS_WIRING_RECEIVE_SERVICE, (char*) INAETICS_WIRING_WIRE_ID, wireId);

        status = serviceTracker_createWithFilter(admin->context, filter, customizer, tracker);
    }

    return status;
}

static celix_status_t wiringAdmin_wiringReceiveAdding(void * handle, service_reference_pt reference, void **service) {
    celix_status_t status;

    wiring_admin_pt admin = handle;

    status = bundleContext_getService(admin->context, reference, service);

    return status;
}

static celix_status_t wiringAdmin_wiringReceiveAdded(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status = CELIX_SUCCESS;

    wiring_admin_pt admin = handle;
    wiring_receive_service_pt wiringReceiveService = (wiring_receive_service_pt) service;
    array_list_pt wiringReceiveServiceList = hashMap_get(admin->wiringReceiveServices, wiringReceiveService->wireId);

    printf("%s: wiringAdmin_wiringReceiveAdded, service w/ wireId %s added\n", TAG, wiringReceiveService->wireId);

    if (wiringReceiveServiceList == NULL) {
        arrayList_create(&wiringReceiveServiceList);
        hashMap_put(admin->wiringReceiveServices, wiringReceiveService->wireId, wiringReceiveServiceList);
    }

    arrayList_add(wiringReceiveServiceList, wiringReceiveService);

    return status;
}

static celix_status_t wiringAdmin_wiringReceiveModified(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status = CELIX_SUCCESS;

    return status;
}

static celix_status_t wiringAdmin_wiringReceiveRemoved(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status = CELIX_SUCCESS;

    wiring_admin_pt admin = handle;
    wiring_receive_service_pt wiringReceiveService = (wiring_receive_service_pt) service;
    array_list_pt wiringReceiveServiceList = hashMap_get(admin->wiringReceiveServices, wiringReceiveService->wireId);

    if (wiringReceiveServiceList != NULL) {
        arrayList_removeElement(wiringReceiveServiceList, wiringReceiveService);

        printf("%s: wiringAdmin_wiringReceiveRemoved, service w/ wireId %s removed!\n", TAG, wiringReceiveService->wireId);

        if (arrayList_size(wiringReceiveServiceList) == 0) {
            arrayList_destroy(wiringReceiveServiceList);
            hashMap_remove(admin->wiringReceiveServices, wiringReceiveService->wireId);
        }
    } else {
        printf("%s: wiringAdmin_wiringReceiveRemoved, service w/ wireId %s not found!\n", TAG, wiringReceiveService->wireId);
    }

    return status;
}

celix_status_t wiringAdmin_exportWiringEndpoint(wiring_admin_pt admin, wiring_endpoint_description_pt* wEndpointDescription) {
    celix_status_t status = CELIX_SUCCESS;

    celixThreadMutex_lock(&admin->exportedWiringEndpointLock);

    if (hashMap_size(admin->wiringReceiveTracker) == 0) {
        status = wiringAdmin_startWebserver(admin->context, &admin);
    }

    if (status == CELIX_SUCCESS) {
        char* fwuuid = NULL;

        status = bundleContext_getProperty(admin->context, OSGI_FRAMEWORK_FRAMEWORK_UUID, &fwuuid);

        if (status == CELIX_SUCCESS) {
            char* wireId = NULL;
            properties_pt props = properties_create();

            printf("%s: HTTP Wiring Endpoint running at %s\n", TAG, admin->url);

            status = wiringEndpointDescription_create(NULL, props, wEndpointDescription);

            properties_set(props, WIRING_ADMIN_PROPERTIES_CONFIG_KEY, WIRING_ADMIN_PROPERTIES_CONFIG_VALUE);
            properties_set(props, WIRING_ENDPOINT_DESCRIPTION_HTTP_URL_KEY, admin->url);
            properties_set(props, (char*) OSGI_RSA_ENDPOINT_FRAMEWORK_UUID, fwuuid);

            wireId = properties_get(props, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

            printf("%s: wiringEndpointDescription_create w/ wireId %s started\n", TAG, wireId);

            if (status == CELIX_SUCCESS) {
                service_tracker_pt tracker = NULL;
                status = wiringAdmin_createWiringReceiveTracker(admin, &tracker, wireId);

                if (status == CELIX_SUCCESS) {
                    status = serviceTracker_open(tracker);

                    if (status == CELIX_SUCCESS) {
                        hashMap_put(admin->wiringReceiveTracker, *wEndpointDescription, tracker);
                        printf("%s: WiringReceiveTracker w/ wireId %s started\n", TAG, wireId);
                    } else {
                        serviceTracker_destroy(tracker);
                    }
                }
            }
        }
    } else {
        printf("%s: Cannot export Wiring Endpoint\n", TAG);
    }

    celixThreadMutex_unlock(&admin->exportedWiringEndpointLock);

    return status;
}

celix_status_t wiringAdmin_removeExportedWiringEndpoint(wiring_admin_pt admin, wiring_endpoint_description_pt wEndpointDescription) {
    celix_status_t status = CELIX_SUCCESS;

    if (wEndpointDescription == NULL) {
        status = CELIX_ILLEGAL_ARGUMENT;
    } else {
        celixThreadMutex_lock(&admin->exportedWiringEndpointLock);
        service_tracker_pt wiringReceiveTracker = NULL;

        wiringReceiveTracker = hashMap_remove(admin->wiringReceiveTracker, wEndpointDescription);

        if (wiringReceiveTracker != NULL) {
            if (serviceTracker_close(wiringReceiveTracker) == CELIX_SUCCESS) {
                serviceTracker_destroy(wiringReceiveTracker);
            }

            if (hashMap_size(admin->wiringReceiveTracker) == 0) {
                wiringAdmin_stopWebserver(admin);
            }
        }

        wiringEndpointDescription_destroy(&wEndpointDescription);

        celixThreadMutex_unlock(&admin->exportedWiringEndpointLock);
    }

    return status;
}

celix_status_t wiringAdmin_importWiringEndpoint(wiring_admin_pt admin, wiring_endpoint_description_pt wEndpointDescription) {
    celix_status_t status = CELIX_SUCCESS;

    wiring_send_service_pt wiringSendService = calloc(1, sizeof(*wiringSendService));

    if (!wiringSendService) {
        status = CELIX_ENOMEM;
    } else {
        service_registration_pt wiringSendServiceReg = NULL;
        char* wireId = properties_get(wEndpointDescription->properties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

        properties_pt props = properties_create();
        properties_set(props, (char*) INAETICS_WIRING_WIRE_ID, wireId);

        wiringSendService->wiringEndpointDescription = wEndpointDescription;
        wiringSendService->send = wiringAdmin_send;
        wiringSendService->admin = admin;
        wiringSendService->errorCount = 0;

        status = bundleContext_registerService(admin->context, (char *) INAETICS_WIRING_SEND_SERVICE, wiringSendService, props, &wiringSendServiceReg);

        if (status == CELIX_SUCCESS) {

            hashMap_put(admin->wiringSendServices, wEndpointDescription, wiringSendService);
            hashMap_put(admin->wiringSendRegistrations, wEndpointDescription, wiringSendServiceReg);

            printf("%s: SEND SERVICE sucessfully registered w/ wireId %s\n", TAG, wireId);
        } else {
            printf("%s: could not register SEND SERVICE w/ wireId %s\n", TAG, wireId);

        }
    }

    return status;
}

celix_status_t wiringAdmin_removeImportedWiringEndpoint(wiring_admin_pt admin, wiring_endpoint_description_pt wEndpointDescription) {
    celix_status_t status;

    celixThreadMutex_lock(&admin->importedWiringEndpointLock);
    char* wireId = properties_get(wEndpointDescription->properties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

    printf("%s: remove Wiring Endpoint w/ wireId %s\n", TAG, wireId);

    wiring_send_service_pt wiringSendService = hashMap_remove(admin->wiringSendServices, wEndpointDescription);
    service_registration_pt wiringSendRegistration = hashMap_remove(admin->wiringSendRegistrations, wEndpointDescription);

    status = serviceRegistration_unregister(wiringSendRegistration);

    if (status == CELIX_SUCCESS) {
        free(wiringSendService);
    }

    celixThreadMutex_unlock(&admin->importedWiringEndpointLock);

    return status;
}

static celix_status_t wiringAdmin_send(wiring_send_service_pt sendService, char *request, char **reply, int* replyStatus) {

    celix_status_t status = CELIX_SUCCESS;

    struct post post;
    post.readptr = request;
    post.size = strlen(request);

    struct get get;
    get.size = 0;
    get.writeptr = malloc(1);

    char* url = properties_get(sendService->wiringEndpointDescription->properties, WIRING_ENDPOINT_DESCRIPTION_HTTP_URL_KEY);

    CURL *curl;
    CURLcode res;

    curl = curl_easy_init();
    if (!curl) {
        status = CELIX_ILLEGAL_STATE;
    } else {
        long http_code = 0;

        curl_easy_setopt(curl, CURLOPT_URL, url);
        curl_easy_setopt(curl, CURLOPT_POST, 1L);
        curl_easy_setopt(curl, CURLOPT_READFUNCTION, wiringAdmin_HTTPReqReadCallback);
        curl_easy_setopt(curl, CURLOPT_READDATA, &post);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, wiringAdmin_HTTPReqWrite);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *)&get);
        curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 2L);
        curl_easy_setopt(curl, CURLOPT_TIMEOUT, 2L);
        curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, (curl_off_t)post.size);
        res = curl_easy_perform(curl);

        curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE, &http_code);

        if (http_code == 200 && res != CURLE_ABORTED_BY_CALLBACK) {
            *replyStatus = res;
            *reply = get.writeptr;
        } else {
            *replyStatus = http_code;
            free(get.writeptr);
        }

        curl_easy_cleanup(curl);
    }

    return status;
}

static size_t wiringAdmin_HTTPReqReadCallback(void *ptr, size_t size, size_t nmemb, void *userp) {
    struct post *post = userp;

    if (post->size) {
        *(char *) ptr = post->readptr[0];
        post->readptr++;
        post->size--;
        return 1;
    }

    return 0;
}

static size_t wiringAdmin_HTTPReqWrite(void *contents, size_t size, size_t nmemb, void *userp) {
    size_t realsize = size * nmemb;
    struct get *mem = (struct get *) userp;

    mem->writeptr = realloc(mem->writeptr, mem->size + realsize + 1);
    if (mem->writeptr == NULL) {

        printf("not enough memory (realloc returned NULL)");
        exit(EXIT_FAILURE);
    }

    memcpy(&(mem->writeptr[mem->size]), contents, realsize);
    mem->size += realsize;
    mem->writeptr[mem->size] = 0;

    return realsize;
}
