#
# Licensed under Apache License v2. See LICENSE for more information.
#

cmake_minimum_required(VERSION 2.8)
project(inaetics-wiring C)
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake/")
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CELIX_DIR}/share/celix/cmake/modules")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wno-long-long")


find_package(CELIX REQUIRED)
include_directories(${CELIX_INCLUDE_DIRS})

add_subdirectory(wiring_topology_manager)
add_subdirectory(node_discovery)
add_subdirectory(wiring_admin)
add_subdirectory(remote_service_admin_inaetics)



deploy_targets()
