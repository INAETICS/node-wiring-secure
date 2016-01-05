/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

/**
 * A white board service that represents a listener for wiring endpoints.
 * 
 * A Wiring Endpoint Event Listener represents a participant in the distributed model
 * that is interested in Wiring Descriptions.
 * 
 * In general, when an Wiring Endpoint Description is discovered, it should be
 * dispatched to all registered Wiring Endpoint Event Listener services. If a new
 * Wiring Endpoint Event Listener is registered, it should be informed about all
 * currently known Wiring Endpoints. If a getter of the Wiring Endpoint
 * Listener service is unregistered, then all its registered Wiring Endpoint
 * Description objects must be removed.
 * 
 * The Wiring Endpoint Event Listener models a <i>best effort</i> approach.
 * Participating bundles should do their utmost to keep the listeners up to
 * date, but implementers should realize that many wirings come through
 * unreliable discovery processes.
 * 
 * @ThreadSafe
 * @since 1.1
 */
public interface WiringEndpointEventListener {

	/**
	 * Notification that an wiring endpoint has changed.
	 * 
	 * Details of the change is captured in the Wiring Endpoint Event provided. This
	 * could be that an wiring endpoint was added or removed.
	 * 
	 * @param event The event containing the details about the change.
	 */
	void endpointChanged(WiringEndpointEvent event);
}
