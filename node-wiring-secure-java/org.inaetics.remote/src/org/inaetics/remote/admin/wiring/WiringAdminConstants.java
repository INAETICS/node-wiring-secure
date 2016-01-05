/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

/**
 * Compile time constants for the Remote Service Admin.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface WiringAdminConstants {

    /**
     * Configuration PID
     */
    String SERVICE_PID = "org.inaetics.remote.admin.wiring";

    /**
     * Configuration property: host
     */
    String HOST_CONFIG_KEY = "org.inaetics.remote.admin.wiring.host";

    /**
     * Configuration property: port
     */
    String PORT_CONFIG_KEY = "org.inaetics.remote.admin.wiring.port";

    /**
     * Configuration property: path
     */
    String PATH_CONFIG_KEY = "org.inaetics.remote.admin.wiring.path";

    /**
     * Configuration Type identifier
     */
    String CONFIGURATION_TYPE = "org.inaetics.remote.admin.wiring";

    /**
     * Configuration types supported by this implementation
     */
    String[] SUPPORTED_CONFIGURATION_TYPES = new String[] { CONFIGURATION_TYPE };

    /**
     * Generic pass-by-value intent
     */
    String PASSBYVALYE_INTENT = "passByValue";

    /**
     * Intents supported by this implementation
     */
    String[] SUPPORTED_INTENTS = new String[] { PASSBYVALYE_INTENT };
    
    /**
     * the id of the Inaetics wir
     */
    String WIRE_ID = CONFIGURATION_TYPE + ".wireId";
}
