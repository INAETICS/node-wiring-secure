/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */

#include <stdio.h>
#include <stdlib.h>

#include <arpa/inet.h>
#include <sys/socket.h>
#include <netdb.h>
#include <ifaddrs.h>
#include <jansson.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <uuid/uuid.h>

#include <curl/curl.h>

#include "remote_service_admin_inaetics.h"
#include "remote_service_admin_inaetics_impl.h"
#include "export_registration_dfi.h"
#include "import_registration_dfi.h"
#include "remote_constants.h"
#include "constants.h"
#include "utils.h"
#include "bundle_context.h"
#include "bundle.h"
#include "service_reference.h"
#include "service_registration.h"
#include "log_helper.h"
#include "log_service.h"
#include "celix_threads.h"

#include "wiring_topology_manager.h"
#include "wiring_endpoint_listener.h"
#include "wiring_endpoint_description.h"
#include "wiring_admin.h"

static const char * const CONFIGURATION_TYPE = "org.inaetics.remote.admin.wiring";

static celix_status_t remoteServiceAdmin_registerReceive(remote_service_admin_pt admin, char* wireId);
static celix_status_t remoteServiceAdmin_unregisterReceive(remote_service_admin_pt admin, char* wireId);

static celix_status_t remoteServiceAdmin_createSendServiceTracker(remote_service_admin_pt admin);
static celix_status_t remoteServiceAdmin_destroySendServiceTracker(remote_service_admin_pt admin);

static celix_status_t remoteServiceAdmin_sendServiceAdding(void * handle, service_reference_pt reference, void **service);
static celix_status_t remoteServiceAdmin_sendServiceAdded(void * handle, service_reference_pt reference, void * service);
static celix_status_t remoteServiceAdmin_sendServiceModified(void * handle, service_reference_pt reference, void * service);
static celix_status_t remoteServiceAdmin_sendServiceRemoved(void * handle, service_reference_pt reference, void * service);
static celix_status_t remoteServiceAdmin_createEndpointDescription(remote_service_admin_pt admin, service_reference_pt reference, char *interface, endpoint_description_pt *endpoint);

static celix_status_t remoteServiceAdmin_wireIdEquals(void *a, void *b, bool *equals);
static unsigned int remoteServiceAdmin_endpointDescription_hash(void* description);
static int remoteServiceAdmin_endpointDescription_equals(void* description1, void* description2);

celix_status_t remoteServiceAdmin_notifyListenersEndpointAdded(remote_service_admin_pt admin, array_list_pt registrations);

celix_status_t remoteServiceAdmin_installEndpoint(remote_service_admin_pt admin, export_registration_pt registration, service_reference_pt reference, char *interface);


celix_status_t remoteServiceAdmin_create(bundle_context_pt context, remote_service_admin_pt *admin) {
    celix_status_t status = CELIX_SUCCESS;

    *admin = calloc(1, sizeof(**admin));

    if (!*admin) {
        status = CELIX_ENOMEM;
    } else {
        (*admin)->context = context;
        arrayList_create(&(*admin)->wtmList);

        (*admin)->exportedServices = hashMap_create(NULL, NULL, NULL, NULL);
        (*admin)->importedServices = hashMap_create(remoteServiceAdmin_endpointDescription_hash, NULL, remoteServiceAdmin_endpointDescription_equals, NULL);
        (*admin)->wiringReceiveServices = hashMap_create(utils_stringHash, NULL, utils_stringEquals, NULL);
        (*admin)->wiringReceiveServiceRegistrations = hashMap_create(utils_stringHash, NULL, utils_stringEquals, NULL);
        (*admin)->sendServicesTracker = NULL;
        (*admin)->sendServices = hashMap_create(utils_stringHash, NULL, utils_stringEquals, NULL);
        (*admin)->listenerList = hashMap_create(serviceReference_hashCode, NULL, serviceReference_equals2, NULL);

        arrayList_createWithEquals(remoteServiceAdmin_wireIdEquals, &((*admin)->exportedWires));

        status = celixThreadMutex_create(&(*admin)->listenerListLock, NULL);
        celixThreadMutex_create(&(*admin)->wtmListLock, NULL);
        celixThreadMutex_create(&(*admin)->sendServicesLock, NULL);
        celixThreadMutex_create(&(*admin)->exportedServicesLock, NULL);
        celixThreadMutex_create(&(*admin)->importedServicesLock, NULL);

        if (logHelper_create(context, &(*admin)->loghelper) == CELIX_SUCCESS) {
            logHelper_start((*admin)->loghelper);
        }

        status = remoteServiceAdmin_createSendServiceTracker(*admin);

    }
    return status;
}

celix_status_t remoteServiceAdmin_destroy(remote_service_admin_pt *admin) {
    celix_status_t status = CELIX_SUCCESS;

    hashMap_destroy((*admin)->wiringReceiveServices, false, false);
    hashMap_destroy((*admin)->wiringReceiveServiceRegistrations, false, false);
    hashMap_destroy((*admin)->sendServices, false, false);
    hashMap_destroy((*admin)->listenerList, false, false);

    arrayList_destroy((*admin)->exportedWires);

    celixThreadMutex_destroy(&(*admin)->sendServicesLock);
    celixThreadMutex_destroy(&(*admin)->exportedServicesLock);
    celixThreadMutex_destroy(&(*admin)->importedServicesLock);

    arrayList_destroy((*admin)->wtmList);


    free(*admin);

    *admin = NULL;

    return status;
}

