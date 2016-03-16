//
// Created by Martin Gaida on 2/26/16.
//
#include <stdio.h>
#include <dirent.h>
#include <regex.h>
#include <string.h>
#include "stdlib.h"
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "trust_manager_storage.h"

#define KEY_STORAGE_FOLDER "/tmp/cinkeys"

#define CA_CERT KEY_STORAGE_FOLDER "/ca.%d.pem"
#define CA_CERT_REGEX "^\\(ca\\.\\)\\{1\\}\\([[:digit:]]\\)\\{10\\}\\(\\.pem\\)\\{1\\}$"

#define PRIVATE_KEY KEY_STORAGE_FOLDER "/client_priv.%d.key"
#define PRIVATE_KEY_REGEX "^\\(client_priv\\.\\)\\{1\\}\\([[:digit:]]\\)\\{10\\}\\(\\.key\\)\\{1\\}$"

#define PUBLIC_KEY KEY_STORAGE_FOLDER "/client_pub.%d.key"
#define PUBLIC_KEY_REGEX "^\\(client_pub\\.\\)\\{1\\}\\([[:digit:]]\\)\\{10\\}\\(\\.key\\)\\{1\\}$"

#define CERTIFICATE KEY_STORAGE_FOLDER "/client.%d.pem"
#define CERTIFICATE_REGEX "^\\(client\\.\\)\\{1\\}\\([[:digit:]]\\)\\{10\\}\\(\\.pem\\)\\{1\\}$"

#define CERTIFICATE_FULL KEY_STORAGE_FOLDER "/client_full.%d.pem"
#define CERTIFICATE_FULL_REGEX "^\\(client_full\\.\\)\\{1\\}\\([[:digit:]]\\)\\{10\\}\\(\\.pem\\)\\{1\\}$"
// in a "readable" way: ^(client\\.){1}([[:digit:]]){10}(\\.pem){1}$

/**
 * Checks if key folder exists and creates it if not.
 */
int check_create_keyfolder()
{
    struct stat st = {0};

    if (stat(KEY_STORAGE_FOLDER, &st) == -1) {
        return mkdir(KEY_STORAGE_FOLDER, 0700);
    }
    return 0;
}

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
 * Obtains the next full certificate (incl keys) file path.
 */
int get_next_full_certificate_file_path(char* filepath)
{
    int stamp = ((int)time(NULL));
    sprintf(filepath, CERTIFICATE_FULL, stamp);
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

/**
 * Function to retrieve the most recent full certificate (incl pub / priv key) filepath.
 * Returns:
 * 0 - success
 * 1 - no cert found
 * 2 - folder not found
 * 3 - failure
 */
int get_recent_full_certificate(char* certificate_filepath)
{
    return get_recent_file_by_regex(certificate_filepath, KEY_STORAGE_FOLDER, CERTIFICATE_FULL_REGEX);
}

/**
 * Reads the contents of a file.
 */
int read_file_contents(char* content, char* filepath)
{
    FILE *fp;
    long lSize;
    char *buffer;

    fp = fopen ( filepath , "rb" );
    if( !fp ) perror(filepath),exit(1);

    fseek( fp , 0L , SEEK_END);
    lSize = ftell( fp );
    rewind( fp );

    /* allocate memory for entire content */
    buffer = calloc( 1, lSize+1 );
    if( !buffer ) fclose(fp),fputs("memory alloc fails",stderr),exit(1);

    /* copy the file into the buffer */
    if( 1!=fread( buffer , lSize, 1 , fp) )
        fclose(fp),free(buffer),fputs("entire read fails",stderr),exit(1);

    strcpy(content, buffer);

    fclose(fp);
    free(buffer);

    return 0;
}

/**
 * Reads the content of the full cert (with pub & priv key).
 */
int get_recent_full_certificate_content(char* content)
{
    int ret;
    char* filepath = malloc(256);
    get_recent_full_certificate(filepath);
    ret = read_file_contents(content, filepath);
    free(filepath);
    return ret;
}

/**
 * Reads the content of the cert.
 */
int get_recent_certificate_content(char* content)
{
    int ret;
    char* filepath = malloc(256);
    get_recent_certificate(filepath);
    ret = read_file_contents(content, filepath);
    free(filepath);
    return ret;
}

/**
 * Reads the content of the ca cert.
 */
int get_recent_ca_certificate_content(char* content)
{
    int ret;
    char* filepath = malloc(256);
    get_recent_ca_certificate(filepath);
    ret = read_file_contents(content, filepath);
    free(filepath);
    return ret;
}

/**
 * Reads the content of the public key.
 */
int get_recent_public_key_content(char* content)
{
    int ret;
    char* filepath = malloc(256);
    get_recent_public_key(filepath);
    ret = read_file_contents(content, filepath);
    free(filepath);
    return ret;
}

/**
 * Reads the content of the private key.
 */
int get_recent_private_key_content(char* content)
{
    int ret;
    char* filepath = malloc(256);
    get_recent_private_key(filepath);
    ret = read_file_contents(content, filepath);
    free(filepath);
    return ret;
}

