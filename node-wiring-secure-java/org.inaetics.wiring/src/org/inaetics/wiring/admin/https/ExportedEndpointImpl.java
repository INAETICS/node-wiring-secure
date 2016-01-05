/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import org.inaetics.wiring.ExportReference;
import org.inaetics.wiring.ExportRegistration;
import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.endpoint.WiringConstants;
import org.inaetics.wiring.endpoint.WiringReceiver;

/**
 * The {@link ExportedEndpointImpl} class represents an active exported endpoint for a
 * unique {@link WiringEndpointDescription}. It manages the server endpoint lifecycle and
 * serves as the {@link ExportRegistration} and {@link ExportReference}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class ExportedEndpointImpl implements ExportRegistration, ExportReference {

    private final AtomicBoolean m_closed = new AtomicBoolean(false);
    
    private final HttpsServerEndpointHandler m_endpointHandler;

    private volatile WiringEndpointDescription m_endpointDescription;
    private volatile WiringReceiver m_receiver;
    private volatile Throwable m_exception;
	private volatile HttpsAdminConfiguration m_configuration;

    /**
     * Constructs an {@link ExportRegistrationImpl} and registers the server endpoint. Any input validation
     * should have been done. Exceptions that occur during construction or registration result in an invalid
     * export registration and are therefore accessible through {@link #getException()}.
     * 
     * @param admin the admin instance
     * @param description the description
     * @param reference the service reference
     * @param properties the export properties
     */
    public ExportedEndpointImpl(HttpsServerEndpointHandler endpointHandler, WiringReceiver receiver,
    		HttpsAdminConfiguration configuration) {

        m_endpointHandler = endpointHandler;
        m_receiver = receiver;
        m_configuration = configuration;

        try {

    		// create new endpoint description
    		m_endpointDescription = new WiringEndpointDescription();
    		m_endpointDescription.setZone(m_configuration.getZone());
    		m_endpointDescription.setNode(m_configuration.getNode());
    		m_endpointDescription.setProtocolName(HttpsAdminConstants.PROTOCOL_NAME);
 
    		m_endpointDescription.setProperty(HttpsWiringEndpointProperties.VERSION, HttpsAdminConstants.PROTOCOL_VERSION);
    		m_endpointDescription.setProperty(WiringConstants.PROPERTY_SECURE, HttpsAdminConstants.SECURE);
    		
    		try {
    			m_endpointDescription.setProperty(HttpsWiringEndpointProperties.URL, new URL(m_configuration.getBaseUrl().toString() + m_endpointDescription.getId()).toString());
    		} catch (MalformedURLException e) {
    			m_exception = e;
    			return;
    		}
    		
    		// create http handler
    		m_endpointHandler.addEndpoint(m_endpointDescription, m_receiver);
        	
        }
        catch (Exception e) {
            m_exception = e;
        }
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
    public void close() {
        if (!m_closed.compareAndSet(false, true)) {
            return;
        }
        if (m_endpointDescription != null) {

        	m_endpointHandler.removeEndpoint(m_endpointDescription);
        
        }
    }

    @Override
    public Throwable getException() {
        return getException(false);
    }

    @Override
    public WiringReceiver getWiringReceiver() {
        return getWiringReceiver(false);
    }

    @Override
    public WiringEndpointDescription getEndpointDescription() {
        return getEndpointDescription(false);
    }

    WiringEndpointDescription getEndpointDescription(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_endpointDescription;
    }

    WiringReceiver getWiringReceiver(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_receiver;
    }

    Throwable getException(boolean ignoreClosed) {
        if (!ignoreClosed && m_closed.get()) {
            return null;
        }
        return m_exception;
    }
}
