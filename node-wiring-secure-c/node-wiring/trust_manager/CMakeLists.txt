# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

SET(BUNDLE_SYMBOLICNAME "inaetics_trust_manager")
SET(BUNDLE_VERSION "0.0.1")
#SET(BUNDLE_NAME "inaetics_trust_manager")

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

bundle(inaetics_trust_manager SOURCES
        private/src/activator
        private/src/trust_manager_caresponse
        private/src/trust_manager_certificaterequester
        private/src/trust_manager_csr_generator
        private/src/trust_manager_impl
        private/src/trust_manager_worker

        private/include/trust_manager_caresponse.h
        private/include/trust_manager_certificaterequester.h
        private/include/trust_manager_csr_generator.h
        private/include/trust_manager_impl.h
        private/include/trust_manager_worker.h
        )

include_directories("${PROJECT_SOURCE_DIR}/utils/public/include")
include_directories("public/include")
include_directories("private/src")
include_directories("private/include")
target_link_libraries(inaetics_trust_manager ${CELIX_FRAMEWORK_LIBRARY} ${CELIX_UTILS_LIBRARY} ${CURL_LIBRARIES}  ${JANSSON_LIBRARIES} ${MBEDTLS_LIBRARIES})
