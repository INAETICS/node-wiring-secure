/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.discovery;


/**
 * Interface for accessing discovery configuration values.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface DiscoveryConfiguration {

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

}
