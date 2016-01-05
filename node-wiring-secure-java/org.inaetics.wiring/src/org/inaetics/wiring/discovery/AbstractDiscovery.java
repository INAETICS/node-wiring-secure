/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.discovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.WiringEndpointEvent;
import org.inaetics.wiring.WiringEndpointEventListener;
import org.inaetics.wiring.base.AbstractWiringEndpointPublishingComponent;
import org.inaetics.wiring.discovery.etcd.EtcdDiscoveryConfiguration;

/**
 * Base class for a Discovery Service that handles wiring endpoint registration as well as listener tracking
 * and invocation.<br/><br/>
 * 
 * This implementation synchronizes all local and remote events/calls through an internal queue to
 * provide a simple and safe programming model for concrete implementations.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractDiscovery extends AbstractWiringEndpointPublishingComponent implements WiringEndpointEventListener {

    private final EtcdDiscoveryConfiguration m_configuration;
    private final ConcurrentHashMap<String, WiringEndpointDescription> m_endpoints = new ConcurrentHashMap<String, WiringEndpointDescription>();

	public AbstractDiscovery(String name, EtcdDiscoveryConfiguration configuration) {
        super("discovery", name);
        m_configuration = configuration;
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();
    }

    @Override
    protected void stopComponent() throws Exception {
        super.stopComponent();
    }

    @Override
    public void endpointChanged(final WiringEndpointEvent event) {

        switch (event.getType()) {
            case WiringEndpointEvent.ADDED:
                executeTask(new Runnable() {

                    @Override
                    public void run() {
                        logInfo("Added local endpoint: %s", event.getEndpoint());
                        addPublishedEndpoint(event.getEndpoint());
                    }
                });
                break;
            case WiringEndpointEvent.REMOVED:
                executeTask(new Runnable() {

                    @Override
                    public void run() {
                        logInfo("Removed local endpoint: %s", event.getEndpoint());
                        removePublishedEndpoint(event.getEndpoint());
                    }
                });
                break;
            default:
                throw new IllegalStateException("Recieved event with unknown type " + event.getType());
        }
    }
 
    /**
     * Set all discovered remote endpoints and invoke relevant listeners.
     * 
     * @param newEndpoints The Wiring Endpoint Description
     */
    protected final void setDiscoveredEndpoints(final List<WiringEndpointDescription> newEndpoints) {

        // first remove old urls
        List<WiringEndpointDescription> toRemove = new ArrayList<>();
        for (WiringEndpointDescription oldEndpoint : m_endpoints.values()) {
            if (!newEndpoints.contains(oldEndpoint)) {
                toRemove.add(oldEndpoint);
            }
        }
        for (WiringEndpointDescription removedEndpoint : toRemove) {
        	removeDiscoveredEndpoint(removedEndpoint);
        }
        // add missing urls
        Collection<WiringEndpointDescription> oldEndpoints = m_endpoints.values();
        for (WiringEndpointDescription newEndpoint : newEndpoints) {
            if (!oldEndpoints.contains(newEndpoint)) {
                addDiscoveredEndpoint(newEndpoint);
            }
        }

    }

    /**
     * Register a newly discovered remote wiring endpoint and invoke relevant listeners. Concrete implementations must
     * call this method for every applicable remote registration they discover.
     * 
     * @param endpoint The service Wiring Endpoint Description
     */
    protected final void addDiscoveredEndpoint(final WiringEndpointDescription endpoint) {

    	if (isLocalEndpoint(endpoint)) {
    		return;
    	}
    	
    	// check if sth as changed
    	WiringEndpointDescription oldEndpoint = m_endpoints.put(endpoint.getId(), endpoint);
    	if (oldEndpoint != null && oldEndpoint.equals(endpoint)) {
    		return;
    	}
    	
    	executeTask(new Runnable() {

            @Override
            public void run() {
                logInfo("Adding remote endpoint: %s", endpoint);
                endpointAdded(endpoint);
            }
        });
    }

    /**
     * Unregister a previously discovered remote wiring endpoint and invoke relevant listeners. Concrete
     * implementations must call this method for every applicable remote registration that disappears.
     * 
     * @param endpoint The service Wiring Endpoint Description
     */
    protected final void removeDiscoveredEndpoint(final WiringEndpointDescription endpoint) {

    	if (isLocalEndpoint(endpoint)) {
    		return;
    	}

    	m_endpoints.remove(endpoint.getId());
    	
    	executeTask(new Runnable() {

            @Override
            public void run() {
                logInfo("Removed remote endpoint: %s", endpoint);
                endpointRemoved(endpoint);
            }
        });
    }

    private boolean isLocalEndpoint(WiringEndpointDescription endpointDescription) {
    	//todo check framework uuid?!
    	return false;
    }
    
    /**
     * Modifies a previously discovered remote wiring endpoint and invoke relevant listeners. Concrete
     * implementations must call this method for every applicable remote registration that disappears.
     * 
     * @param endpoint The Wiring Endpoint Description
     */
    protected final void modifyDiscoveredEndpoint(WiringEndpointDescription endpoint) {

        addDiscoveredEndpoint(endpoint);
    }

    /**
     * Called when an wiring endpoint is published. The concrete implementation is responsible for registering
     * the service in its service registry.
     * 
     * @param endpoint The Wiring Endpoint Description
     */
    protected abstract void addPublishedEndpoint(WiringEndpointDescription endpoint);

    /**
     * Called when an exported wiring endpoint is depublished. The concrete implementation is responsible for unregistering
     * the service in its service registry.
     * 
     * @param endpoint The Wiring Endpoint Description
     */
    protected abstract void removePublishedEndpoint(WiringEndpointDescription endpoint);

}