celix_status_t remoteServiceAdmin_stop(remote_service_admin_pt admin) {
    celix_status_t status = CELIX_SUCCESS;


    celixThreadMutex_lock(&admin->exportedServicesLock);

    hash_map_iterator_pt iter = hashMapIterator_create(admin->exportedServices);
    while (hashMapIterator_hasNext(iter)) {
        array_list_pt exports = hashMapIterator_nextValue(iter);
        int i;
        for (i = 0; i < arrayList_size(exports); i++) {
            export_registration_pt export = (export_registration_pt) arrayList_get(exports, i);
               if (export != NULL) {
                   exportRegistration_stop(export);
               }
        }
    }
    hashMapIterator_destroy(iter);
    celixThreadMutex_unlock(&admin->exportedServicesLock);


    celixThreadMutex_lock(&admin->importedServicesLock);

    iter = hashMapIterator_create(admin->importedServices);
    while (hashMapIterator_hasNext(iter)) {
        import_registration_pt import = (import_registration_pt) hashMapIterator_nextValue(iter);

        if (import != NULL) {
             importRegistration_stop(import);
         }

    }

    hashMapIterator_destroy(iter);
    celixThreadMutex_unlock(&admin->importedServicesLock);

    hashMap_destroy(admin->exportedServices, false, false);
    hashMap_destroy(admin->importedServices, false, false);

    status = remoteServiceAdmin_destroySendServiceTracker(admin);

    if (logHelper_stop(admin->loghelper) == CELIX_SUCCESS) {
        logHelper_destroy(&admin->loghelper);
    }

    return status;
}

static celix_status_t remoteServiceAdmin_wireIdEquals(void *a, void *b, bool *equals) {
    *equals = (strcmp(a, b) == 0);
    return CELIX_SUCCESS;
}


static unsigned int remoteServiceAdmin_endpointDescription_hash(void* description) {

    endpoint_description_pt endpointDesc = (endpoint_description_pt) description;
    if (endpointDesc != NULL) {
        char serviceNameId[512];

        snprintf(serviceNameId, 512, "%s%s", endpointDesc->service, endpointDesc->id);
        return (utils_stringHash(serviceNameId));
    }

    return 0;
}

static int remoteServiceAdmin_endpointDescription_equals(void* description1, void* description2) {

    endpoint_description_pt endpointDesc1 = (endpoint_description_pt) description1;
    endpoint_description_pt endpointDesc2 = (endpoint_description_pt) description2;

    if (endpointDesc1 == NULL || endpointDesc2 == NULL) {
        return 0;
    }
    else {
        return (endpointDesc1->serviceId == endpointDesc2->serviceId) && !(strcmp(endpointDesc1->service, endpointDesc2->service));
    }
}

static celix_status_t remoteServiceAdmin_receive(void* handle, char* data, char**response) {
    celix_status_t status = CELIX_ILLEGAL_ARGUMENT;
    remote_service_admin_pt admin = (remote_service_admin_pt) handle;

    //celixThreadMutex_lock(&admin->exportedServicesLock);
    json_error_t jsonError;
    json_t* root;

    root = json_loads(data, 0, &jsonError);

    if (root) {
        int serviceId = -1;
        char* request = NULL;
        json_t* json_request = NULL;

        json_unpack(root, "{s:i, s:o}", "service.id", &serviceId, "request", &json_request);
        request = json_dumps(json_request, 0);

        //find endpoint
        export_registration_pt export = NULL;
        hash_map_iterator_pt iter = hashMapIterator_create(admin->exportedServices);
        while (hashMapIterator_hasNext(iter)) {
            hash_map_entry_pt entry = hashMapIterator_nextEntry(iter);
            array_list_pt exports = hashMapEntry_getValue(entry);
            int expIt = 0;
            for (expIt = 0; expIt < arrayList_size(exports); expIt++) {
                export_registration_pt check = arrayList_get(exports, expIt);
                export_reference_pt ref = NULL;
                exportRegistration_getExportReference(check, &ref);
                endpoint_description_pt checkEndpoint = NULL;
                exportReference_getExportedEndpoint(ref, &checkEndpoint);
                if (ref != NULL) {
                    free(ref);
                }
                if (serviceId == checkEndpoint->serviceId) {
                    export = check;
                    break;
                }
            }
        }
        hashMapIterator_destroy(iter);

        if (export != NULL) {
            int responceLength = 0;

            status = exportRegistration_call(export, request, -1, response, &responceLength);

            if (status != CELIX_SUCCESS) {
                logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: Error trying to invoke remove service, got error %i\n", status);
            }
        }
        free(request);
        json_decref(root);

        //celixThreadMutex_unlock(&admin->exportedServicesLock);

    }

    return status;
}

