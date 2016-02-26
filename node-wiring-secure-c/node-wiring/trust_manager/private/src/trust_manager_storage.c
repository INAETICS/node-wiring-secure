//
// Created by Martin Gaida on 2/26/16.
//
#include <stdio.h>
#include <dirent.h>
#include <regex.h>
#include <string.h>

#include "trust_manager_storage.h"

#define KEY_STORAGE_FOLDER "/tmp/cinkeys"

#define CA_CERT_REGEX "^\\(ca\\.\\)\\{1\\}\\([[:digit:]]\\)\\{10\\}\\(\\.pem\\)\\{1\\}$"
#define CA_CERT "/tmp/cinkeys/ca.%d.pem"

#define PRIVATE_KEY_REGEX "^\\(client_priv\\.\\)\\{1\\}\\([[:digit:]]\\)\\{10\\}\\(\\.key\\)\\{1\\}$"
#define PRIVATE_KEY "/tmp/cinkeys/client_priv.%d.key"

#define PUBLIC_KEY_REGEX "^\\(client_pub\\.\\)\\{1\\}\\([[:digit:]]\\)\\{10\\}\\(\\.key\\)\\{1\\}$"
#define PUBLIC_KEY "/tmp/cinkeys/client_pub.%d.key"

#define CERTIFICATE "/tmp/cinkeys/client.%d.pem"
#define CERTIFICATE_REGEX "^\\(client\\.\\)\\{1\\}\\([[:digit:]]\\)\\{10\\}\\(\\.pem\\)\\{1\\}$"
// in a "readable" way: ^(client\\.){1}([[:digit:]]){10}(\\.pem){1}$

/**
 * Obtains the next certificate file path.
 */
int get_next_certificate_file_path(char* filepath)
{
    int stamp = ((int)time(NULL));
    sprintf(filepath, CERTIFICATE, stamp);
    return 0;
}

/**
 * Obtains the next ca certificate file path.
 */
int get_next_ca_certificate_file_path(char* filepath)
{
    int stamp = ((int)time(NULL));
    sprintf(filepath, CA_CERT, stamp);
    return 0;
}

/**
 * Obtains the next private key file path.
 */
int get_next_private_key_file_path(char* filepath)
{
    int stamp = ((int)time(NULL));
    sprintf(filepath, PRIVATE_KEY, stamp);
    return 0;
}

/**
 * Obtains the next public key file path.
 */
int get_next_public_key_file_path(char* filepath)
{
    int stamp = ((int)time(NULL));
    sprintf(filepath, PUBLIC_KEY, stamp);
    return 0;
}

/**
 * Function to retrieve the most recent trust related file by regex.
 * Returns:
 * 0 - success
 * 1 - no cert found
 * 2 - folder not found
 * 3 - failure
 */
int get_recent_file_by_regex(char* filepath, char folder[], char reg_expression[]) {
    DIR *d;
    struct dirent *dir;
    d = opendir(folder);

//    char certlist[1024][1024];
    regex_t regex;
    int reti;

    if (d)
    {
        // compile regex
        reti = regcomp(&regex, reg_expression, 0);
        if (reti) {
            fprintf(stderr, "Could not compile regex\n");
            return(3);
        }

        unsigned char buffer_priv[1024];
//        int i=0;
        int skip=2;
        int found=0;
        while ((dir = readdir(d)) != NULL)
        {
            // match regex
            reti = regexec(&regex, dir->d_name, 0, NULL, 0);
            if (!reti) {
                sprintf(&buffer_priv, "%s/%s", folder, dir->d_name);
                if (found == 0) {
                    // set on first match...
                    strcpy(filepath, &buffer_priv);
                    found = 1;
                } else {
                    if (skip <= 0) {
                        remove(&buffer_priv);
                    } else {
                        skip--;
                    }
                }
//                strcpy(certlist[i++], filepath);
            } else if (reti == REG_NOMATCH) {
                // nothing, no match
            } else {
                // regex broke
                return(3);
            }
        }
        closedir(d);
        regfree(&regex);
        if (found)
            return(0);
        else
            return(1);
    } else {
        return(2);
    }
}

/**
 * Function to retrieve the most recent private key filepath.
 * Returns:
 * 0 - success
 * 1 - no cert found
 * 2 - folder not found
 * 3 - failure
 */
int get_recent_private_key(char* ca_cert_filepath)
{
    return get_recent_file_by_regex(ca_cert_filepath, KEY_STORAGE_FOLDER, PRIVATE_KEY_REGEX);
}

/**
 * Function to retrieve the most recent public key filepath.
 * Returns:
 * 0 - success
 * 1 - no cert found
 * 2 - folder not found
 * 3 - failure
 */
int get_recent_public_key(char* ca_cert_filepath)
{
    return get_recent_file_by_regex(ca_cert_filepath, KEY_STORAGE_FOLDER, PUBLIC_KEY_REGEX);
}

/**
 * Function to retrieve the most recent CA certificate filepath.
 * Returns:
 * 0 - success
 * 1 - no cert found
 * 2 - folder not found
 * 3 - failure
 */
int get_recent_ca_certificate(char* ca_cert_filepath)
{
    return get_recent_file_by_regex(ca_cert_filepath, KEY_STORAGE_FOLDER, CA_CERT_REGEX);
}

/**
 * Function to retrieve the most recent certificate filepath.
 * Returns:
 * 0 - success
 * 1 - no cert found
 * 2 - folder not found
 * 3 - failure
 */
int get_recent_certificate(char* certificate_filepath)
{
    return get_recent_file_by_regex(certificate_filepath, KEY_STORAGE_FOLDER, CERTIFICATE_REGEX);
}