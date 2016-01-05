/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import static org.inaetics.remote.EndpointUtil.computeHash;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.CONFIGURATION_TYPE;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_ERROR;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_WARNING;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.inaetics.wiring.endpoint.WiringSender;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;

/**
 * The {@link ImportedEndpointImpl} class represents an active imported endpoint for a
 * unique {@link EndpointDescription}. It manages the client endpoint lifecycle and
 * serves as the {@link ImportRegistration} and {@link ImportReference}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class ImportedEndpointImpl implements ImportRegistration, ImportReference, ClientEndpointProblemListener {

    private final AtomicBoolean m_closed = new AtomicBoolean(false);
    private final RemoteServiceAdminImpl m_admin;

    private volatile EndpointDescription m_endpoint;
    private volatile String m_endpointHash;
    private volatile WiringClientEndpointFactory m_clientEndpoint;
    private volatile ServiceRegistration<?> m_clientRegistration;
    private volatile ServiceReference<?> m_clientReference;

    private volatile Throwable m_exception;

    /**
     * Constructs an {@link ImportedEndpointImpl} and registers the client endpoint. Any input validation
     * should have been done. Exceptions that occur during construction or registration result in an invalid
     * import registration and are therefore accessible through {@link #getException()}.
     * 
     * @param admin the admin instance
     * @param endpoint the description
     * @param wiringSender 
     */
    public ImportedEndpointImpl(RemoteServiceAdminImpl admin, EndpointDescription endpoint, WiringSender wiringSender) {

    	if (admin == null) {
    		throw new IllegalArgumentException("admin must not be null!");
    	}
    	if (endpoint == null) {
    		throw new IllegalArgumentException("endpoint must not be null!");
    	}
    	if (wiringSender == null) {
    		throw new IllegalArgumentException("wiringSender must not be null!");
    	}
    	
        m_admin = admin;
        m_endpoint = endpoint;
        m_endpointHash = computeHash(endpoint);

        try {
            m_clientEndpoint = new WiringClientEndpointFactory(endpoint, wiringSender);
            m_clientEndpoint.setProblemListener(this);

            String[] objectClass = createImportedServiceObjectClass(m_endpoint);
            Dictionary<String, Object> serviceProperties = createImportedServiceProperties(m_endpoint);
            m_clientRegistration =
                m_admin.getBundleContext().registerService(objectClass, m_clientEndpoint, serviceProperties);
            if (m_clientRegistration != null) {
                m_clientReference = m_clientRegistration.getReference();
            }
        }
        catch (Exception e) {
            m_exception = e;
        }
    }

    @Override
    public void handleEndpointError(Throwable exception) {
        m_admin.getEventsHandler().emitEvent(IMPORT_ERROR, m_admin.getBundleContext().getBundle(), this, exception);
    }

    @Override
    public void handleEndpointWarning(Throwable exception) {
        m_admin.getEventsHandler().emitEvent(IMPORT_WARNING, m_admin.getBundleContext().getBundle(), this, exception);
    }

    @Override
    public ImportReference getImportReference() {
        if (m_closed.get()) {
            return null;
        }
        if (m_exception != null) {
            throw new IllegalStateException("Endpoint registration is failed. See #getException()");
        }
        return this;
    }

    @Override
    public Throwable getException() {
        return getException(false);
    }

    @Override
    public boolean update(EndpointDescription endpoint) {
        if (m_closed.get()) {
            throw new IllegalStateException("Updating closed Import Registration not supported");
        }
        if (m_exception != null) {
            throw new IllegalStateException("Updating invalid Import Registration not allowed");
        }
        if (!endpoint.equals(m_endpoint)) {
            throw new IllegalArgumentException(
                "Updating Import Registation with different service instance not allowed");
        }

        List<String> configurationTypes = endpoint.getConfigurationTypes();
        if (!configurationTypes.contains(CONFIGURATION_TYPE)) {
            // TODO setexception
            return false;
        }

        String updateHash = computeHash(endpoint);
        if (!updateHash.equals(m_endpointHash)) {

            m_endpoint = endpoint;
            m_endpointHash = updateHash;

            Dictionary<String, Object> serviceProperties = createImportedServiceProperties(m_endpoint);
            m_clientRegistration.setProperties(serviceProperties);

            m_admin.importedEndpointUpdated(this);
        }
        return true;
    }

    @Override
    public void close() {
        if (!m_closed.compareAndSet(false, true)) {
            return;
        }
        if (m_clientRegistration != null) {
            m_clientRegistration.unregister();
            m_clientRegistration = null;
        }
        m_clientReference = null;
        m_admin.importedEndpointClosed(this);
    }

    @Override
    public ServiceReference<?> getImportedService() {
        return getExportedService(false);
    }

    @Override
    public EndpointDescription getImportedEndpoint() {
        return getImportedEndpoint(false);
    }

    EndpointDescription getImportedEndpoint(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_endpoint;
    }

    ServiceReference<?> getExportedService(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_clientReference;
    }

    Throwable getException(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_exception;
    }

    /**
     * Create an objectClass value from an Endpoint Description.
     * 
     * @param description
     * @return the objectClass
     */
    private static String[] createImportedServiceObjectClass(EndpointDescription description) {
        List<String> ifaceList = description.getInterfaces();
        return ifaceList.toArray(new String[ifaceList.size()]);
    }

    /**
     * Create an service properties value from an Endpoint Description.
     * 
     * @param endpointDescription
     * @return the properties
     */
    private static Dictionary<String, Object> createImportedServiceProperties(EndpointDescription endpointDescription) {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_IMPORTED, endpointDescription.getId());
        for (String key : endpointDescription.getProperties().keySet()) {
            serviceProperties.put(key, endpointDescription.getProperties().get(key));
        }
        return serviceProperties;
    }
}
