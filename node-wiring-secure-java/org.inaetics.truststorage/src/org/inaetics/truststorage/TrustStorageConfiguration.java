/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.truststorage;

/**
 * Interface for accessing Trust Storage configuration values.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * @author <a href="mailto:hans.kr91@gmail.com">Hans Kruisselbrink, Thales Group NL</a>
 */
public interface TrustStorageConfiguration {

	/**
     * returns the keystore/truststore type (like/default "JKS").
     * 
     * @return the type of the keystore/truststore.
     */
    public String getKeyStoreType();
	
    /**
     * returns the keystore file name.
     * 
     * @return keystore file name.
     */
    public String getKeyStoreFileName();
    
    /**
     * returns the keystore password.
     * 
     * @return the keystore password.
     */
    public String getKeyStorePassword();
    
    /**
     * returns the keystore key password.
     * 
     * @return the keystore key password.
     */
    public String getKeyStoreKeyPassword();
    
	/**
     * returns the truststore file name.
     * 
     * @return truststore file name.
     */
    public String getTrustStoreFileName();
    
    /**
     * returns the truststore password.
     * 
     * @return the truststore password.
     */
    public String getTrustStorePassword();
    
   
}
