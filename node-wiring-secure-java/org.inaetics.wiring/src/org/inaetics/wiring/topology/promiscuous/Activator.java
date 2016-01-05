/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.topology.promiscuous;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.wiring.WiringEndpointEventListener;
import org.inaetics.wiring.WiringAdmin;
import org.inaetics.wiring.WiringAdminListener;
import org.inaetics.wiring.endpoint.WiringReceiver;
import org.inaetics.wiring.endpoint.WiringTopologyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

/**
 * Activator for the Amdatu Topology Manager service implementation.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {

        String[] objectClass =
            new String[] { WiringAdminListener.class.getName(), WiringEndpointEventListener.class.getName(),
                ManagedService.class.getName(), WiringTopologyManager.class.getName() };

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, PromiscuousTopologyManager.SERVICE_PID);

        PromiscuousTopologyManager promiscuousTopologyManager = new PromiscuousTopologyManager(manager);
        
        manager.add(
            createComponent()
                .setInterface(objectClass, properties)
                .setImplementation(promiscuousTopologyManager)
                .add(createServiceDependency()
                    .setService(LogService.class)
                    .setRequired(false))
                .add(createServiceDependency()
                    .setService(WiringAdmin.class)
                    .setCallbacks("wiringAdminAdded", "wiringAdminRemoved")
                    .setRequired(true))
                .add(createServiceDependency()
                    .setService(WiringReceiver.class)
                    .setCallbacks("wiringReceiverAdded", "wiringReceiverRemoved")
                    .setRequired(false))
                .add(createServiceDependency()
                    .setService(WiringEndpointEventListener.class)
                    .setCallbacks("eventListenerAdded", "eventListenerModified", "eventListenerRemoved")
                    .setRequired(false))
            );
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager)
        throws Exception {
    }
}
