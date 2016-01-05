/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.discovery;

/**
 * Common constants used by all implementations of the discovery service.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface DiscoveryConstants {
    /** Indicates that a service is actually a discovery service, should have a value of "true". */
    String DISCOVERY = "discovery";
    /** Indicates what kind of discovery service is provided. */
    String DISCOVERY_TYPE = "discovery.type";

}
