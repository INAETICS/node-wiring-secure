#
# Licensed under Apache License v2. See LICENSE for more information.
#

find_package(CURL REQUIRED)

include_directories(${CURL_INCLUDE_DIR})
include_directories("${CELIX_INCLUDE_DIRS}/remote_service_admin")
include_directories("${PROJECT_SOURCE_DIR}/remote_service_admin_inaetics/rsa/public/include")
include_directories("${PROJECT_SOURCE_DIR}/wiring_common_secure/public/include")
include_directories("${PROJECT_SOURCE_DIR}/wiring_common_secure/private/include")
include_directories("${PROJECT_SOURCE_DIR}/trust_manager/public/include")
include_directories("private/include")
#include_directories("${CELIX_INCLUDE_DIRS}/dependency_manager")
# supposed to be the rsa


SET(BUNDLE_SYMBOLICNAME "apache_celix_wiring_admin")
SET(BUNDLE_VERSION "0.0.1")
SET(BUNDLE_NAME "apache_celix_wiring_admin")

bundle(org.inaetics.wiring_admin_secure.WiringAdmin SOURCES
        private/src/wiring_admin_impl
        private/src/wiring_admin_activator
        ${PROJECT_SOURCE_DIR}/wiring_common_secure/private/src/civetweb.c
        ${PROJECT_SOURCE_DIR}/wiring_common_secure/private/src/wiring_common_utils.c
        ${PROJECT_SOURCE_DIR}/wiring_common_secure/private/src/wiring_endpoint_description.c
        ${PROJECT_SOURCE_DIR}/trust_manager/public/include/trust_manager_service.h
        )

install_bundle(org.inaetics.wiring_admin_secure.WiringAdmin)


get_filename_component(DM_DIR ${CELIX_FRAMEWORK_LIBRARY} DIRECTORY)
# with dependecy manage
#target_link_libraries(org.inaetics.wiring_admin_secure.WiringAdmin ${CELIX_FRAMEWORK_LIBRARY} ${CELIX_UTILS_LIBRARY} ${CURL_LIBRARIES} ${DM_DIR}/libdependency_manager.a)
target_link_libraries(org.inaetics.wiring_admin_secure.WiringAdmin ${CELIX_FRAMEWORK_LIBRARY} ${CELIX_UTILS_LIBRARY} ${CURL_LIBRARIES})