/* Functions for wiring endpoint listener */
celix_status_t remoteServiceAdmin_addWiringEndpoint(void *handle, wiring_endpoint_description_pt wEndpoint, char *matchedFilter) {
    celix_status_t status;

    remote_service_admin_pt admin = (remote_service_admin_pt) handle;

    char* ownUuid = NULL;

    status = bundleContext_getProperty(admin->context, OSGI_FRAMEWORK_FRAMEWORK_UUID, &ownUuid);

    if (status == CELIX_SUCCESS) {
        char* wireId = NULL;
        char* wireUuid = NULL;

        wireId = properties_get(wEndpoint->properties, (char*) WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);
        wireUuid = properties_get(wEndpoint->properties, (char*) OSGI_RSA_ENDPOINT_FRAMEWORK_UUID);

        /* added wiring enpoint is used for export */
        if (wireUuid != NULL && strcmp(ownUuid, wireUuid) == 0) {
            char* exportServiceId = properties_get(wEndpoint->properties, (char*) "requested.service.id");

            logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: exported wire available %s for serviceId %s\n", wireUuid, exportServiceId);

            if (!arrayList_contains(admin->exportedWires, wireId)) {
                arrayList_add(admin->exportedWires, wireId);
            }

            // go through all references to find correct serviceId
            status = celixThreadMutex_lock(&admin->exportedServicesLock);
            hash_map_iterator_pt exportedServicesIterator = hashMapIterator_create(admin->exportedServices);

            while (hashMapIterator_hasNext(exportedServicesIterator)) {
                int i = 0;
                char* id_str;

                hash_map_entry_pt entry = hashMapIterator_nextEntry(exportedServicesIterator);
                service_reference_pt reference = hashMapEntry_getKey(entry);

                serviceReference_getProperty(reference, (char *) OSGI_FRAMEWORK_SERVICE_ID, &id_str);

                if (id_str == NULL) {
                    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: service reference misses framework service id\n");
                }
                else if (strcmp(id_str, exportServiceId) == 0) {
                    array_list_pt registrations = hashMapEntry_getValue(entry);
                    for (i = 0; i < arrayList_size(registrations); i++) {

                        export_registration_pt export = arrayList_get(registrations, i);
                        export_reference_pt reference = NULL;
                        endpoint_description_pt endpoint = NULL;

                        exportRegistration_getExportReference(export, &reference);
                        exportReference_getExportedEndpoint(reference, &endpoint);

                        properties_set(endpoint->properties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY, wireId);
                        free(reference);
                    }

                    status = remoteServiceAdmin_registerReceive(admin, wireId);
                    if (status == CELIX_SUCCESS) {
                        // inform endpoint listener
                        remoteServiceAdmin_notifyListenersEndpointAdded(admin, registrations);
                    }
                }

            }

            hashMapIterator_destroy(exportedServicesIterator);

            status = celixThreadMutex_unlock(&admin->exportedServicesLock);
        }
        /* added wiring endpoint is used for import */
        else {
            char* id = properties_get(wEndpoint->properties, "requested.service");

            logHelper_log(admin->loghelper, OSGI_LOGSERVICE_DEBUG, "RSA: imported wiring endpoint available - wire %s service %s\n", wireUuid, id);

            celixThreadMutex_lock(&admin->importedServicesLock);

            if (hashMap_size(admin->importedServices) == 0) {
                logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: No imported services found");
            } else {
                // TODO: add additional check for serviceName/fwId
                hash_map_iterator_pt importedServicesIterator = hashMapIterator_create(admin->importedServices);
                while (hashMapIterator_hasNext(importedServicesIterator)) {
                    hash_map_entry_pt entry = hashMapIterator_nextEntry(importedServicesIterator);
                    endpoint_description_pt endpointDescription = (endpoint_description_pt) hashMapEntry_getKey(entry);

                    if (endpointDescription != NULL) {
                        if (strcmp(id, endpointDescription->id) == 0) {
                            import_registration_pt import = (import_registration_pt) hashMapEntry_getValue(entry);

                            if (import == NULL) {
                                logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: No import registration found for requested service");
                                status = CELIX_BUNDLE_EXCEPTION;
                            }

                            if (status == CELIX_SUCCESS) {
                                importRegistration_setSendFn(import, (send_func_type) remoteServiceAdmin_send, admin);
                            }

                            if (status == CELIX_SUCCESS) {
                                logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: Registering service factory (proxy) for %s w/ wireId %s.", endpointDescription->service, wireId);
                                status = importRegistration_start(import);
                            }
                        }
                    }
                }

                hashMapIterator_destroy(importedServicesIterator);
            }

            celixThreadMutex_unlock(&admin->importedServicesLock);
        }
    }

    return status;
}

