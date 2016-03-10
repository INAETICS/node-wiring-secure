/*
 * trust_manager_impl.c
 *
 *  \date       Feb 12, 2016
 *  \author    	<a href="mailto:dev@celix.apache.org">Inaetics Project Team</a>
 *  \copyright	Apache License, Version 2.0
 */
#include "trust_manager_impl.h"
#include "trust_manager_storage.h"

void trust_manager_getCertificate(trust_manager_pt instance){
    printf("Getting certificate from  %s\n", instance->name);
}

/**
 * Obtains the most recent certificate (absolute filepath)..
 */
extern int get_current_certificate(char* certificate_filepath)
{
    return get_recent_certificate(certificate_filepath);
}

/**
 * Obtains the most recent full certificate (absolute filepath)..
 */
extern int get_current_full_certificate(char* certificate_filepath)
{
    return get_recent_full_certificate(certificate_filepath);
}

/**
 * Obtains the most recent ca certificate (absolute filepath)..
 */
extern int get_current_ca_certificate(char* ca_cert_filepath)
{
    return get_recent_ca_certificate(ca_cert_filepath);
}

/**
 * Obtains the most recent private key (absolute filepath).
 */
extern int get_current_private_key(char* key_filepath)
{
    return get_recent_private_key(key_filepath);
}

/**
 * Obtains the most recent public key (absolute filepath)..
 */
extern int get_current_public_key(char* key_filepath)
{
    return get_recent_public_key(key_filepath);
}

/**
 * Obtains the most recent content of the public key.
 */
extern int get_current_public_key_content(char* content)
{
    return get_recent_public_key_content(content);
}

/**
 * Obtains the most recent content of the private key.
 */
extern int get_current_private_key_content(char* content)
{
    return get_recent_private_key_content(content);
}

/**
 * Obtains the most recent content of the ca cert.
 */
extern int get_current_ca_certificate_content(char* content)
{
    return get_recent_ca_certificate_content(content);
}

/**
 * Obtains the most recent content of the full cert (incl keys).
 */
extern int get_current_full_certificate_content(char* content)
{
    return get_recent_full_certificate_content(content);
}

/**
 * Obtains the most recent content of the cert.
 */
extern int get_current_certificate_content(char* content)
{
    return get_recent_certificate_content(content);
}

