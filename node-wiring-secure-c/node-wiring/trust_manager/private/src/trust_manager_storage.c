//
// Created by Martin Gaida on 2/26/16.
//
#include <stdio.h>
#include <dirent.h>
#include <regex.h>
#include <string.h>

#include "trust_manager_storage.h"

#define KEY_STORAGE_FOLDER "/tmp/cinkeys"
#define CA_CERT "/tmp/cinkeys/ca.%d.pem"
#define PRIVATE_KEY "/tmp/cinkeys/client_priv.%d.key"
#define PUBLIC_KEY "/tmp/cinkeys/client_pub.%d.key"

#define CERTIFICATE "/tmp/cinkeys/client.%d.pem"
#define CERTIFICATE_REGEX "^\\(client\\.\\)\\{1\\}\\([[:digit:]]\\)\\{10\\}\\(\\.pem\\)\\{1\\}$"
// ^(client\\.){1}([[:digit:]]){10}(\\.pem){1}$

int get_next_certificate_file_path(char* filepath) {
    int stamp = ((int)time(NULL));
    sprintf(filepath, CERTIFICATE, stamp);
    return 0;
}

int get_next_ca_certificate_file_path(char* filepath) {
    int stamp = ((int)time(NULL));
    sprintf(filepath, CA_CERT, stamp);
    return 0;
}

/**
 * Function to retrieve the most recent certificate.
 * Returns:
 * 0 - success
 * 1 - no cert found
 * 2 - folder not found
 * 3 - failure
 */
int get_most_recent_certificate(char* certificate_filepath) {
    DIR *d;
    struct dirent *dir;
    d = opendir(KEY_STORAGE_FOLDER);

//    char certlist[1024][1024];
    regex_t regex;
    int reti;
    char msgbuf[100];

    if (d)
    {
        // compile regex
        reti = regcomp(&regex, CERTIFICATE_REGEX, 0);
        if (reti) {
            fprintf(stderr, "Could not compile regex\n");
            exit(1);
        }

        unsigned char buffer_priv[1024];
        int i=0;
        while ((dir = readdir(d)) != NULL)
        {
            // match regex
            reti = regexec(&regex, dir->d_name, 0, NULL, 0);
            if (!reti) {
                sprintf(&buffer_priv, "%s/%s", KEY_STORAGE_FOLDER, dir->d_name);
                // return on first match...
                strcpy(certificate_filepath, &buffer_priv);
                return (0);
//                strcpy(certlist[i++], filepath);
            } else if (reti == REG_NOMATCH) {
                // nothing, no match
            } else {
                // regex broke
                return(3);
            }
        }
        closedir(d);

        // free mem
        regfree(&regex);
        return(1);
    } else {
        return(2);
    }
}