celix_status_t remoteServiceAdmin_removeWiringEndpoint(void *handle, wiring_endpoint_description_pt wEndpoint, char *matchedFilter) {
    celix_status_t status;

    remote_service_admin_pt admin = (remote_service_admin_pt) handle;
    char* wireId = properties_get(wEndpoint->properties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: Removing wiring endpoint w/ wireId %s\n", wireId);

    /* handle exported Wires */
    array_list_pt exportRegistrationList = NULL;
    arrayList_create(&exportRegistrationList);

    // go through all references to find correct serviceId
    status = celixThreadMutex_lock(&admin->exportedServicesLock);
    hash_map_iterator_pt exportedServicesIterator = hashMapIterator_create(admin->exportedServices);

    while (hashMapIterator_hasNext(exportedServicesIterator)) {
        int i = 0;

        hash_map_entry_pt entry = hashMapIterator_nextEntry(exportedServicesIterator);
        array_list_pt registrations = hashMapEntry_getValue(entry);
        for (i = 0; i < arrayList_size(registrations); i++) {

            export_registration_pt export = arrayList_get(registrations, i);
            export_reference_pt reference = NULL;
            endpoint_description_pt endpoint = NULL;

            exportRegistration_getExportReference(export, &reference);
            exportReference_getExportedEndpoint(reference, &endpoint);

            char* regWireId = properties_get(endpoint->properties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

            if (strcmp(wireId, regWireId) == 0) {
                arrayList_add(exportRegistrationList, export);
            }
            free(reference);
        }
    }

    hashMapIterator_destroy(exportedServicesIterator);

    status = celixThreadMutex_unlock(&admin->exportedServicesLock);

    int i;

    for (i = 0; i < arrayList_size(exportRegistrationList); i++) {
        export_registration_pt export = arrayList_get(exportRegistrationList, i);
        exportRegistration_close(export);
    }

    arrayList_destroy(exportRegistrationList);

    status = remoteServiceAdmin_unregisterReceive(admin, wireId);


    /* handle imported Wires */
    celixThreadMutex_lock(&admin->importedServicesLock);

    if (hashMap_size(admin->importedServices) == 0) {
        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_DEBUG, "RSA: No importedServices found for removal.");
    }
    else {
        hash_map_iterator_pt importedServicesIterator = hashMapIterator_create(admin->importedServices);

        while (hashMapIterator_hasNext(importedServicesIterator)) {
            hash_map_entry_pt entry = hashMapIterator_nextEntry(importedServicesIterator);

            if (entry != NULL) {

                // this one might already be destroyed by the endpoint_discovery_poller
                endpoint_description_pt endpointDescription = hashMapEntry_getKey(entry);

                char* regWireId = properties_get(endpointDescription->properties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

                if (regWireId == NULL) {
                    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: endpointDescription of %s misses a wire id\n", endpointDescription->service);
                } else if (strcmp(wireId, regWireId) == 0) {

                    /* TODO: registration handling missing - combine w/ removeImportedService */
                    import_registration_pt import = (import_registration_pt) hashMap_get(admin->importedServices, endpointDescription);

                    if (import == NULL) {
                        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: endpointDescription cannot be found for removal.");
                    } else {
                        importRegistration_close(import);

                        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: proxyService unregistered for %s w/ wireId %s.", endpointDescription->service, wireId);
                    }
                    hashMapIterator_remove(importedServicesIterator);

                }
            } else {
                logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: received entry was NULL.");
            }
        }

        hashMapIterator_destroy(importedServicesIterator);
    }

    celixThreadMutex_unlock(&admin->importedServicesLock);


    return status;
}

static celix_status_t remoteServiceAdmin_registerReceive(remote_service_admin_pt admin, char* wireId) {
    celix_status_t status = CELIX_SUCCESS;

    wiring_receive_service_pt wiringReceiveService = calloc(1, sizeof(*wiringReceiveService));

    if (!wiringReceiveService) {
        status = CELIX_ENOMEM;
    } else if (hashMap_containsKey(admin->wiringReceiveServices, wireId)) {
        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_DEBUG, "RSA: registerReceivewireId %s already known\n", wireId);
    } else {
        properties_pt props = properties_create();
        service_registration_pt wiringReceiveServiceRegistration = NULL;

        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: registerReceive w/ wireId %s\n", wireId);

        properties_set(props, (char*) INAETICS_WIRING_WIRE_ID, wireId);

        wiringReceiveService->handle = admin;
        wiringReceiveService->receive = remoteServiceAdmin_receive;
        wiringReceiveService->wireId = wireId;

        status = bundleContext_registerService(admin->context, (char *) INAETICS_WIRING_RECEIVE_SERVICE, wiringReceiveService, props, &wiringReceiveServiceRegistration);

        if (status == CELIX_SUCCESS) {
            hashMap_put(admin->wiringReceiveServices, wireId, wiringReceiveService);
            hashMap_put(admin->wiringReceiveServiceRegistrations, wireId, wiringReceiveServiceRegistration);
        } else {
            free(wiringReceiveService);
        }
    }

    return status;
}

static celix_status_t remoteServiceAdmin_unregisterReceive(remote_service_admin_pt admin, char* wireId) {
    celix_status_t status = CELIX_SUCCESS;

    service_registration_pt wiringReceiveServiceRegistration = hashMap_remove(admin->wiringReceiveServiceRegistrations, wireId);
    wiring_receive_service_pt wiringReceiveService = hashMap_remove(admin->wiringReceiveServices, wireId);

    if (wiringReceiveServiceRegistration != NULL) {
        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: unregisterReceive w/ wireId %s\n", wireId);
        status = serviceRegistration_unregister(wiringReceiveServiceRegistration);
    }

    if (wiringReceiveService != NULL) {
        free(wiringReceiveService);
    }

    return status;
}





celix_status_t remoteServiceAdmin_exportService(remote_service_admin_pt admin, char *serviceId, properties_pt properties, array_list_pt *registrations) {
    celix_status_t status = CELIX_SUCCESS;
    array_list_pt references = NULL;
    service_reference_pt reference = NULL;
    char filter[256];

    snprintf(filter, 256, "(%s=%s)", (char *) OSGI_FRAMEWORK_SERVICE_ID, serviceId);

    bundleContext_getServiceReferences(admin->context, NULL, filter, &references);

    if (arrayList_size(references) >= 1) {
        reference = arrayList_get(references, 0);
    }

    if (references != NULL) {
        arrayList_destroy(references);
    }

    if (reference == NULL) {
        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: expected a reference for service id %s.", serviceId);
        return CELIX_ILLEGAL_STATE;
    }

    char *exports = NULL;
    char *provided = NULL;
    if (status == CELIX_SUCCESS) {
        serviceReference_getProperty(reference, (char *) OSGI_RSA_SERVICE_EXPORTED_INTERFACES, &exports);
        serviceReference_getProperty(reference, (char *) OSGI_FRAMEWORK_OBJECTCLASS, &provided);

        if (exports == NULL || provided == NULL || strcmp(exports, provided) != 0) {
            logHelper_log(admin->loghelper, OSGI_LOGSERVICE_WARNING, "RSA: No Services to export.");
            status = CELIX_ILLEGAL_STATE;
        } else {
            logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: Export service (%s)", provided);
        }
    }

    if (status == CELIX_SUCCESS) {
        arrayList_create(registrations);
    }

    if (status == CELIX_SUCCESS) {
        char *interface = provided;
        endpoint_description_pt endpoint = NULL;
        export_registration_pt registration = NULL;

        remoteServiceAdmin_createEndpointDescription(admin, reference, interface, &endpoint);
        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_DEBUG, "RSA: Creating export registration with endpoint pointer %p\n", endpoint);

        //TOOD precheck if descriptor exists
        status = exportRegistration_create(admin->loghelper, reference, endpoint, admin->context, &registration);
        if (status == CELIX_SUCCESS) {
            status = exportRegistration_start(registration);
            if (status == CELIX_SUCCESS) {
                arrayList_add(*registrations, registration);
            }
        }
    }


    if (status == CELIX_SUCCESS) {
        celixThreadMutex_lock(&admin->exportedServicesLock);

        hashMap_put(admin->exportedServices, reference, *registrations);

        celixThreadMutex_unlock(&admin->exportedServicesLock);

        if (properties == NULL) {
            properties = properties_create();

            unsigned int size = 0;
            char **keys;

            serviceReference_getPropertyKeys(reference, &keys, &size);
            int i = 0;
            for (; i < size; i++) {
                char *key = keys[i];
                char *value = NULL;
                serviceReference_getProperty(reference, key, &value);

                properties_set(properties, key, value);
            }

            free(keys);
        }

        array_list_pt localWTMs = NULL;
        remoteServiceAdmin_getWTMs(admin, &localWTMs);

        int size = arrayList_size(localWTMs);
        if (size == 0) {
            logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: No WTM available yet.\n");
            status = CELIX_SERVICE_EXCEPTION;
        }
        int iter;
        for (iter = 0; iter < size; iter++) {
            wiring_topology_manager_service_pt wtmService = arrayList_get(localWTMs, iter);

            if (wtmService->exportWiringEndpoint(wtmService->manager, properties) != CELIX_SUCCESS) {
                logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: Wire export failed\n");
                status = CELIX_SERVICE_EXCEPTION;
            } else {
                logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: Wire sucessfully exported \n");
            }
        }

        arrayList_destroy(localWTMs);

    }

    /*
     * we do not support sync export, exports will inform the
     * wiring endpoint listener, which will in turn inform
     * discovery
     */

    return CELIX_SERVICE_EXCEPTION;
}

