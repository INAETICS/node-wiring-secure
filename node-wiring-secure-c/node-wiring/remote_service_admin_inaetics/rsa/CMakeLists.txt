#
# Licensed under Apache License v2. See LICENSE for more information.
#

find_package(CURL REQUIRED)
find_package(Jansson REQUIRED)
find_package(FFI REQUIRED)

include_directories(
	${CURL_INCLUDE_DIRS}
	${JANSSON_INCLUDE_DIRS}
	${FFI_INCLUDE_DIRS}
	${CELIX_INCLUDE_DIR}/utils/public/include
	${CELIX_INCLUDE_DIR}/remote_services/utils/private/include
	${CELIX_INCLUDE_DIR}/remote_services/utils/public/include
	${CELIX_INCLUDE_DIR}/endpoint_listener
	${CELIX_INCLUDE_DIR}/remote_services/endpoint_listener/public/include
	${CELIX_INCLUDE_DIR}/log_service
	${CELIX_INCLUDE_DIR}/remote_service_admin
	${CELIX_INCLUDE_DIR}/dfi
	${PROJECT_SOURCE_DIR}/remote_service_admin/private/include
	${PROJECT_SOURCE_DIR}/wiring_common/private/include
	${PROJECT_SOURCE_DIR}/wiring_common/public/include
	private/include
	public/include
	../dynamic_function_interface
)

if (${CMAKE_SYSTEM_NAME} STREQUAL "Darwin")
	include_directories(dynamic_function_interface/memstream)
endif()



SET(BUNDLE_SYMBOLICNAME "apache_celix_wiring_remote_service_admin")
SET(BUNDLE_VERSION "0.0.1")
SET(BUNDLE_NAME "apache_celix_wiring_remote_service_admin")

bundle(org.inaetics.remote_service_admin SOURCES 
	private/src/remote_service_admin_impl
	private/src/remote_service_admin_activator
	${PROJECT_SOURCE_DIR}/remote_service_admin/private/src/endpoint_description 
    ${PROJECT_SOURCE_DIR}/remote_service_admin/private/src/export_registration_dfi
    ${PROJECT_SOURCE_DIR}/remote_service_admin/private/src/import_registration_dfi
	${CELIX_DIR}/share/celix/log_service/log_helper
	${PROJECT_SOURCE_DIR}/wiring_common/private/src/civetweb.c
)

install_bundle(org.inaetics.remote_service_admin)
    
target_link_libraries(org.inaetics.remote_service_admin ${CELIX_FRAMEWORK_LIBRARY} ${CELIX_UTILS_LIBRARY} ${FFI_LIBRARIES} ${CURL_LIBRARIES}  ${JANSSON_LIBRARIES} ${CELIX_DIR}/lib/libcelix_dfi.so )
