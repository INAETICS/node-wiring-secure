/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.config;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class FrameworkConfig extends Config {

    private String m_name;
    private int m_logLevel;
    private long m_serviceTimeout;

    private Map<String, String> m_properties = new HashMap<String, String>();
    private String m_bundlePaths;

    public FrameworkConfig(String name) {
        m_name = name;
    }

    public String getName() {
        return m_name;
    }

    public int getLogLevel() {
        return m_logLevel;
    }

    public long getServiceTimeout() {
        return m_serviceTimeout;
    }

    public Map<String, String> getProperties() {
        return m_properties;
    }

    public String getBundlePaths() {
        return m_bundlePaths;
    }

    public FrameworkConfig logLevel(int logLevel) {
        m_logLevel = logLevel;
        return this;
    }

    public FrameworkConfig serviceTimeout(long serviceTimeout) {
        m_serviceTimeout = serviceTimeout;
        return this;
    }

    public FrameworkConfig frameworkProperty(String key, String... values) {
        m_properties.put(key, join(values));
        return this;
    }

    public FrameworkConfig bundlePaths(String... bundlePaths) {
        m_bundlePaths = join(bundlePaths);
        return this;
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