celix_status_t remoteServiceAdmin_removeExportedService(remote_service_admin_pt admin, export_registration_pt registration) {
    celix_status_t status;
    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: Removing exported service");

    export_reference_pt  ref = NULL;
    status = exportRegistration_getExportReference(registration, &ref);

    if (status == CELIX_SUCCESS) {
        celixThreadMutex_lock(&admin->exportedServicesLock);
        exportRegistration_close(registration);
        //exportRegistration_destroy(registration); TODO test
        hashMap_remove(admin->exportedServices, ref);
        celixThreadMutex_unlock(&admin->exportedServicesLock);
    } else {
        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_WARNING, "RSA: Cannot find reference for registration");
    }
    return status;
}


static celix_status_t remoteServiceAdmin_createEndpointDescription(remote_service_admin_pt admin, service_reference_pt reference, char *interface, endpoint_description_pt *endpoint) {
    celix_status_t status = CELIX_SUCCESS;
    properties_pt endpointProperties = properties_create();

    unsigned int size = 0;
    char **keys;
    int i;

    serviceReference_getPropertyKeys(reference, &keys, &size);
    for (i = 0; i < size; i++) {
        char *key = keys[i];
        char *value = NULL;

        if (serviceReference_getProperty(reference, key, &value) == CELIX_SUCCESS
            && strcmp(key, (char*) OSGI_RSA_SERVICE_EXPORTED_INTERFACES) != 0
            && strcmp(key, (char*) OSGI_FRAMEWORK_OBJECTCLASS) != 0) {
            properties_set(endpointProperties, key, value);
            printf("Added property '%s' with value '%s'\n", key, value);
        }
    }

    hash_map_entry_pt entry = hashMap_getEntry(endpointProperties, (void *) OSGI_FRAMEWORK_SERVICE_ID);

    char* key = hashMapEntry_getKey(entry);
    char *serviceId = (char *) hashMap_remove(endpointProperties, (void *) OSGI_FRAMEWORK_SERVICE_ID);
    char *uuid = NULL;

    uuid_t endpoint_uid;
    uuid_generate(endpoint_uid);
    char endpoint_uuid[37];
    uuid_unparse_lower(endpoint_uid, endpoint_uuid);

    bundleContext_getProperty(admin->context, OSGI_FRAMEWORK_FRAMEWORK_UUID, &uuid);
    properties_set(endpointProperties, (char*) OSGI_RSA_ENDPOINT_FRAMEWORK_UUID, uuid);
    properties_set(endpointProperties, (char*) OSGI_FRAMEWORK_OBJECTCLASS, interface);
    properties_set(endpointProperties, (char*) OSGI_RSA_ENDPOINT_SERVICE_ID, serviceId);
    properties_set(endpointProperties, (char*) OSGI_RSA_ENDPOINT_ID, endpoint_uuid);
    properties_set(endpointProperties, (char*) OSGI_RSA_SERVICE_IMPORTED, "true");
    properties_set(endpointProperties, (char*) OSGI_RSA_SERVICE_IMPORTED_CONFIGS, (char*) CONFIGURATION_TYPE);


    *endpoint = calloc(1, sizeof(**endpoint));
    if (!*endpoint) {
        status = CELIX_ENOMEM;
    } else {
        (*endpoint)->id = properties_get(endpointProperties, (char*) OSGI_RSA_ENDPOINT_ID);
        char *serviceId = NULL;
        serviceReference_getProperty(reference, (char*) OSGI_FRAMEWORK_SERVICE_ID, &serviceId);
        (*endpoint)->serviceId = strtoull(serviceId, NULL, 0);
        (*endpoint)->frameworkUUID = properties_get(endpointProperties, (char*) OSGI_RSA_ENDPOINT_FRAMEWORK_UUID);
        (*endpoint)->service = interface;
        (*endpoint)->properties = endpointProperties;
    }

    free(key);
    free(serviceId);
    free(keys);

    return status;
}


celix_status_t remoteServiceAdmin_destroyEndpointDescription(endpoint_description_pt *description) {
    celix_status_t status = CELIX_SUCCESS;

    properties_destroy((*description)->properties);
    free(*description);

    return status;
}

celix_status_t remoteServiceAdmin_getExportedServices(remote_service_admin_pt admin, array_list_pt *services) {
    celix_status_t status = CELIX_SUCCESS;
    return status;
}

celix_status_t remoteServiceAdmin_getImportedEndpoints(remote_service_admin_pt admin, array_list_pt *services) {
    celix_status_t status = CELIX_SUCCESS;
    return status;
}

