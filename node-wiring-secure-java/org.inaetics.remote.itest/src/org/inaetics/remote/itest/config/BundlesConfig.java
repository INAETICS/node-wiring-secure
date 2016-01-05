/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.config;


/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class BundlesConfig extends Config {

    private String m_bundlePaths;

    public BundlesConfig(String... bundlePaths) {
        m_bundlePaths = join(bundlePaths);
    }

    public String getBundlePaths() {
        return m_bundlePaths;
    }

    private static String join(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(part);
        }
        return builder.toString();
    }
}
