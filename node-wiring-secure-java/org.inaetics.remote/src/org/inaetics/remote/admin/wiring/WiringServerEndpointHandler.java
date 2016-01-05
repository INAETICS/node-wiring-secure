/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import static org.osgi.framework.Constants.OBJECTCLASS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.inaetics.remote.AbstractComponentDelegate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

/**
 * RSA component that handles all server endpoints.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringServerEndpointHandler extends AbstractComponentDelegate {

    private final Map<String, WiringServerEndpoint> m_handlers = new HashMap<String, WiringServerEndpoint>();
    private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

    private final RemoteServiceAdminFactory m_factory;

    private final ObjectMapper m_objectMapper = new ObjectMapper();
    private final JsonFactory m_jsonFactory = new JsonFactory(m_objectMapper);
    private static final String APPLICATION_JSON = "application/json";

    public WiringServerEndpointHandler(RemoteServiceAdminFactory factory) {
        super(factory);
        m_factory = factory;
    }
    
    /**
     * Add a Server Endpoint.
     * @param admin 
     * @param exportedEndpoint 
     * 
     * @param reference The local Service Reference
     * @param properties 
     * @param properties The Endpoint Description
     * @param endpointId 
     */
    public WiringServerEndpoint addEndpoint(RemoteServiceAdminImpl admin, ExportRegistration exportRegistration,
    		ServiceReference<?> reference, Map<String, ?> properties, Map<String, String> extraProperties, String endpointId) {

        // TODO sanity check and throw exception se the Export Handler can deal with it
        String[] endpointInterfaces = (String[]) reference.getProperty(OBJECTCLASS);
        Class<?>[] serviceInterfaces = getServiceInterfaces(getBundleContext(), reference);
        Class<?>[] exportedInterfaces = getExportInterfaceClasses(serviceInterfaces, endpointInterfaces);
        WiringServerEndpoint serverEndpoint = new WiringServerEndpoint(m_factory, admin, exportRegistration, getBundleContext(), reference, extraProperties, exportedInterfaces);

        m_lock.writeLock().lock();
        try {
            m_handlers.put(endpointId, serverEndpoint);
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serverEndpoint;
    }

    /**
     * Remove a Server Endpoint.
     * 
     * @param endpoint The Endpoint Description
     */
    public WiringServerEndpoint removeEndpoint(EndpointDescription endpoint) {
    	return removeEndpoint(endpoint.getId());
    }

    /**
     * Remove a Server Endpoint.
     * 
     * @param endpointId The Endpoint Id
     */
    public WiringServerEndpoint removeEndpoint(String endpointId) {
        WiringServerEndpoint serv;

        m_lock.writeLock().lock();
        try {
            serv = m_handlers.remove(endpointId);
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serv;
    }
    
    /**
     * Returns an array of interface classes implemented by the service instance for the specified Service
     * Reference.
     * 
     * @param reference the reference
     * @return an array of interface classes
     */
    private static Class<?>[] getServiceInterfaces(BundleContext context, ServiceReference<?> reference) {
        Set<Class<?>> serviceInterfaces = new HashSet<Class<?>>();
        Object serviceInstance = context.getService(reference);
        try {
            if (serviceInstance != null) {
                collectInterfaces(serviceInstance.getClass(), serviceInterfaces);
            }
        }
        finally {
            context.ungetService(reference);
        }
        return serviceInterfaces.toArray(new Class<?>[serviceInterfaces.size()]);
    }

    private static void collectInterfaces(Class<?> clazz, Set<Class<?>> accumulator) {
        for (Class<?> serviceInterface : clazz.getInterfaces()) {
            if (accumulator.add(serviceInterface)) {
                // Collect the inherited interfaces...
                collectInterfaces(serviceInterface, accumulator);
            }
        }
        // Go up in the hierarchy...
        Class<?> parent = clazz.getSuperclass();
        if (parent != null) {
            collectInterfaces(parent, accumulator);
        }
    }

    /**
     * Returns an array of interface classes by retaining the classes provided as the first argument if their
     * name is listed in the second argument.
     * 
     * @param interfaceClasses and array of classes
     * @param interfaceNames a list of class names
     * @return an array of classes
     */
    private static Class<?>[] getExportInterfaceClasses(Class<?>[] interfaceClasses, String[] interfaceNames) {
        Class<?>[] exportInterfaceClasses = new Class<?>[interfaceNames.length];
        for (int i = 0; i < interfaceNames.length; i++) {
            String interfaceName = interfaceNames[i];
            for (Class<?> interfaceClass : interfaceClasses) {
                if (interfaceClass.getName().equals(interfaceName)) {
                    exportInterfaceClasses[i] = interfaceClass;
                }
            }
            if (exportInterfaceClasses[i] == null) {
                throw new IllegalArgumentException("Service does not implement " + interfaceName);
            }
        }
        return exportInterfaceClasses;
    }

}
