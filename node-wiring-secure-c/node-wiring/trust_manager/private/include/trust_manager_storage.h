//
// Created by Martin Gaida on 2/26/16.
//

#ifndef THALESCWIRING_TRUST_MANAGER_STORAGE_H
#define THALESCWIRING_TRUST_MANAGER_STORAGE_H

#endif //THALESCWIRING_TRUST_MANAGER_STORAGE_H

/**
 * Obtains the next certificate file path.
 */
int get_next_certificate_file_path(char* filepath);

/**
 * Obtains the next ca certificate file path.
 */
int get_next_ca_certificate_file_path(char* filepath);

/**
 * Obtains the next private key file path.
 */
int get_next_private_key_file_path(char* filepath);

/**
 * Obtains the next public key file path.
 */
int get_next_public_key_file_path(char* filepath);

/**
 * Obtains the most recent certificate filepath.
 */
int get_recent_certificate(char* certificate_filepath);

/**
 * Obtains the most recent ca certificate filepath.
 */
int get_recent_ca_certificate(char* ca_cert_filepath);

/**
 * Obtains the most recent private key filepath.
 */
int get_recent_private_key(char* ca_cert_filepath);

/**
 * Obtains the most recent public key filepath.
 */
int get_recent_public_key(char* ca_cert_filepath);