/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.truststorage;

/**
 * Compile time constants for the Remote Service Admin.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * @author <a href="mailto:hans.kr91@gmail.com">Hans Kruisselbrink, Thales Group NL</a>
 */
public interface TrustStorageConstants {
	
	/**
     * Configuration PID
     */
    String SERVICE_PID = "org.inaetics.truststorage";
	
    /**
     * Configuration property: truststore type
     */
    String CONFIG_KEYSTORE_TYPE = SERVICE_PID + ".keystore.type";
    
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
}
