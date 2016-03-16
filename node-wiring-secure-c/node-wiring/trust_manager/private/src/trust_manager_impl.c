/*
 * trust_manager_impl.c
 *
 *  \date       Feb 12, 2016
 *  \author    	<a href="mailto:dev@celix.apache.org">Inaetics Project Team</a>
 *  \copyright	Apache License, Version 2.0
 */
#include "trust_manager_impl.h"
#include "trust_manager_storage.h"

/**
 * Obtains the most recent certificate (absolute filepath)..
 */
int trust_manager_getCurrentCertificate(trust_manager_pt instance, char* certificate_filepath)
{
    return get_recent_certificate(certificate_filepath, instance->key_storage);
}

/**
 * Obtains the most recent full certificate (absolute filepath)..
 */
int trust_manager_getCurrentFullCertificate(trust_manager_pt instance, char* certificate_filepath)
{
    return get_recent_full_certificate(certificate_filepath, instance->key_storage);
}

/**
 * Obtains the most recent ca certificate (absolute filepath)..
 */
int trust_manager_getCurrentCaCertificate(trust_manager_pt instance, char* ca_cert_filepath)
{
    return get_recent_ca_certificate(ca_cert_filepath, instance->key_storage);
}

/**
 * Obtains the most recent private key (absolute filepath).
 */
int trust_manager_getCurrentPrivateKey(trust_manager_pt instance, char* key_filepath)
{
    return get_recent_private_key(key_filepath, instance->key_storage);
}

/**
 * Obtains the most recent public key (absolute filepath)..
 */
int trust_manager_getCurrentPublicKey(trust_manager_pt instance, char* key_filepath)
{
    return get_recent_public_key(key_filepath, instance->key_storage);
}

/**
 * Obtains the most recent content of the public key.
 */
int trust_manager_getCurrentPublicKeyContent(trust_manager_pt instance, char* content)
{
    return get_recent_public_key_content(content, instance->key_storage);
}

/**
 * Obtains the most recent content of the private key.
 */
int trust_manager_getCurrentPrivateKeyContent(trust_manager_pt instance, char* content)
{
    return get_recent_private_key_content(content, instance->key_storage);
}

/**
 * Obtains the most recent content of the ca cert.
 */
int trust_manager_getCurrentCaCertificateContent(trust_manager_pt instance, char* content)
{
    return get_recent_ca_certificate_content(content, instance->key_storage);
}

/**
 * Obtains the most recent content of the full cert (incl keys).
 */
int trust_manager_getCurrentFullCertificateContent(trust_manager_pt instance, char* content)
{
    return get_recent_full_certificate_content(content, instance->key_storage);
}

/**
 * Obtains the most recent content of the cert.
 */
int trust_manager_getCurrentCertificateContent(trust_manager_pt instance, char* content)
{
    return get_recent_certificate_content(content, instance->key_storage);
}

