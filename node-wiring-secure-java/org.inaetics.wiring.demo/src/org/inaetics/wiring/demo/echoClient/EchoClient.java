/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.demo.echoClient;

import org.inaetics.wiring.demo.Util;
import org.inaetics.wiring.endpoint.WiringSender;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class EchoClient {

	private volatile LogService m_logService;
	private volatile BundleContext m_context;
	
	public void tm() {
		String wireId = ""; // get the wire id somehow...
		sendMessage(wireId, "test message");
	}
	
	public String sendMessage(String wireId, String message) {

		try {
			WiringSender wiringSender = Util.getWiringSender(m_context, wireId);
			if (wiringSender == null) {
				m_logService.log(LogService.LOG_ERROR, "endpoint not found for message %s" + message);
				return "error: no sender for given id found";
			}
			else {
				String response = wiringSender.sendMessage(message);
				m_logService.log(LogService.LOG_INFO, "message response: %s" + response);
				return response;
			}			
		} catch (Throwable e) {
			m_logService.log(LogService.LOG_ERROR, "error sending message " + message, e);
			return "error: " + e.getMessage();
		}
		
	}

}