celix_status_t remoteServiceAdmin_importService(remote_service_admin_pt admin, endpoint_description_pt endpointDescription, import_registration_pt *registration) {
    celix_status_t status = CELIX_SUCCESS;

    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: Import service %s", endpointDescription->service);

    celixThreadMutex_lock(&admin->importedServicesLock);

    /* get WireId */
    char* wireId = properties_get(endpointDescription->properties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

    if (wireId == NULL) {
        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: Missing WireId for service %s\n", endpointDescription->service);
        status = CELIX_SERVICE_EXCEPTION;
    } else {
        const char* objectClass = properties_get(endpointDescription->properties, "objectClass");

        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: Import service %s w/ wireId %s\n", endpointDescription->service, wireId);

        if (hashMap_containsKey(admin->importedServices, endpointDescription)) {
            logHelper_log(admin->loghelper, OSGI_LOGSERVICE_WARNING, "RSA: Imported services %s w/ wireId %s already available", endpointDescription->service, wireId);
            status = CELIX_BUNDLE_EXCEPTION;
        }
        else if (objectClass != NULL) {
              status = importRegistration_create(admin->context, endpointDescription, objectClass, registration);
        }

        if (status == CELIX_SUCCESS) {
            hashMap_put(admin->importedServices, endpointDescription, *registration);
        }

        // request wire
        array_list_pt localWTMs = NULL;
        remoteServiceAdmin_getWTMs(admin, &localWTMs);

        int size = arrayList_size(localWTMs);
        if (size == 0) {
            logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: No WTM available yet.\n");
        }
        else {
            int iter;
            for (iter = 0; iter < size; iter++) {
                wiring_topology_manager_service_pt wtmService = arrayList_get(localWTMs, iter);
                properties_pt rsaProperties = properties_create();

                properties_set(rsaProperties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY, wireId);
                properties_set(rsaProperties, "requested.service", endpointDescription->id);

                // TODO: move locking
                celixThreadMutex_unlock(&admin->importedServicesLock);
                wtmService->importWiringEndpoint(wtmService->manager, rsaProperties) ;
                celixThreadMutex_lock(&admin->importedServicesLock);
            }
        }

        arrayList_destroy(localWTMs);

    }

    celixThreadMutex_unlock(&admin->importedServicesLock);

    /* we should return SUCCESS here, otherwise the TPM will not add it to
     * its list of importedRegistrations and cannot remove it later on
     */
    return status;
}


celix_status_t remoteServiceAdmin_removeImportedService(remote_service_admin_pt admin, import_registration_pt registration) {
    celix_status_t status;
    endpoint_description_pt endpointDescription = NULL;
    import_registration_pt import = NULL;

    if (registration == NULL) {
    	status = CELIX_ILLEGAL_ARGUMENT;
    }
    else {
		status = importRegistration_getEndpointDescription(registration, &endpointDescription);

		celixThreadMutex_lock(&admin->importedServicesLock);

		import = (import_registration_pt) hashMap_remove(admin->importedServices, endpointDescription);

		if (import == NULL) {
			logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: endpointDescription cannot be found for removal.");
		}
		else {
			importRegistration_close(import);
			importRegistration_destroy(import);
			logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: proxyService unregistered for %s.", endpointDescription->service);
		}

		celixThreadMutex_unlock(&admin->importedServicesLock);
    }

    return status;
}

celix_status_t remoteServiceAdmin_send(remote_service_admin_pt admin, endpoint_description_pt endpointDescription, char *request, char **reply, int* replyStatus) {
    celix_status_t status = CELIX_ILLEGAL_ARGUMENT;
    char* wireId = properties_get(endpointDescription->properties, (char*) WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

    if (wireId == NULL) {
        logHelper_log(admin->loghelper, OSGI_LOGSERVICE_ERROR, "RSA: send called w/ proper endpoint_description: wireId missing.\n");
    } else {
        wiring_send_service_pt wiringSendService = NULL;

        status = celixThreadMutex_lock(&admin->sendServicesLock);

        if (status == CELIX_SUCCESS) {
            wiringSendService = hashMap_get(admin->sendServices, wireId);

            if (wiringSendService == NULL) {
                logHelper_log(admin->loghelper, OSGI_LOGSERVICE_WARNING, "RSA: No SendService w/ wireId %s found.\n", wireId);
                status = CELIX_ILLEGAL_ARGUMENT;
            } else {
                json_t *root;
                json_t *json_request;
                json_error_t jsonError;

                json_request = json_loads(request, 0, &jsonError);
                long serviceId = endpointDescription->serviceId;
                root = json_pack("{s:i, s:o}", "service.id", serviceId, "request", json_request);
                char *json_data = json_dumps(root, 0);

                status = wiringSendService->send(wiringSendService, json_data, reply, replyStatus);

                if (status != CELIX_SUCCESS || *reply == NULL) {
                    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_WARNING, "RSA: wireSendService->send of wireId %s return no success (%d; %d)\n", wireId, status, *replyStatus);
                    wiringSendService->errorCount++;
                }

                if (wiringSendService->errorCount >= 5) {
                    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_WARNING, "RSA: wireSendService fails five times now .. sleeping for some seconds\n");
                    sleep(5);
                    wiringSendService->errorCount = 0;
                }

                free(json_data);
                json_decref(root);
            }
            celixThreadMutex_unlock(&admin->sendServicesLock);
        }
    }

    return status;
}

static celix_status_t remoteServiceAdmin_createSendServiceTracker(remote_service_admin_pt admin) {
    celix_status_t status;
    service_tracker_customizer_pt customizer = NULL;

    status = serviceTrackerCustomizer_create(admin, remoteServiceAdmin_sendServiceAdding, remoteServiceAdmin_sendServiceAdded, remoteServiceAdmin_sendServiceModified,
            remoteServiceAdmin_sendServiceRemoved, &customizer);

    if (status == CELIX_SUCCESS) {
        char filter[512];

        snprintf(filter, 512, "(%s=%s)", (char*) OSGI_FRAMEWORK_OBJECTCLASS, (char*) INAETICS_WIRING_SEND_SERVICE);

        status = serviceTracker_createWithFilter(admin->context, filter, customizer, &admin->sendServicesTracker);

        if ((status == CELIX_SUCCESS) && (serviceTracker_open(admin->sendServicesTracker) == CELIX_SUCCESS)) {
            logHelper_log(admin->loghelper, OSGI_LOGSERVICE_DEBUG, "RSA: sendServiceTracker created w/ filter %s\n", filter);
        } else {
            logHelper_log(admin->loghelper, OSGI_LOGSERVICE_DEBUG, "RSA: sendServiceTracker could not be created w/ %s\n", filter);
        }
    }

    return status;
}

