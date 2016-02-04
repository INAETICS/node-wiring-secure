#
# Licensed under Apache License v2. See LICENSE for more information.
#

cmake_minimum_required(VERSION 2.8)
project(caclient C)

#Note. If celix is not installed in /usr/local dir, change the location accordingly.
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake/")
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${HOME}/Documents/workspace/celix/celix-install/share/celix/cmake/modules")

find_package(CELIX REQUIRED)
include_directories(${CELIX_INCLUDE_DIRS})

add_subdirectory(bundles/caclient)

deploy_targets()
