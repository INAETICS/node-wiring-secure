/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.demo.echoService2.impl;

import org.inaetics.remote.demo.echoService2.EchoService2;
import org.osgi.service.log.LogService;

public class SimpleEchoService2 implements EchoService2 {

	private volatile LogService m_logService;
	
	@Override
	public String echo(String message) {

		m_logService.log(LogService.LOG_DEBUG, "received message: " + message);
		return "echo2: " + message;
		
	}


}
