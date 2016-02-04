#include <stdlib.h>
#include <stdio.h>

#include "bundle_activator.h"
#include "../include/caclient_certificaterequester.h"


celix_status_t bundleActivator_create(bundle_context_pt context, void **userData) {
    return CELIX_SUCCESS;
}

celix_status_t bundleActivator_start(void * userData, bundle_context_pt context) {
    request_certificate();
    return CELIX_SUCCESS;
}

celix_status_t bundleActivator_stop(void * userData, bundle_context_pt context) {
    return CELIX_SUCCESS;
}

celix_status_t bundleActivator_destroy(void * userData, bundle_context_pt context) {
    return CELIX_SUCCESS;
}
