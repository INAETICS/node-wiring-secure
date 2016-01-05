/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import static org.inaetics.remote.admin.wiring.WiringAdminConstants.SUPPORTED_CONFIGURATION_TYPES;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.SUPPORTED_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_CONFIGS_SUPPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_INTENTS_SUPPORTED;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.wiring.endpoint.WiringSender;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * Activator and configuration manager for the Amdatu HTTP Remote Service Admin service implementation.
 * <p>
 * Configuration can be provided through cm as well as system properties. The former take precedence and
 * in addition some fallbacks and defaults are provided. See {@link WiringAdminConstants} for supported
 * configuration properties.
 * <p>
 * Note that any effective configuration change will close all existing import- and export registrations.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class Activator extends DependencyActivatorBase {

	private volatile DependencyManager m_dependencyManager;
    private volatile Component m_factoryComponent;

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        m_dependencyManager = manager;

        try {
            registerFactoryService();
        }
        catch (Exception e) {
            throw new ConfigurationException("init", "error", e);
        }
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {

        unregisterFactoryService();
        
    }

    private void registerFactoryService() {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(REMOTE_CONFIGS_SUPPORTED, SUPPORTED_CONFIGURATION_TYPES);
        properties.put(REMOTE_INTENTS_SUPPORTED, SUPPORTED_INTENTS);

        RemoteServiceAdminFactory factory = new RemoteServiceAdminFactory(m_dependencyManager);

        Component component = createComponent()
            .setInterface(RemoteServiceAdmin.class.getName(), properties)
            .setImplementation(factory)
            .add(createServiceDependency()
                .setService(LogService.class)
                .setRequired(false))
            .add(createServiceDependency()
                .setService(RemoteServiceAdminListener.class)
                .setCallbacks(factory.getEventsHandler(), "listenerAdded", "listenerRemoved")
                .setRequired(false))
            .add(createServiceDependency()
                .setService(EventAdmin.class)
                .setCallbacks(factory.getEventsHandler(), "eventAdminAdded", "eventAdminRemoved")
                .setRequired(false))
            .add(createServiceDependency()
        		.setService(WiringSender.class)
        		.setRequired(false)
        		.setCallbacks("wiringSenderAdded", "wiringSenderRemoved"));

        m_factoryComponent = component;
        m_dependencyManager.add(component);
    }

    private void unregisterFactoryService() {
        Component component = m_factoryComponent;
        m_factoryComponent = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }
    }

}
