/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.demo.echoService.impl;

import org.inaetics.remote.demo.echoService.EchoService;
import org.osgi.service.log.LogService;

public class SimpleEchoService implements EchoService {

	private volatile LogService m_logService;
	
	@Override
	public String echo(String message) {

		m_logService.log(LogService.LOG_DEBUG, "received message: " + message);
		return "echo: " + message;
		
	}


}
