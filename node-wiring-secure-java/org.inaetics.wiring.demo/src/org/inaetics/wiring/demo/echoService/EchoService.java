/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.demo.echoService;

import org.inaetics.wiring.endpoint.WiringReceiver;
import org.osgi.service.log.LogService;

public class EchoService implements WiringReceiver {

	private volatile LogService m_logService;
	
	@Override
	public String messageReceived(String message) {

		m_logService.log(LogService.LOG_DEBUG, "received message: " + message);
		return "echo: " + message;
		
	}

	@Override
	public void wiringEndpointAdded(String wireId) {
		m_logService.log(LogService.LOG_INFO, "endpoint added: " + wireId);
	}

	@Override
	public void wiringEndpointRemoved(String wireId) {
		m_logService.log(LogService.LOG_INFO, "endpoint removed: " + wireId);
	}

}