static celix_status_t remoteServiceAdmin_destroySendServiceTracker(remote_service_admin_pt admin) {
    celix_status_t status;

    status = serviceTracker_close(admin->sendServicesTracker);

    if (status == CELIX_SUCCESS) {
        status = serviceTracker_destroy(admin->sendServicesTracker);
    }

    return status;
}

static celix_status_t remoteServiceAdmin_sendServiceAdding(void * handle, service_reference_pt reference, void **service) {
    celix_status_t status;

    remote_service_admin_pt admin = (remote_service_admin_pt) handle;

    status = bundleContext_getService(admin->context, reference, service);

    return status;
}

static celix_status_t remoteServiceAdmin_sendServiceAdded(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status;
    remote_service_admin_pt admin = (remote_service_admin_pt) handle;
    wiring_send_service_pt wiringSendService = (wiring_send_service_pt) service;
    char* wireId;

    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_DEBUG, "RSA: Send Service Added\n");

    wireId = properties_get(wiringSendService->wiringEndpointDescription->properties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

    status = celixThreadMutex_lock(&admin->sendServicesLock);

    if (status == CELIX_SUCCESS) {
        hashMap_put(admin->sendServices, wireId, wiringSendService);
        status = celixThreadMutex_unlock(&admin->sendServicesLock);
    }

    return status;
}

static celix_status_t remoteServiceAdmin_sendServiceModified(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status = CELIX_SUCCESS;

    return status;
}

static celix_status_t remoteServiceAdmin_sendServiceRemoved(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status;
    remote_service_admin_pt admin = (remote_service_admin_pt) handle;
    wiring_send_service_pt wiringSendService = (wiring_send_service_pt) service;
    char* wireId = properties_get(wiringSendService->wiringEndpointDescription->properties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_DEBUG, "RSA: Send Service Removed for wireId %s\n", wireId);

    status = celixThreadMutex_lock(&admin->sendServicesLock);

    if (status == CELIX_SUCCESS) {
        hashMap_remove(admin->sendServices, wireId);
        status = celixThreadMutex_unlock(&admin->sendServicesLock);
    }


    return status;
}

celix_status_t remoteServiceAdmin_getWTMs(remote_service_admin_pt admin, array_list_pt *wtmList) {
    celix_status_t status;

    status = arrayList_create(wtmList);

    if (status == CELIX_SUCCESS) {
        status = celixThreadMutex_lock(&admin->wtmListLock);
    }

    if (status == CELIX_SUCCESS) {
        arrayList_addAll(*wtmList, admin->wtmList);
        status = celixThreadMutex_unlock(&admin->wtmListLock);
    }

    return status;
}

celix_status_t remoteServiceAdmin_notifyListenersEndpointAdded(remote_service_admin_pt admin, array_list_pt registrations) {
    celix_status_t status = CELIX_SUCCESS;

    hash_map_iterator_pt iter = hashMapIterator_create(admin->listenerList);
    while (hashMapIterator_hasNext(iter)) {
        char *scope = NULL;
        endpoint_listener_pt epl = NULL;
        service_reference_pt reference = hashMapIterator_nextKey(iter);

        serviceReference_getProperty(reference, (char *) OSGI_ENDPOINT_LISTENER_SCOPE, &scope);

        status = bundleContext_getService(admin->context, reference, (void **) &epl);
        if (status == CELIX_SUCCESS) {
            filter_pt filter = filter_create(scope);

            int regSize = arrayList_size(registrations);
            int regIt = 0;
            for (; regIt < regSize; regIt++) {
                export_registration_pt export = arrayList_get(registrations, regIt);

                endpoint_description_pt endpoint = NULL;
                export_reference_pt reference = NULL;

                status = exportRegistration_getExportReference(export, &reference);
                if (status == CELIX_SUCCESS) {
                    status = exportReference_getExportedEndpoint(reference, &endpoint);
                    free(reference);
                }

                if (status == CELIX_SUCCESS) {
                    bool matchResult = false;
                    filter_match(filter, endpoint->properties, &matchResult);
                    if (matchResult) {
                        status = epl->endpointAdded(epl->handle, endpoint, scope);
                    }
                }
            }

            filter_destroy(filter);
        }
    }

    hashMapIterator_destroy(iter);

    status = celixThreadMutex_unlock(&admin->listenerListLock);

    return status;
}

celix_status_t remoteServiceAdmin_endpointListenerAdding(void* handle, service_reference_pt reference, void** service) {
    celix_status_t status = CELIX_SUCCESS;
    remote_service_admin_pt admin = (remote_service_admin_pt) handle;

    bundleContext_getService(admin->context, reference, service);

    return status;
}

