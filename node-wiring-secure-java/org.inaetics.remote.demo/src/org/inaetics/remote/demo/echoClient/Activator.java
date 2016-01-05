/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.demo.echoClient;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.service.command.CommandProcessor;
import org.inaetics.remote.demo.echoService.EchoService;
import org.inaetics.remote.demo.echoService2.EchoService2;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class Activator extends DependencyActivatorBase {

	private Component m_component;

	@Override
	public void init(BundleContext context, DependencyManager manager)
			throws Exception {

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(CommandProcessor.COMMAND_SCOPE, "echo");
		properties.put(CommandProcessor.COMMAND_FUNCTION, new String[]{"sendMessage", "sendMessage2", "tm" });
		
		m_component = createComponent()
			.setInterface(Object.class.getName(), properties)
			.setImplementation(EchoClient.class)
			.add(createServiceDependency().setService(EchoService.class).setRequired(false))
			.add(createServiceDependency().setService(EchoService2.class).setRequired(false))
			.add(createServiceDependency().setService(LogService.class).setRequired(false));
		
		manager.add(m_component);
	}

	@Override
	public void destroy(BundleContext context, DependencyManager manager)
			throws Exception {
		
		manager.remove(m_component);
		
	}


}
