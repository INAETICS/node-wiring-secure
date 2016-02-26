//
// Created by Martin Gaida on 2/26/16.
//

#ifndef THALESCWIRING_TRUST_MANAGER_STORAGE_H
#define THALESCWIRING_TRUST_MANAGER_STORAGE_H

#endif //THALESCWIRING_TRUST_MANAGER_STORAGE_H

int get_next_certificate_file_path(char* filepath);

int get_next_ca_certificate_file_path(char* filepath);

/**
 * Obtains the most recent certificate filepath.
 */
int get_most_recent_certificate(char* certificate_filepath);