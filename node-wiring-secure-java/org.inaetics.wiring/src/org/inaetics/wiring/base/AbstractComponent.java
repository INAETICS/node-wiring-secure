/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.base;

import static org.inaetics.wiring.base.Constants.CONSOLE_PROP_PRE;
import static org.inaetics.wiring.base.Constants.LOGGING_PROP_PRE;
import static org.inaetics.wiring.base.ServiceUtil.getFrameworkUUID;

import java.lang.reflect.Array;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * Generic base class for service components. This class provides easy access to
 * the {@link BundleContext} and logging methods.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractComponent {

    private final String m_type;
    private final String m_name;
    private final String m_identifier;

    private int m_logLevel = LogService.LOG_INFO;
    private int m_conLevel = LogService.LOG_ERROR - 1;

    private volatile BundleContext m_bundleContext;
    private volatile LogService m_logService;

    /**
     * Create a new instance.
     * 
     * @param type The identifying name of logical component
     * @param name The identifying name of the implementation
     */
    public AbstractComponent(String type, String name) {
        m_type = type;
        m_name = name;
        m_identifier = type + "/" + name + "(" + (System.currentTimeMillis() % 1000) + ")";
    }

    /**
     * Final implementation of the DependecyManager lifecycle start callback.
     * 
     * @throws Exception
     * @see {@link #startComponent()}
     */
    protected final void start() throws Exception {

        m_logLevel = getLevelProperty(LOGGING_PROP_PRE + ".level", LogService.LOG_INFO);
        m_logLevel = getLevelProperty(LOGGING_PROP_PRE + "." + m_type + ".level", m_logLevel);
        m_logLevel = getLevelProperty(LOGGING_PROP_PRE + "." + m_type + "." + m_name + ".level", m_logLevel);

        m_conLevel = getLevelProperty(CONSOLE_PROP_PRE + ".level", LogService.LOG_ERROR + 1);
        m_conLevel = getLevelProperty(CONSOLE_PROP_PRE + "." + m_type + ".level", m_conLevel);
        m_conLevel = getLevelProperty(CONSOLE_PROP_PRE + "." + m_type + "." + m_name + ".level", m_conLevel);

        startComponent();
        logDebug("started (frameworkUUID=%s)", getFrameworkUUID(m_bundleContext));
    }

    /**
     * Final implementation of the DependecyManager lifecycle stop callback.
     * 
     * @throws Exception
     * @see {@link #stopComponent()}
     */
    protected final void stop() throws Exception {
        stopComponent();
        logDebug("stopped (frameworkUUID=%s)", getFrameworkUUID(m_bundleContext));
    }

    /**
     * Lifecycle method called when the component is started.
     * 
     * @throws Exception
     */
    protected void startComponent() throws Exception {
    }

    /**
     * Lifecycle method called when the component is started.
     * 
     * @throws Exception
     */
    protected void stopComponent() throws Exception {
    }

    /**
     * Returns the BundleContext
     * 
     * @return the BundleContext
     */
    public final BundleContext getBundleContext() {
        return m_bundleContext;
    }

    public final void logDebug(String message, Object... args) {
        log(LogService.LOG_DEBUG, message, null, args);
    }

    public final void logDebug(String message, Throwable cause, Object... args) {
        log(LogService.LOG_DEBUG, message, cause, args);
    }

    public final void logInfo(String message, Object... args) {
        log(LogService.LOG_INFO, message, null, args);
    }

    public final void logInfo(String message, Throwable cause, Object... args) {
        log(LogService.LOG_INFO, message, cause, args);
    }

    public final void logWarning(String message, Object... args) {
        log(LogService.LOG_WARNING, message, null, args);
    }

    public final void logWarning(String message, Throwable cause, Object... args) {
        log(LogService.LOG_WARNING, message, cause, args);
    }

    public final void logError(String message, Object... args) {
        log(LogService.LOG_ERROR, message, null, args);
    }

    public final void logError(String message, Throwable cause, Object... args) {
        log(LogService.LOG_ERROR, message, cause, args);
    }

    private final void log(int level, String message, Throwable cause, Object... args) {
        if (level <= m_logLevel || level <= m_conLevel) {
            if (args.length > 0) {
                message = String.format(message, processArgs(args));
            }
            message = m_identifier + " " + message;

            if (level <= m_logLevel && m_logService != null) {
                m_logService.log(level, message);
            }
            if (level <= m_conLevel) {
                System.out.println("[CONSOLE] " + getLevelName(level) + " " + message);
                if (cause != null) {
                    cause.printStackTrace(System.out);
                }
            }
        }
    }

    private static final Object[] processArgs(Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ServiceReference<?>) {
                args[i] = toString((ServiceReference<?>) args[i]);
            }
            else if (args[i] instanceof ServiceRegistration<?>) {
                args[i] = toString(((ServiceRegistration<?>) args[i]).getReference());
            }
            else if (args[i] instanceof Bundle) {
                args[i] = toString((Bundle) args[i]);
            }
        }
        return args;
    }

    private static String toString(ServiceReference<?> reference) {
        StringBuilder builder = new StringBuilder().append("{");
        for (String propertyKey : reference.getPropertyKeys()) {
            Object propertyValue = reference.getProperty(propertyKey);
            builder.append(propertyKey).append("=");
            if (propertyValue.getClass().isArray()) {
                builder.append("[");
                for (int i = 0; i < Array.getLength(propertyValue); i++) {
                    builder.append(Array.get(propertyValue, i));
                    if (i < Array.getLength(propertyValue) - 1) {
                        builder.append(", ");
                    }
                }
                builder.append("]");
            }
            else {
                builder.append(propertyValue.toString());
            }
            builder.append(", ");
        }
        builder.setLength(builder.length() - 2);
        return builder.toString();
    }

    private static String toString(Bundle bundle) {
        return "bundle(" + bundle.getBundleId() + ") " + bundle.getSymbolicName() + "/" + bundle.getVersion();
    }

    private final int getLevelProperty(String key, int def) {
        int result = def;
        String value = m_bundleContext.getProperty(key);
        if (value != null && !value.equals("")) {
            try {
                result = Integer.parseInt(value);
            }
            catch (Exception e) {
                // ignore
            }
        }
        return result;
    }

    private final String getLevelName(int level) {
        switch (level) {
            case 1:
                return "[ERROR  ]";
            case 2:
                return "[WARNING]";
            case 3:
                return "[INFO   ]";
            case 4:
                return "[DEBUG  ]";
            default:
                return "[?????]";
        }
    }
}
