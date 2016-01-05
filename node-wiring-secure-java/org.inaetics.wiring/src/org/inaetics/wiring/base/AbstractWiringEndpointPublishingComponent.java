/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.base;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.WiringEndpointEvent;
import org.inaetics.wiring.WiringEndpointEventListener;
import org.osgi.framework.ServiceReference;

/**
 * Base class for service components that wish to inform listeners about Wiring Endpoint Description
 * events.<p>
 * 
 * This implementation synchronizes all events/calls through an internal queue. This provides a simple and safe
 * programming model for concrete implementations which may also leverage the the queue for ordered
 * asynchronous execution by calling {@link #executeTask(Runnable)}.<p>
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractWiringEndpointPublishingComponent extends AbstractComponent {

    private final Map<ServiceReference<?>, AbstractListenerHandler<?>> m_listeners =
        new HashMap<ServiceReference<?>, AbstractListenerHandler<?>>();

    private final Set<WiringEndpointDescription> m_endpoints = new HashSet<WiringEndpointDescription>();

    private volatile ExecutorService m_executor;

    public AbstractWiringEndpointPublishingComponent(String type, String name) {
        super(type, name);
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();
        m_executor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void stopComponent() throws Exception {
        m_executor.shutdown();
        m_executor = null;
        super.stopComponent();
    }

    /**
     * Component callback for Wiring Endpoint Event Listener addition.
     * 
     * @param reference The Service Reference of the added Wiring Endpoint Event Listener
     * @param listener The Wiring Endpoint Event Listener
     */
    final void eventListenerAdded(final ServiceReference<WiringEndpointEventListener> reference,
        final WiringEndpointEventListener listener) {

        if (listener == this) {
            logDebug("Ignoring Wiring Endpoint Event Listener because it is a reference this service instance: %s", reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Adding Wiring Endpoint Event Listener %s", reference);
                try {
                    WiringEndpointEventListenerHandler handler =
                        new WiringEndpointEventListenerHandler(reference, listener, m_endpoints);
                    AbstractListenerHandler<?> previous = m_listeners.put(reference, handler);
                    if (previous != null) {
                        logWarning("Wiring Endpoint Event Listener overwrites previous mapping %s", reference);
                    }
                }
                catch (Exception e) {
                    logError("Failed to handle added Wiring Endpoint Event Listener %s", e, reference);
                }
            }
        });
    }

    /**
     * Component callback for Wiring Endpoint Event Listener modification.
     * 
     * @param reference The Service Reference of the added Wiring Endpoint Event Listener
     * @param listener The Wiring Endpoint Event Listener
     */
    final void eventListenerModified(final ServiceReference<WiringEndpointEventListener> reference,
        final WiringEndpointEventListener listener) {

        if (listener == this) {
            logDebug("Ignoring Wiring Endpoint Event Listener because it is a reference this service instance: %s", reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Modifying Wiring Endpoint Event Listener %s", listener);
                try {
                    AbstractListenerHandler<?> handler = m_listeners.get(reference);
                    if (handler == null) {
                        logWarning("Failed to locate modified Wiring Endpoint Event Listener %s", reference);
                    }
                }
                catch (Exception e) {
                    logError("Failed to handle modified Wiring Endpoint Event Listener %s", e, reference);
                }
            }
        });

    }

    /**
     * Component callback for Wiring Endpoint Event Listener removal.
     * 
     * @param reference The Service Reference of the added Wiring Endpoint Event Listener
     * @param listener The Wiring Endpoint Event Listener
     */
    final void eventListenerRemoved(final ServiceReference<WiringEndpointEventListener> reference,
        final WiringEndpointEventListener listener) {

        if (listener == this) {
            logDebug("Ignoring Event Wiring Endpoint Listener because it is a reference this service instance: %s", reference);
            return;
        }

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Removing Wiring Endpoint Event Listener %s", reference);
                AbstractListenerHandler<?> removed = m_listeners.remove(reference);
                if (removed == null) {
                    logWarning("Failed to locate removed Wiring Endpoint Event Listener %s", reference);
                }
            }
        });
    }

    /**
     * Submit a task for synchronous execution.
     * 
     * @param task the task
     */
    protected final void executeTask(Runnable task) {
        m_executor.submit(task);
    }
 
    /**
     * Call endpoint added on all registered listeners with as scope that matches the specified WiringEndpointDescription.
     * 
     * @param endpoint The Wiring Endpoint Description
     * @throws IllegalStateException if called with a previsouly added Wiring Endpoint Description
     */
    public final void endpointAdded(final WiringEndpointDescription endpoint) {

        executeTask(new Runnable() {

            @Override
            public void run() {
                logDebug("Adding Wiring Endpoint: %s", endpoint);
                if (!m_endpoints.add(endpoint)) {
                    throw new IllegalStateException("Trying to add duplicate Wiring Endpoint Description: " + endpoint);
                }
                for (AbstractListenerHandler<?> handler : m_listeners.values()) {
                    try {
                        handler.endpointAdded(endpoint);
                    }
                    catch (Exception e) {
                        logWarning("Caught exception while invoking Wiring Endpoint added on %s", e, handler.getReference());
                    }
                }
            }
        });
    }

    /**
     * Call endpoint removed on all registered listeners with a scope that matches the specified WiringEndpointDescription.
     * 
     * @param endpoint The Wiring Endpoint Description
     * @throws IllegalStateException if called with an unknown Wiring Endpoint Description
     */
    public final void endpointRemoved(final WiringEndpointDescription endpoint) {

        executeTask(new Runnable() {

            @Override
            public void run() {

                logDebug("Removing Wiring Endpoint: %s", endpoint);
                if (!m_endpoints.remove(endpoint)) {
                    throw new IllegalStateException("Trying to remove unknown Wiring Endpoint Description: " + endpoint);
                }
                for (AbstractListenerHandler<?> handler : m_listeners.values()) {
                    try {
                        handler.endpointRemoved(endpoint);
                    }
                    catch (Exception e) {
                        logWarning("Caught exception while invoking Wiring Endpoint removed on %s", e, handler.getReference());
                    }
                }
            }
        });
    }

    /**
     * Abstract handler for listeners that encapsulates filter parsing, caching and matching
     * <p>
     * This implementation is not thread-safe. Synchronization is handled from the outside.
     * 
     * @param <T> The concrete listener type
     */
    private static abstract class AbstractListenerHandler<T> {

        private final ServiceReference<T> m_reference;
        private final T m_listener;

        /**
         * Constructs a new handler and initializes by calling {@link #referenceModified(Collection)} internally.
         * 
         * @param reference The listener Service Reference
         * @param listener The listener of type T
         * @param scopeKey The scope property key
         * @param endpoints The current wiring endpoint collection
         * @throws Exception If the initialization fails
         */
        public AbstractListenerHandler(ServiceReference<T> reference, T listener,
            Collection<WiringEndpointDescription> endpoints) throws Exception {

            m_reference = reference;
            m_listener = listener;
        }

        /**
         * Returns the listener.
         * 
         * @return The listener
         */
        public final T getListener() {
            return m_listener;
        }

        /**
         * Return the reference.
         * 
         * @return The reference
         */
        public final ServiceReference<T> getReference() {
            return m_reference;
        }
        
        /**
         * Invoke the relevant callback on the listener.
         * 
         * @param endpoint The Wiring Endpoint Description
         */
        protected abstract void endpointAdded(WiringEndpointDescription endpoint);

        /**
         * Invoke the relevant callback on the listener.
         * 
         * @param endpoint The Wiring Endpoint Description
         */
        protected abstract void endpointRemoved(WiringEndpointDescription endpoint);

    }

    /**
     * Concrete holder for type Wiring Endpoint Event Listener.
     */
    private static class WiringEndpointEventListenerHandler extends AbstractListenerHandler<WiringEndpointEventListener> {

        public WiringEndpointEventListenerHandler(ServiceReference<WiringEndpointEventListener> reference,
            WiringEndpointEventListener listener, Collection<WiringEndpointDescription> endpoints) throws Exception {
            super(reference, listener, endpoints);
            
            for(WiringEndpointDescription endpoint : endpoints) {
            	endpointAdded(endpoint);
            }
        }

        @Override
        protected void endpointAdded(WiringEndpointDescription description) {
            try {
                getListener().endpointChanged(new WiringEndpointEvent(WiringEndpointEvent.ADDED, description));
            }
            catch (Exception e) {}
        }

        @Override
        protected void endpointRemoved(WiringEndpointDescription description) {
            try {
                getListener().endpointChanged(new WiringEndpointEvent(WiringEndpointEvent.REMOVED, description));
            }
            catch (Exception e) {}
        }

    }

}
