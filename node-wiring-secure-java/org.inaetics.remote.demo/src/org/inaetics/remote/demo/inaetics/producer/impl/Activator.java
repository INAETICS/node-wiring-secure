package org.inaetics.remote.demo.inaetics.producer.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.remote.demo.inaetics.producer.Producer;
import org.inaetics.remote.demo.inaetics.queue.Queue;
import org.osgi.framework.BundleContext;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(BundleContext context, DependencyManager manager) throws Exception {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, Producer.class.getName());
		
		manager.add(createComponent()
			.setInterface(Producer.class.getName(), properties)
			.setImplementation(ProducerImpl.class)
			.add(createServiceDependency().setService(Queue.class).setRequired(true))
		);

	}

}
