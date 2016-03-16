//
// Created by Martin Gaida on 2/15/16.
//

#include <celix_errno.h>
#include <bundle_context.h>
#include <unistd.h>
#include "trust_manager_worker.h"
#include "trust_manager_impl.h"
#include "stdlib.h"
#include <mbedtls/pk.h>
#include <mbedtls/x509_crt.h>
#include "trust_manager_service.h"
#include "trust_manager_certhandler.h"
#include "trust_manager_keygen.h"
#include "trust_manager_storage.h"

#define TRUST_MANAGER_REFRESH_INTERVAL_PROPERTY_NAME	"trust.manager.refresh.interval"
#define DEFAULT_TRUST_MANAGER_REFRESH_INTERVAL			1

struct trust_worker {
    celix_thread_mutex_t workerLock;
    celix_thread_t workerThread;

    volatile bool running;

    char *ca_host;
    int ca_port;
    int refresh_interval;
};

static int trustWorker_getRefreshInterval(bundle_context_pt context);
static int trustWorker_getProperty(bundle_context_pt context, char* propertyName, int defaultValue);

/**
 *
 */
static int trustWorker_rekey(void* data) {
    int res;
    trust_worker_pt worker = (trust_worker_pt) data;
    mbedtls_pk_context *key = (mbedtls_pk_context *) malloc(sizeof(mbedtls_pk_context));
    generate_keypair(key);

    char *cert_path = malloc(1024);
    char *cert_path_full = malloc(1024);
    char *ca_cert_path = malloc(1024);
    char *pubkey_path = malloc(1024);
    char *privkey_path = malloc(1024);
    char *pubkey = malloc(4096);
    char *privkey = malloc(4096);
    char* cert = malloc(4096);

    get_next_public_key_file_path(pubkey_path);
    get_next_full_certificate_file_path(cert_path_full);
    res = get_public_key(key, pubkey);
    if (!res) {
        write_pem_to_file(pubkey, pubkey_path, false);
        write_pem_to_file(pubkey, cert_path_full, true);
    } else {
        goto fail;
    }

    get_next_private_key_file_path(privkey_path);
    res = get_private_key(key, privkey);
    if (!res) {
        write_pem_to_file(privkey, privkey_path, false);
        write_pem_to_file(privkey, cert_path_full, true);
    } else {
        goto fail;
    }

    // request cert from ca
    get_next_certificate_file_path(cert_path);
    res = csr_get_certificate(key, cert, worker->ca_host, worker->ca_port);
    if (!res) {
        write_pem_to_file(cert, cert_path, false);
        write_pem_to_file(cert, cert_path_full, true);
    } else {
        goto fail;
    }

    printf("\nrekey SUCCESS!");

    fail:
    free(cert);
    free(pubkey_path);
    free(privkey_path);
    free(ca_cert_path);
    free(cert_path);
    free(pubkey);
    free(privkey);
    return res;
}

/**
 * Recusrive trust reload. r specifies tries.
 */
static int refresh_ca_trust_r(void* data, mbedtls_x509_crt *ca_cert, int r)
{
    trust_worker_pt worker = (trust_worker_pt) data;
    
    int ret;
    char *ca_cert_filename = malloc(1024);
    ret = get_recent_ca_certificate(ca_cert_filename);
    ret += load_certificate(ca_cert, ca_cert_filename);

    // verify ca cert
    if (ret != 0) {
        char *ca_cert_path = malloc(1024);
        char* ca_cert_buf = malloc(4096);
        get_next_ca_certificate_file_path(ca_cert_path);

        if (!request_ca_certificate(ca_cert_buf, worker->ca_host, worker->ca_port)) {
            write_pem_to_file(ca_cert_buf, ca_cert_path, false);
        }
        free(ca_cert_buf);
        free(ca_cert_path);

        if (r >= 0) {
            refresh_ca_trust_r(data, ca_cert, --r);
        }
    }
    return ret;
}

/**
 * Reload the trust.
 */
static int refresh_ca_trust(void* data, mbedtls_x509_crt *ca_cert)
{
    return refresh_ca_trust_r(data, ca_cert, 2);
}


/*
 * Perform all validation and refresh of certificates / csr's.
 */
static void* trustWorker_run(void* data) {
    trust_worker_pt worker = (trust_worker_pt) data;

    int i = 0;
    while ((celixThreadMutex_lock(&worker->workerLock) == CELIX_SUCCESS) && worker->running) {
        int ret = 0;

        if (check_create_keyfolder() != 0) {
            printf("\nERROR: can't create key storage folder.\n");
            fflush(stdout);
            exit(1);
        }

        mbedtls_x509_crt *ca_cert = (mbedtls_x509_crt*) malloc(sizeof(mbedtls_x509_crt));
        mbedtls_x509_crt *cert = (mbedtls_x509_crt*) malloc(sizeof(mbedtls_x509_crt));

        if (refresh_ca_trust(data, ca_cert) != 0) {
            goto fail;
        }

        char *cert_filename = malloc(1024);
        ret = get_recent_certificate(cert_filename);
        ret += load_certificate(cert, cert_filename);
        // verify cert
        if (ret == 0) {
            if (verify_certificate(cert, ca_cert, 0) != 0) {
                trustWorker_rekey(data);
            } else {
                goto fail;
            }
        } else {
            trustWorker_rekey(data);
        }


        // clean old keys
        get_recent_public_key(cert);
        get_recent_private_key(cert);
        get_recent_full_certificate(cert);

        fail:
        free(cert_filename);
        free(ca_cert);
        free(cert);

        // unlock
        celixThreadMutex_unlock(&worker->workerLock);

        sleep(worker->refresh_interval);
    }

    if (worker->running == false) {
        celixThreadMutex_unlock(&worker->workerLock);
    }

    return NULL;
}

celix_status_t trustWorker_create(bundle_context_pt context, trust_worker_pt *worker, trust_manager_pt *tm) {
    celix_status_t status = CELIX_SUCCESS;

    if (worker == NULL) {
        return CELIX_BUNDLE_EXCEPTION;
    }

    (*worker) = calloc(1, sizeof(struct trust_worker));
    (*worker)->ca_host = (*tm)->ca_host;
    (*worker)->ca_port = (*tm)->ca_port;
    (*worker)->refresh_interval = (*tm)->refresh_interval;

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

static int trustWorker_getRefreshInterval(bundle_context_pt context) {
    return trustWorker_getProperty(context, TRUST_MANAGER_REFRESH_INTERVAL_PROPERTY_NAME, DEFAULT_TRUST_MANAGER_REFRESH_INTERVAL);
}

static int trustWorker_getProperty(bundle_context_pt context, char* propertyName, int defaultValue) {
    char *strValue = NULL;
    int value;

    bundleContext_getProperty(context, propertyName, &strValue);
    if (strValue != NULL) {
        char* endptr = strValue;

        errno = 0;
        value = strtol(strValue, &endptr, 10);
        if (*endptr || errno != 0) {
//			logHelper_log(bi->loghelper, OSGI_LOGSERVICE_WARNING, "incorrect format for %s", propertyName);
            value = defaultValue;
        }
    }
    else {
        value = defaultValue;
    }

    return value;
}
