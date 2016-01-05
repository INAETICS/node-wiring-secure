/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.WiringEndpointEvent;
import org.inaetics.wiring.WiringEndpointEventListener;
import org.inaetics.wiring.base.AbstractComponentDelegate;

public class WiringEndpointEventHandler extends AbstractComponentDelegate implements WiringEndpointEventListener {

    private final Set<WiringEndpointDescription> m_importedEndpoints =
    		Collections.newSetFromMap(new ConcurrentHashMap<WiringEndpointDescription, Boolean>());
	
    private final WiringAdminFactory m_manager;
    private final HttpsAdminConfiguration m_configuration;
    private final HttpsClientEndpointFactory m_clientFactory;

	public WiringEndpointEventHandler(WiringAdminFactory wiringAdminFactory,
			HttpsAdminConfiguration configuration, HttpsClientEndpointFactory clientEndpointFactory) {
		super (wiringAdminFactory);
		m_manager = wiringAdminFactory;
        m_configuration = configuration;
        m_clientFactory = clientEndpointFactory;
	}

	@Override
	public void endpointChanged(WiringEndpointEvent event) {
		int type = event.getType();
		switch (type) {
			case WiringEndpointEvent.ADDED:
				endpointAdded(event.getEndpoint()); break;
			case WiringEndpointEvent.REMOVED:
				endpointRemoved(event.getEndpoint()); break;
		}
	}

	private void endpointAdded(WiringEndpointDescription endpoint) {
		m_importedEndpoints.add(endpoint);
		m_clientFactory.addEndpoint(endpoint);
	}

	private void endpointRemoved(WiringEndpointDescription endpoint) {
		m_importedEndpoints.remove(endpoint);
		m_clientFactory.removeEndpoint(endpoint);
	}
	
}
