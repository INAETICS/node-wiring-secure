#
# Licensed under Apache License v2. See LICENSE for more information.
#

find_package(CURL REQUIRED)
find_package(Jansson REQUIRED)

include_directories("${CURL_INCLUDE_DIR}")
include_directories("${JANSSON_INCLUDE_DIR}")
include_directories("${CELIX_INCLUDE_DIRS}/remote_service_admin")
include_directories("${PROJECT_SOURCE_DIR}/wiring_common/public/include")
include_directories("${PROJECT_SOURCE_DIR}/wiring_common/private/include")
include_directories("private/include")
include_directories("public/include")


SET(BUNDLE_SYMBOLICNAME "apache_celix_wiring_node_discovery_etcd")
SET(BUNDLE_VERSION "0.0.1")
SET(BUNDLE_NAME "apache_celix_wiring_node_discovery_etcd")


bundle(org.inaetics.node_discovery.etcd.NodeDiscovery SOURCES
	private/src/etcd.c
	private/src/etcd_watcher.c
	private/src/node_discovery.c
	private/src/node_discovery_activator.c
	private/src/node_description.c
	private/src/wiring_endpoint_reader.c
	private/src/wiring_endpoint_writer.c
   ${PROJECT_SOURCE_DIR}/wiring_common/private/src/wiring_endpoint_description.c
)

install_bundle(org.inaetics.node_discovery.etcd.NodeDiscovery)
	
target_link_libraries(org.inaetics.node_discovery.etcd.NodeDiscovery  ${CELIX_FRAMEWORK_LIBRARY} ${CELIX_UTILS_LIBRARY} ${CURL_LIBRARIES} ${JANSSON_LIBRARIES})
