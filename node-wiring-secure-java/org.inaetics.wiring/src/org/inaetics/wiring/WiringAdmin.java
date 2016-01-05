/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

import org.inaetics.wiring.endpoint.WiringReceiver;

/**
 * A Wiring Admin manages the import and export of services.
 * 
 * @ThreadSafe
 * @author $Id: 92eba8188a1b59224ce3494fc5b04cafce490330 $
 */
public interface WiringAdmin {

	/**
	 * Export an Endpoint.
	 */
	public ExportRegistration exportEndpoint(WiringReceiver listener);

	/**
	 * Import an Endpoint.
	 */
	public ImportRegistration importEndpoint(WiringEndpointDescription endpoint);
}
