//
// Created by Martin Gaida on 2/15/16.
//

#include <celix_errno.h>
#include <bundle_context.h>
#include <unistd.h>
#include "trust_manager_worker.h"
#include "stdlib.h"
#include "../include/trust_manager_certificaterequester.h"

#define DEFAULT_WORKER_SLEEP 3

struct trust_worker {
    celix_thread_mutex_t workerLock;
    celix_thread_t workerThread;

    volatile bool running;
};

/*
 * Perform all validation and refresh of certificates / csr's.
 */
static void* trustWorker_run(void* data) {
    trust_worker_pt worker = (trust_worker_pt) data;

    int i = 0;

    while ((celixThreadMutex_lock(&worker->workerLock) == CELIX_SUCCESS) && worker->running) {

        // init

        // unlock
        celixThreadMutex_unlock(&worker->workerLock);

        printf("%d: certificate worker executed.\n", i++);
        request_certificate();

        sleep(DEFAULT_WORKER_SLEEP);
    }

    if (worker->running == false) {
        celixThreadMutex_unlock(&worker->workerLock);
    }

    return NULL;
}

celix_status_t trustWorker_create(bundle_context_pt context, trust_worker_pt *worker) {
    celix_status_t status = CELIX_SUCCESS;

    if (worker == NULL) {
        return CELIX_BUNDLE_EXCEPTION;
    }

    (*worker) = calloc(1, sizeof(struct trust_worker));

    if (*worker) {
        if ((status = celixThreadMutex_create(&(*worker)->workerLock, NULL)) != CELIX_SUCCESS) {
            return status;
        }

        if ((status = celixThreadMutex_lock(&(*worker)->workerLock)) != CELIX_SUCCESS) {
            return status;
        }

        if ((status = celixThread_create(&(*worker)->workerThread, NULL, trustWorker_run, *worker)) != CELIX_SUCCESS) {
            return status;
        }

        (*worker)->running = true;

        if ((status = celixThreadMutex_unlock(&(*worker)->workerLock)) != CELIX_SUCCESS) {
            return status;
        }
    } else {
        status = CELIX_ENOMEM;
    }

    return status;
}

celix_status_t trustWorker_destroy(trust_worker_pt watcher) {
    celix_status_t status = CELIX_SUCCESS;

    celixThreadMutex_lock(&(watcher->workerLock));
    watcher->running = false;
    celixThreadMutex_unlock(&(watcher->workerLock));

    watcher->running = false;

    celixThread_join(watcher->workerThread, NULL);
    celixThreadMutex_destroy(&(watcher->workerLock));

    // remove own registration
    free(CELIX_SUCCESS);

    return status;
}
