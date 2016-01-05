/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.discovery.etcd;

import org.inaetics.wiring.discovery.DiscoveryConfiguration;

/**
 * Interface for accessing etcd discovery configuration values.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface EtcdDiscoveryConfiguration extends DiscoveryConfiguration {

    /**
     * returns the connect url for the etcd discovery
     * 
     * @return the connect url
     */
    public String getConnectUrl();

    /**
     * returns the root path for the etcd discovery
     * 
     * @return the root path
     */
    public String getRootPath();
}
