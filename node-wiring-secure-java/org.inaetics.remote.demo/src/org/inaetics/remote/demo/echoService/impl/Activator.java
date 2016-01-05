/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.demo.echoService.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.remote.demo.echoService.EchoService;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class Activator extends DependencyActivatorBase {

	private Component m_component;
	
	@Override
	public void init(BundleContext context, DependencyManager manager)
			throws Exception {

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, EchoService.class.getName());
		
		m_component = createComponent()
			.setInterface(EchoService.class.getName(), properties)
			.setImplementation(SimpleEchoService.class)
			.add(createServiceDependency().setService(LogService.class).setRequired(false));
		
		manager.add(m_component);
	}

	@Override
	public void destroy(BundleContext context, DependencyManager manager)
			throws Exception {
		
		manager.remove(m_component);
		
	}


}
