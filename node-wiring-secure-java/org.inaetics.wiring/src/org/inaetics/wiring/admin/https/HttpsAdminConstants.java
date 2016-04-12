/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

/**
 * Compile time constants for the Remote Service Admin.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * @author <a href="mailto:martin@gaida.eu">Martin Gaida, Thales Group NL</a>
 */
public interface HttpsAdminConstants {

    /**
     * Configuration PID
     */
    String SERVICE_PID = "org.inaetics.wiring.admin.https";

    /**
     * Configuration property: host
     */
    String HOST_CONFIG_KEY = SERVICE_PID + ".host";

    /**
     * Configuration property: port
     */
    String PORT_CONFIG_KEY = SERVICE_PID + ".port.secure";
    
    /**
     * Configuration property: truststore type
     */
    String CLIENT_CERT_ENFORCE_KEY = SERVICE_PID + ".clientvalidation";

    /**
     * Configuration property: path
     */
    String PATH_CONFIG_KEY = SERVICE_PID + ".path";

    /**
     * Configuration property: connect timeout
     */
    String CONNECT_TIMEOUT_CONFIG_KEY = SERVICE_PID + ".connecttimeout";

    /**
     * Configuration property: timeout
     */
    String READ_TIMEOUT_CONFIG_KEY = SERVICE_PID + ".readtimeout";

    /**
     * Configuration property: zone
     */
    String ZONE_CONFIG_KEY = SERVICE_PID + ".zone";

    /**
     * Configuration property: node
     */
    String NODE_CONFIG_KEY = SERVICE_PID + ".node";
    
    /**
     * Configuration property: keystore file name
     */
    String CONFIG_KEYSTORE_FILE_NAME = SERVICE_PID + ".keystore";
    
    /**
     * Configuration property: keystore password
     */
    String CONFIG_KEYSTORE_PASSWORD = SERVICE_PID + ".keystore.password";
    
    /**
     * Configuration property: keystore key password
     */
    String CONFIG_KEYSTORE_KEY_PASSWORD = SERVICE_PID + ".keystore.key.password";
	
    /**
     * Configuration property: truststore file name
     */
    String CONFIG_TRUSTSTORE_FILE_NAME = SERVICE_PID + ".truststore";
        
    /**
     * Configuration property: truststore password
     */
    String CONFIG_TRUSTSTORE_PASSWORD = SERVICE_PID + ".truststore.password";
    
    /**
     * Configuration property: truststore type
     */
    String CONFIG_TRUSTSTORE_TYPE = SERVICE_PID + ".truststore.type";

    /**
     * Configuration Type identifier
     */
    String CONFIGURATION_TYPE = "org.inaetics.wiring.admin.https";

    /**
     * Configuration Type url
     */
    String ENDPOINT_URL = CONFIGURATION_TYPE + ".url";

    /**
     * Configuration types supported by this implementation
     */
    String[] SUPPORTED_CONFIGURATION_TYPES = new String[] { CONFIGURATION_TYPE };

    /** Indicates that a service is actually a admin service, should have a value of "true". */
    String ADMIN = "admin";
    /** Indicates what kind of discovery service is provided. */
    String ADMIN_TYPE = "admin.type";
    
    String PROTOCOL_NAME = "inaetics.wiring.https";
    String PROTOCOL_VERSION = "0.1";
    String SECURE = "yes";
}
