/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

import java.net.URL;

/**
 * Interface for accessing HTTPS Admin configuration values.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface HttpsAdminConfiguration {

    /**
     * returns the base url for the HTTP admin
     * 
     * @return the base url
     */
    public URL getBaseUrl();
    
    /**
     * returns the connect timeout for the client endpoint
     * 
     * @return connect timeout in ms
     */
    public int getConnectTimeout();

    /**
     * returns the read timeout for the client endpoint
     * 
     * @return read timeout in ms
     */
    public int getReadTimeout();
    
    /**
     * returns the zone id
     * 
     * @return the zone id
     */
    public String getZone();
    
    /**
     * returns the node id
     * 
     * @return the node id
     */
    public String getNode();    
    
    /**
     * returns the truststore file name.
     * 
     * @return truststore file name.
     */
    public String getTruststoreFileName();
    
    /**
     * returns the truststore password.
     * 
     * @return the truststore password.
     */
    public String getTruststorePassword();
    
    /**
     * returns the truststore type (like/default "JKS").
     * 
     * @return the type of the truststore.
     */
    public String getTruststoreType();
}
