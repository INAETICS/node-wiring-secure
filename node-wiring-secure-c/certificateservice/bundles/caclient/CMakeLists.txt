set(BUNDLE_SYMBOLICNAME "com.example.caclient")
set(BUNDLE_VERSION "0.0.1")
set(BUNDLE_NAME "caclient")

find_package(mbedTLS REQUIRED)

if(!MBEDTLS_FOUND)
    MESSAGE(SEND_ERROR "Could not find MBEDTLS on your system")

endif()

FIND_PACKAGE(CURL REQUIRED)
find_path(JANSSON_INCLUDE_DIR jansson.h
        /usr/include
        /usr/local/include )
find_library(JANSSON_LIBRARY NAMES jansson
        PATHS /usr/lib /usr/local/lib )
set(JANSSON_LIBRARIES ${JANSSON_LIBRARY} )
set(JANSSON_INCLUDE_DIR ${JANSSON_INCLUDE_DIR})
MESSAGE(STATUS "JANSSON libraries found at: ${JANSSON_LIBRARY}")
MESSAGE(STATUS "JANSSON includes found at: ${JANSSON_INCLUDE_DIR}")

INCLUDE_DIRECTORIES(${mbedTLS_INCLUDE_DIR})
INCLUDE_DIRECTORIES(${CURL_INCLUDE_DIR})
INCLUDE_DIRECTORIES(${JANSSON_INCLUDE_DIR})

bundle(caclient SOURCES private/src/caclient_activator.c private/src/caclient_caresponse.c private/src/caclient_csr_generator.c private/src/caclient_certificaterequester.c)
install_bundle(caclient)
target_link_libraries(caclient  ${CELIX_FRAMEWORK_LIBRARY} ${CELIX_UTILS_LIBRARY} ${CURL_LIBRARIES} ${JANSSON_LIBRARIES} ${MBEDTLS_LIBRARIES})

