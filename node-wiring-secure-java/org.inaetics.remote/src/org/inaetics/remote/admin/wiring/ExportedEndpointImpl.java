/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import static org.inaetics.remote.EndpointUtil.computeHash;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_ERROR;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_WARNING;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

/**
 * The {@link ExportedEndpointImpl} class represents an active exported endpoint for a
 * unique {@link EndpointDescription}. It manages the server endpoint lifecycle and
 * serves as the {@link ExportRegistration} and {@link ExportReference}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class ExportedEndpointImpl implements ExportRegistration, ExportReference, ServerEndpointProblemListener {

    private final AtomicBoolean m_closed = new AtomicBoolean(false);
    private final RemoteServiceAdminImpl m_admin;
    private final ServiceReference<?> m_reference;

    private volatile EndpointDescription m_endpoint;
    private volatile String m_endpointHash;
    private volatile Throwable m_exception;
    private volatile Map<String, ?> m_properties; // original export properties
    private volatile WiringServerEndpoint m_serverEndpoint;
    private EndpointDescriptionBuilder m_endpointBuilder;

    /**
     * Constructs an {@link ExportRegistrationImpl} and registers the server endpoint. Any input validation
     * should have been done. Exceptions that occur during construction or registration result in an invalid
     * export registration and are therefore accessible through {@link #getException()}.
     * 
     * @param admin the admin instance
     * @param reference the service reference
     * @param properties the export properties
     * @param m_endpointBuilder 
     */
    public ExportedEndpointImpl(RemoteServiceAdminImpl admin, ServiceReference<?> reference,
    		Map<String, ?> properties, EndpointDescriptionBuilder endpointBuilder) {

        m_admin = admin;
        m_reference = reference;
        m_endpointBuilder = endpointBuilder;
        
        Map<String, String> extraProperties = new HashMap<String, String>();
        String endpointId = UUID.randomUUID().toString();

        try {
            m_serverEndpoint = m_admin.getServerEndpointHandler().addEndpoint(admin, this, m_reference, properties, extraProperties, endpointId);
            m_serverEndpoint.setProblemListener(this);
        }
        catch (Exception e) {
            m_exception = e;
        }
        
        EndpointDescription description = endpointBuilder.createEndpointDescription(endpointId, reference, properties, extraProperties);
        
        if (description == null) {
        	m_admin.getServerEndpointHandler().removeEndpoint(endpointId);
        	m_exception = new IllegalStateException("error creating endpoint description");
        }
        else {
        	m_endpoint = description;
        	m_endpointHash = computeHash(description);
        }        
    }

    @Override
    public void handleEndpointError(Throwable exception) {
        m_admin.getEventsHandler().emitEvent(EXPORT_ERROR, m_admin.getBundleContext().getBundle(), this, exception);
    }

    @Override
    public void handleEndpointWarning(Throwable exception) {
        m_admin.getEventsHandler().emitEvent(EXPORT_WARNING, m_admin.getBundleContext().getBundle(), this, exception);
    }

    @Override
    public ExportReference getExportReference() {
        if (m_closed.get()) {
            return null;
        }
        if (m_exception != null) {
            throw new IllegalStateException("Endpoint registration is failed. See #getException()");
        }
        return this;
    }

    @Override
    public EndpointDescription update(Map<String, ?> properties) {
        if (m_closed.get()) {
            throw new IllegalStateException("Updating closed Export Registration not supported");
        }
        if (m_exception != null) {
            throw new IllegalStateException("Updating invalid Export Registration not allowed");
        }
        if (properties != null) {
            m_properties = properties;
        }

        EndpointDescription updateDescription =
            m_endpointBuilder.createEndpointDescription(m_endpoint.getId(), m_reference, m_properties, null);
        if (updateDescription == null) {
            // TODO set exception?
            return null;
        }

        String updateHash = computeHash(updateDescription);
        if (!updateDescription.equals(m_endpointHash)) {
            m_endpoint = updateDescription;
            m_endpointHash = updateHash;
            // TODO m_endpoint#update()
            m_admin.exportedEndpointUpdated(this);
        }

        return m_endpoint;
    }

    @Override
    public void close() {
        if (!m_closed.compareAndSet(false, true)) {
            return;
        }
        if (m_serverEndpoint != null) {
            m_admin.getServerEndpointHandler().removeEndpoint(m_endpoint);
            m_serverEndpoint.close();
            m_serverEndpoint = null;
        }
        m_admin.exportedEndpointClosed(this);
    }

    @Override
    public Throwable getException() {
        return getException(false);
    }

    @Override
    public ServiceReference<?> getExportedService() {
        return getExportedService(false);
    }

    @Override
    public EndpointDescription getExportedEndpoint() {
        return getExportedEndpoint(false);
    }

    EndpointDescription getExportedEndpoint(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_endpoint;
    }

    ServiceReference<?> getExportedService(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_reference;
    }

    Throwable getException(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_exception;
    }
}
