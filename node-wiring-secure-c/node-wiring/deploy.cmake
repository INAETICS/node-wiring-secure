
# add external dependencies

add_library(shell SHARED IMPORTED)
set_property(TARGET shell PROPERTY LINKER_LANGUAGE C)
set_property(TARGET shell PROPERTY BUNDLE "${CELIX_BUNDLES_DIR}/shell.zip")
set_property(TARGET shell PROPERTY IMPORTED_LOCATION "${CELIX_BUNDLES_DIR}/shell.zip")

add_library(shell_tui SHARED IMPORTED)
set_property(TARGET shell_tui PROPERTY LINKER_LANGUAGE C)
set_property(TARGET shell_tui PROPERTY BUNDLE "${CELIX_BUNDLES_DIR}/shell_tui.zip")
set_property(TARGET shell_tui PROPERTY IMPORTED_LOCATION "${CELIX_BUNDLES_DIR}/shell_tui.zip")

add_library(topology_manager SHARED IMPORTED)
set_property(TARGET topology_manager PROPERTY LINKER_LANGUAGE C)
set_property(TARGET topology_manager PROPERTY BUNDLE "${CELIX_BUNDLES_DIR}/topology_manager.zip")
set_property(TARGET topology_manager PROPERTY IMPORTED_LOCATION "${CELIX_BUNDLES_DIR}/topology_manager.zip")

add_library(discovery_etcd SHARED IMPORTED)
set_property(TARGET discovery_etcd PROPERTY LINKER_LANGUAGE C)
set_property(TARGET discovery_etcd PROPERTY BUNDLE "${CELIX_BUNDLES_DIR}/discovery_etcd.zip")
set_property(TARGET discovery_etcd PROPERTY IMPORTED_LOCATION "${CELIX_BUNDLES_DIR}/discovery_etcd.zip")

#add_library(calculator_shell SHARED IMPORTED)
#set_property(TARGET calculator_shell PROPERTY LINKER_LANGUAGE C)
#set_property(TARGET calculator_shell PROPERTY BUNDLE "${CELIX_BUNDLES_DIR}/calculator_shell.zip")
#set_property(TARGET calculator_shell PROPERTY IMPORTED_LOCATION "${CELIX_BUNDLES_DIR}/calculator_shell.zip")

#add_library(calculator SHARED IMPORTED)
#set_property(TARGET calculator PROPERTY LINKER_LANGUAGE C)
#set_property(TARGET calculator PROPERTY BUNDLE "${CELIX_BUNDLES_DIR}/calculator.zip")
#set_property(TARGET calculator PROPERTY IMPORTED_LOCATION "${CELIX_BUNDLES_DIR}/calculator.zip")

deploy("wiring_rsa_client" BUNDLES
   shell
   shell_tui
   topology_manager
   discovery_etcd
#   calculator_shell
   org.inaetics.node_discovery.etcd.NodeDiscovery
   org.inaetics.wiring_topology_manager.WiringTopologyManager
   org.inaetics.wiring_admin_secure.WiringAdmin
   org.inaetics.remote_service_admin
)

deploy("wiring_rsa_server" BUNDLES
   shell
   shell_tui
   topology_manager
   discovery_etcd
#   calculator
   org.inaetics.node_discovery.etcd.NodeDiscovery
   org.inaetics.wiring_topology_manager.WiringTopologyManager
   org.inaetics.wiring_admin_secure.WiringAdmin
   org.inaetics.remote_service_admin
)

