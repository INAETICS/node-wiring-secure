//
// Created by Martin Gaida on 2/15/16.
//

#include <celix_errno.h>
#include <bundle_context.h>
#include "trust_manager_caresponse.h"

#ifndef THALESCWIRING_TRUST_MANAGER_WORKER_H
#define THALESCWIRING_TRUST_MANAGER_WORKER_H

#endif //THALESCWIRING_TRUST_MANAGER_WORKER_H

typedef struct trust_worker *trust_worker_pt;

celix_status_t trustWorker_create(bundle_context_pt context, trust_worker_pt *worker);
celix_status_t trustWorker_destroy(trust_worker_pt worker);