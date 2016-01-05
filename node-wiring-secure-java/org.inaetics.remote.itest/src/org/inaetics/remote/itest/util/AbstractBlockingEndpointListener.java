/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.util;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Abstract Self-registering utility that uses a listener to count and await callbacks
 * for a specific {@link EndpointDescription}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 *
 * @param <T>
 */
public abstract class AbstractBlockingEndpointListener<T> {

    private final BundleContext m_context;
    private final EndpointDescription m_description;

    private volatile CountDownLatch m_addedLatch = new CountDownLatch(1);
    private volatile int m_addedCount = 0;

    private volatile CountDownLatch m_removedLatch = new CountDownLatch(1);
    private volatile int m_removedCount = 0;

    private volatile CountDownLatch m_modifiedLatch = new CountDownLatch(1);
    private volatile int m_modifiedCount = 0;

    private volatile CountDownLatch m_endmatchLatch = new CountDownLatch(1);
    private volatile int m_endmatchCount = 0;

    private volatile ServiceRegistration<?> m_registration;
    private volatile String m_scopeFilter;

    public AbstractBlockingEndpointListener(BundleContext context, String scopeFilter, EndpointDescription endpoint) {
        m_context = context;
        m_scopeFilter = scopeFilter;
        m_description = endpoint;
    }

    /**
     * Change the endPoint listener scope of the listener.
     * 
     * @param scopeFilter new scopeFilter
     */
    public final void changeScopeFilter(String scopeFilter) {
        checkRegistrationState();
        m_scopeFilter = scopeFilter;
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        for (String propertyKey : m_registration.getReference().getPropertyKeys()) {
            properties.put(propertyKey, m_registration.getReference().getProperty(propertyKey));
        }
        properties.put(getScopeKey(), m_scopeFilter);
        m_registration.setProperties(properties);
    }

    /**
     * Await the added callback. When this method returns the block is unregistered and must be reset before
     * it can be used again.
     * 
     * @param timeout timeout
     * @param unit unit
     * @return <code>true</code> if the call was received, otherwise <code>false</code>
     */
    public final boolean awaitAdded(long timeout, TimeUnit unit) throws InterruptedException {
        checkRegistrationState();
        try {
            return m_addedLatch.await(timeout, unit);
        }
        catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * Get the number of added callbacks received since last {@link #reset()}.
     * 
     * @return the count
     * @throws InterruptedException
     */
    public final int getAddedCount() {
        checkRegistrationState();
        return m_addedCount;
    }

    /**
     * Await the removed callback. When this method returns the block is unregistered and must be reset before
     * it can be used again.
     * 
     * @param timeout timeout
     * @param unit unit
     * @return <code>true</code> if the call was received, otherwise <code>false</code>
     */
    public final boolean awaitRemoved(long timeout, TimeUnit unit) {
        checkRegistrationState();
        try {
            return m_removedLatch.await(timeout, unit);
        }
        catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * Get the number of removed callbacks received since last {@link #reset()}.
     * 
     * @return the count
     * @throws InterruptedException
     */
    public final int getRemovedCount() throws InterruptedException {
        checkRegistrationState();
        return m_removedCount;
    }

    /**
     * Await the modified callback. When this method returns the block is unregistered and must be reset before
     * it can be used again.
     * 
     * @param timeout timeout
     * @param unit unit
     * @return <code>true</code> if the call was received, otherwise <code>false</code>
     */
    public final boolean awaitModified(long timeout, TimeUnit unit) {
        checkRegistrationState();
        try {
            return m_modifiedLatch.await(timeout, unit);
        }
        catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * Get the number of modified callbacks received since last {@link #reset()}.
     * 
     * @return the count
     * @throws InterruptedException
     */
    public final int getModifiedCount() throws InterruptedException {
        checkRegistrationState();
        return m_modifiedCount;
    }

    /**
     * Await the endmatch callback. When this method returns the block is unregistered and must be reset before
     * it can be used again.
     * 
     * @param timeout timeout
     * @param unit unit
     * @return <code>true</code> if the call was received, otherwise <code>false</code>
     */
    public final boolean awaitEndmatch(long timeout, TimeUnit unit) {
        checkRegistrationState();
        try {
            return m_endmatchLatch.await(timeout, unit);
        }
        catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * Get the number of endmatch callbacks received since last {@link #reset()}.
     * 
     * @return the count
     * @throws InterruptedException
     */
    public final int getEndmatchCount() throws InterruptedException {
        checkRegistrationState();
        return m_endmatchCount;
    }

    public final synchronized void register() {
        unregister();
        m_registration = m_context.registerService(getObjectClass(), getListener(), new Hashtable<String, Object>() {
            private static final long serialVersionUID = 1L;

            {
                put(getScopeKey(), m_scopeFilter);
            }
        });

    }

    public synchronized void unregister() {
        if (m_registration != null) {
            m_registration.unregister();
            m_registration = null;
        }
    }

    /**
     * Resets the block to be used again.
     */
    public synchronized void reset() {
        m_addedLatch = new CountDownLatch(1);
        m_addedCount = 0;
        m_removedLatch = new CountDownLatch(1);
        m_removedCount = 0;
        m_modifiedLatch = new CountDownLatch(1);
        m_modifiedCount = 0;
        m_endmatchLatch = new CountDownLatch(1);
        m_endmatchCount = 0;
    }

    protected void endpointAdded(EndpointDescription endpoint) {
        if (m_description.isSameService(endpoint)) {
            m_addedCount++;
            m_addedLatch.countDown();
        }
    }

    protected void endpointRemoved(EndpointDescription endpoint) {
        if (m_description.isSameService(endpoint)) {
            m_removedCount++;
            m_removedLatch.countDown();
        }
    }

    protected void endpointModified(EndpointDescription endpoint) {
        if (m_description.isSameService(endpoint)) {
            m_modifiedCount++;
            m_modifiedLatch.countDown();
        }
    }

    protected void endpointEndmatch(EndpointDescription endpoint) {
        if (m_description.isSameService(endpoint)) {
            m_endmatchCount++;
            m_endmatchLatch.countDown();
        }
    }

    protected abstract Object getListener();

    protected abstract String getScopeKey();

    protected abstract String[] getObjectClass();

    private final void checkRegistrationState() {
        if (m_registration == null) {
            throw new IllegalStateException("Unregistered state! Use reset if you want to reuse this listener.");
        }
    }
}