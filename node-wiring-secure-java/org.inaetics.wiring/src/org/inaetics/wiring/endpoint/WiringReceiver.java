/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.endpoint;

public interface WiringReceiver {

	public String messageReceived(String message) throws Exception;
	
	public void wiringEndpointAdded(String wireId);
	public void wiringEndpointRemoved(String wireId);
	
}
