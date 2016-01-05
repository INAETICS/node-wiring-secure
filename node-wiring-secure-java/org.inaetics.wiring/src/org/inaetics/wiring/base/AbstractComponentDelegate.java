/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.base;

import org.osgi.framework.BundleContext;

/**
 * Generic base class for service components delegates that provides easy access to the
 * component Bundle Context and methods for logging.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractComponentDelegate {

    private final AbstractComponent m_abstractComponent;

    public final void start() throws Exception {
        startComponentDelegate();
    }

    public final void stop() throws Exception {
        stopComponentDelegate();
    }

    protected void startComponentDelegate() throws Exception {
    }

    protected void stopComponentDelegate() throws Exception {
    }

    public AbstractComponentDelegate(AbstractComponent abstractComponent) {
        m_abstractComponent = abstractComponent;
    }

    public final BundleContext getBundleContext() {
        return m_abstractComponent.getBundleContext();
    }

    public final void logDebug(String message, Object... args) {
        m_abstractComponent.logDebug(message, args);
    }

    public final void logDebug(String message, Throwable cause, Object... args) {
        m_abstractComponent.logDebug(message, cause, args);
    }

    public final void logInfo(String message, Object... args) {
        m_abstractComponent.logDebug(message, args);
    }

    public final void logInfo(String message, Throwable cause, Object... args) {
        m_abstractComponent.logInfo(message, cause, args);
    }

    public final void logWarning(String message, Object... args) {
        m_abstractComponent.logWarning(message, args);
    }

    public final void logWarning(String message, Throwable cause, Object... args) {
        m_abstractComponent.logWarning(message, cause, args);
    }

    public final void logError(String message, Object... args) {
        m_abstractComponent.logError(message, args);
    }

    public final void logError(String message, Throwable cause, Object... args) {
        m_abstractComponent.logError(message, cause, args);
    }
}
