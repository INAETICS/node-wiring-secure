/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.demo.echoClient;

import org.inaetics.remote.demo.echoService.EchoService;
import org.inaetics.remote.demo.echoService2.EchoService2;
import org.osgi.service.log.LogService;

public class EchoClient {

	private volatile EchoService m_echoService;
	private volatile EchoService2 m_echoService2;
	private volatile LogService m_logService;
	
	public String tm() {
		String result = sendMessage("test message1") + "\n";
		return result + sendMessage2("test message2");
	}
	
	public String sendMessage(String message) {
		String response = m_echoService.echo(message);
		m_logService.log(LogService.LOG_INFO, "message response: %s" + response);
		return response;
	}

	public String sendMessage2(String message) {
		String response = m_echoService2.echo(message);
		m_logService.log(LogService.LOG_INFO, "message response: %s" + response);
		return response;
	}
}
