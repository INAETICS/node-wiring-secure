/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.config;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class Configs {

    public static Config[] configs(Config... configs) {
        return configs;
    }

    public static BundlesConfig bundlesConfig(String... bundlePaths) {
        return new BundlesConfig(bundlePaths);
    }

    public static FrameworkConfig frameworkConfig(String name) {
        return new FrameworkConfig(name);
    }
}
