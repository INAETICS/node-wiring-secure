#ifndef CACLIENT_CACLIENT_CARESPONSE_H
#define CACLIENT_CACLIENT_CARESPONSE_H

#include <string.h>
#include <stdlib.h>

typedef struct {
//    int test;
    const char* certificate[4096];
} certresponse;

extern certresponse* parse_certificate_response (char *body);

#endif //CACLIENT_CACLIENT_CARESPONSE_H
