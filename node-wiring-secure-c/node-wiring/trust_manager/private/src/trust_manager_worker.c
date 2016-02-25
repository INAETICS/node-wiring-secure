//
// Created by Martin Gaida on 2/15/16.
//

#include <celix_errno.h>
#include <bundle_context.h>
#include <unistd.h>
#include "trust_manager_worker.h"
#include "stdlib.h"
#include <mbedtls/pk.h>
#include <mbedtls/x509_crt.h>
#include "../include/trust_manager_certificaterequester.h"
#include "../include/trust_manager_keygen.h"

#define CA_CERT "/tmp/cinkeys/ca.pem"
#define PRIVATE_KEY "/tmp/cinkeys/client_private.key"
#define PUBLIC_KEY "/tmp/cinkeys/client_public.key"
#define CERTIFICATE "/tmp/cinkeys/client.pem"

#define DEFAULT_WORKER_SLEEP 1

struct trust_worker {
    celix_thread_mutex_t workerLock;
    celix_thread_t workerThread;

    volatile bool running;
};

static int trustWorker_rekey(void) {
    // allocate mem
    mbedtls_pk_context *key = (mbedtls_pk_context *) malloc(sizeof(mbedtls_pk_context));

    // gen keypair
    generate_keypair(key);

    int res;

    // print keys
    char *pubkey = malloc(4096);
    res = get_public_key(key, pubkey);
    if (!res) {
        write_pem_to_file(pubkey, PUBLIC_KEY);
    } else {
        return res;
    }
    free(pubkey);

    char *privkey = malloc(4096);
    res = get_private_key(key, privkey);
    if (!res) {
        write_pem_to_file(pubkey, PRIVATE_KEY);
    } else {
        return res;
    }
    free(privkey);

    // request cert from ca
    char* cert = malloc(4096);
    res = request_certificate(key, cert);
    if (!res) {
        write_pem_to_file(cert, CERTIFICATE);
    } else {
        return res;
    }
    free(cert);
    return res;
}

/*
 * Perform all validation and refresh of certificates / csr's.
 */
static void* trustWorker_run(void* data) {
    trust_worker_pt worker = (trust_worker_pt) data;

    int i = 0;
    while ((celixThreadMutex_lock(&worker->workerLock) == CELIX_SUCCESS) && worker->running) {
        int ret = 0;
        mbedtls_x509_crt *ca_cert = (mbedtls_x509_crt*) malloc(sizeof(mbedtls_x509_crt));
        mbedtls_x509_crt *cert = (mbedtls_x509_crt*) malloc(sizeof(mbedtls_x509_crt));
        ret += load_certificate(ca_cert, CA_CERT);

        // verify ca cert
        if (ret != 0) {
            // TODO: implement ca cert getter
            goto fail;
        }

        ret += load_certificate(cert, CERTIFICATE);

        // verify cert
        if (!ret) {
            if (verify_certificate(cert, ca_cert, 0) != 0) {
                trustWorker_rekey();
            } else {
                goto fail;
            }
        } else {
            trustWorker_rekey();
        }

        fail:
        free(ca_cert);
        free(cert);

        // unlock
        celixThreadMutex_unlock(&worker->workerLock);

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
