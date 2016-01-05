/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

import org.inaetics.wiring.endpoint.WiringReceiver;

public interface ExportReference {
	
	/**
	 * Return the service being exported.
	 * 
	 * @return The service being exported. Must be {@code null} when the service
	 *         is no longer exported.
	 */
	WiringReceiver getWiringReceiver();

	/**
	 * Return the Endpoint Description for the local endpoint.
	 * 
	 * @return The Endpoint Description for the local endpoint. Must be
	 *         {@code null} when the service is no longer exported.
	 */
	WiringEndpointDescription getEndpointDescription();
}
