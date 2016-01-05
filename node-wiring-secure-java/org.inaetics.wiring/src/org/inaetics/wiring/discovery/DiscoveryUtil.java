/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.discovery;

import static org.inaetics.wiring.discovery.DiscoveryConstants.DISCOVERY;
import static org.inaetics.wiring.discovery.DiscoveryConstants.DISCOVERY_TYPE;

import java.util.Properties;

import org.osgi.framework.BundleContext;

/**
 * Collection of Discovery specific utility methods.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class DiscoveryUtil {

    public static Properties createNodeListenerServiceProperties(BundleContext context, String discoveryType) {
        Properties properties = new Properties();
        properties.put(DISCOVERY, true);
        properties.put(DISCOVERY_TYPE, discoveryType);
        return properties;
    }

    private DiscoveryUtil() {
    }

}
