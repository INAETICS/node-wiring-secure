#ifndef CACLIENT_CACLIENT_CERTIFICATEREQUESTER_H
#define CACLIENT_CACLIENT_CERTIFICATEREQUESTER_H

#include <stdlib.h>

/**
 * The string for return body.
 */
struct string {
    char *ptr;
    size_t len;
};

void init_string(struct string *s);
size_t writefunc(void *ptr, size_t size, size_t nmemb, struct string *s);
int request_certificate(void);


#endif //CACLIENT_CACLIENT_CERTIFICATEREQUESTER_H