celix_status_t remoteServiceAdmin_endpointListenerAdded(void* handle, service_reference_pt reference, void* service) {
    celix_status_t status = CELIX_SUCCESS;
    remote_service_admin_pt admin = (remote_service_admin_pt) handle;
    char *scope = NULL;

    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: Added ENDPOINT_LISTENER");

    celixThreadMutex_lock(&admin->listenerListLock);
    hashMap_put(admin->listenerList, reference, NULL);
    celixThreadMutex_unlock(&admin->listenerListLock);

    serviceReference_getProperty(reference, (char *) OSGI_ENDPOINT_LISTENER_SCOPE, &scope);

    filter_pt filter = filter_create(scope);
    hash_map_iterator_pt refIter = hashMapIterator_create(admin->exportedServices);

    while (hashMapIterator_hasNext(refIter)) {

        array_list_pt registrations = hashMapIterator_nextValue(refIter);

        int arrayListSize = arrayList_size(registrations);
        int cnt = 0;

        for (; cnt < arrayListSize; cnt++) {
            export_registration_pt export = arrayList_get(registrations, cnt);

            endpoint_description_pt endpoint = NULL;
            export_reference_pt reference = NULL;

            status = exportRegistration_getExportReference(export, &reference);
            if (status == CELIX_SUCCESS) {
                status = exportReference_getExportedEndpoint(reference, &endpoint);
            }

            if (status == CELIX_SUCCESS) {
                bool matchResult = false;
                filter_match(filter, endpoint->properties, &matchResult);
                if (matchResult) {
                    endpoint_listener_pt listener = (endpoint_listener_pt) service;
                    status = listener->endpointAdded(listener->handle, endpoint, scope);
                }
            }
        }
    }
    hashMapIterator_destroy(refIter);

    filter_destroy(filter);

    return status;
}

celix_status_t remoteServiceAdmin_endpointListenerModified(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status;

    status = remoteServiceAdmin_endpointListenerRemoved(handle, reference, service);
    if (status == CELIX_SUCCESS) {
        status = remoteServiceAdmin_endpointListenerAdded(handle, reference, service);
    }

    return status;
}

celix_status_t remoteServiceAdmin_endpointListenerRemoved(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status;
    remote_service_admin_pt admin = (remote_service_admin_pt) handle;

    status = celixThreadMutex_lock(&admin->listenerListLock);

    if (status == CELIX_SUCCESS) {
        if (hashMap_remove(admin->listenerList, reference)) {
            logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: EndpointListener Removed");
        }

        status = celixThreadMutex_unlock(&admin->listenerListLock);
    }

    return status;
}

celix_status_t remoteServiceAdmin_wtmAdding(void * handle, service_reference_pt reference, void **service) {
    celix_status_t status;

    struct activator* activator = (struct activator*) handle;
    remote_service_admin_pt admin = (remote_service_admin_pt) activator->admin;

    status = bundleContext_getService(admin->context, reference, service);

    return status;
}

celix_status_t remoteServiceAdmin_wtmAdded(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status;
    struct activator* activator = (struct activator*) handle;
    remote_service_admin_pt admin = activator->admin;
    wiring_topology_manager_service_pt wtmService = (wiring_topology_manager_service_pt) service;

    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_INFO, "RSA: Added WTM");

    status = celixThreadMutex_lock(&admin->wtmListLock);


    if (status == CELIX_SUCCESS) {
    	arrayList_add(admin->wtmList, wtmService);
    	status = celixThreadMutex_unlock(&admin->wtmListLock);
    }

	// add already imported services to new wtm
    if (status == CELIX_SUCCESS) {
		status = celixThreadMutex_lock(&admin->importedServicesLock);
		hash_map_iterator_pt importedServicesIterator = hashMapIterator_create(admin->importedServices);
		while (hashMapIterator_hasNext(importedServicesIterator)) {
			endpoint_description_pt endpointDescription = (endpoint_description_pt) hashMapIterator_nextKey(importedServicesIterator);

			properties_pt rsaProperties = properties_create();

			/* get WireId */
			char* wireId = properties_get(endpointDescription->properties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY);

			if (wireId == NULL) {
			    logHelper_log(admin->loghelper, OSGI_LOGSERVICE_WARNING, "RSA: Missing WireId for service %s\n", endpointDescription->service);
			} else {
				properties_set(rsaProperties, WIRING_ENDPOINT_DESCRIPTION_WIRE_ID_KEY, wireId);
				properties_set(rsaProperties, "requested.service", endpointDescription->id);

				wtmService->importWiringEndpoint(wtmService->manager, rsaProperties);
			}
		}

		hashMapIterator_destroy(importedServicesIterator);

		status = celixThreadMutex_unlock(&admin->importedServicesLock);
    }

    // add already exported services to new wtm
    if (status == CELIX_SUCCESS) {
		status = celixThreadMutex_lock(&admin->exportedServicesLock);

		if (status == CELIX_SUCCESS) {
			hash_map_iterator_pt exportedServicesIterator = hashMapIterator_create(admin->exportedServices);

			while (hashMapIterator_hasNext(exportedServicesIterator)) {
				hash_map_entry_pt entry = hashMapIterator_nextEntry(exportedServicesIterator);
				service_reference_pt reference = hashMapEntry_getKey(entry);

				properties_pt properties = properties_create();

				unsigned int size = 0;
				char **keys;

				serviceReference_getPropertyKeys(reference, &keys, &size);
				int i = 0;
				for (; i < size; i++) {
					char *key = keys[i];
					char *value = NULL;
					serviceReference_getProperty(reference, key, &value);

					properties_set(properties, key, value);
				}

				free(keys);

				wtmService->exportWiringEndpoint(wtmService->manager, properties);
			}

			hashMapIterator_destroy(exportedServicesIterator);

			status = celixThreadMutex_unlock(&admin->exportedServicesLock);
		}
	}

    if (status == CELIX_SUCCESS) {
		// publish rsa service after wtm is available
		if (activator->registration == NULL) {
			status = bundleContext_registerService(admin->context, OSGI_RSA_REMOTE_SERVICE_ADMIN, activator->adminService, NULL, &activator->registration);
		}
    }

    return status;
}

celix_status_t remoteServiceAdmin_wtmModified(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status = CELIX_SUCCESS;

    // Nop...

    return status;
}

celix_status_t remoteServiceAdmin_wtmRemoved(void * handle, service_reference_pt reference, void * service) {
    celix_status_t status = CELIX_SUCCESS;

    // NYI

    return status;
}

