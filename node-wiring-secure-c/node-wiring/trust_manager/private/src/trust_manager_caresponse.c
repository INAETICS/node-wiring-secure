#include "trust_manager_caresponse.h"
#include <jansson.h>
#include <stdbool.h>

/**
 * The key for the json value that determines if the certificate signing request was a success.
 */
const char CFSSL_SUCCESS_RESPONSE_KEY[] = "success";

const char CFSSL_RESULT_DATA_KEY[] = "result";

const char CFSSL_RESULT_DATA_CERTIFICATE_KEY[] = "certificate";

/**
 * Parse the json root element.
 */
json_t *parse_json(char *s)
{
    json_t *root;
    json_error_t error;

    root = json_loads(s, 0, &error);

    if (root) {
        return root;
    } else {
        fprintf(stderr, "json error on line %d: %s\n", error.line, error.text);
        return (json_t *)0;
    }
}

/**
 * Verify that the response was successful.
 */
bool verify_response (json_t *json)
{
    bool correct = true;
    correct = json_is_object(json) && correct;
    if (!correct)
        return false;

    json_t *success = json_object_get(json, CFSSL_SUCCESS_RESPONSE_KEY);
    correct = json_boolean_value(success) && correct;

    return correct;
}

/**
 * Parse the cert structure from the given root json.
 */
certresponse* parse_cert_from_json (json_t *json)
{
    json_t *data = json_object_get(json, CFSSL_RESULT_DATA_KEY);

    if (data) {
        json_t *cert = json_object_get(data, CFSSL_RESULT_DATA_CERTIFICATE_KEY);
        const char* cert_string = json_string_value(cert);

        //Create space for the new struct (outside stack space)
        certresponse* response = (certresponse*) malloc(sizeof(certresponse));
        strcpy((char *) response->certificate, cert_string);

        json_decref(data);
        json_decref(cert);

        return response;
    }

    // no success
    json_decref(data);
    return NULL;
}

/**
 * Verify and parse.
 */
certresponse* parse_certificate_response (char *body)
{
    json_t *response = parse_json(body);
    certresponse* cert = NULL;

    if (response) {
        bool correct = verify_response(response);
        if (!correct)
            return cert;

        cert = parse_cert_from_json(response);

        /* print and release the JSON structure */
        json_decref(response);
    }
    return cert;
}