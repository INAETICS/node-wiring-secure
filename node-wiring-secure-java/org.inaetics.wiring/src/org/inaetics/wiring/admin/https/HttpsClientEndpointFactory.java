/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.truststorage.TrustStorageService;
import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.base.AbstractComponentDelegate;

/**
 * Provides a factory that creates a {@link HttpsClientEndpoint} for each bundle that is getting the endpoint.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * @author <a href="mailto:martin@gaida.eu">Martin Gaida</a>
 */
public class HttpsClientEndpointFactory extends AbstractComponentDelegate implements ClientEndpointProblemListener {

	private Map<String, HttpsClientEndpoint> m_clients =
			new ConcurrentHashMap<String, HttpsClientEndpoint>();
	
    private ClientEndpointProblemListener m_problemListener;
    private HttpsAdminConfiguration m_configuration;
    private volatile TrustStorageService trustService;
    
    public void setTrustStorageService(TrustStorageService trustStorage)

    {
    	this.trustService = trustStorage;
    }
    /**
     * Creates a new {@link HttpsClientEndpointFactory} instance.
     */
    public HttpsClientEndpointFactory(WiringAdminFactory factory, HttpsAdminConfiguration configuration) {
    	super(factory);
        m_configuration = configuration;
        trustService = factory.getTrustStorageService();
    }

    public WiringSenderImpl addEndpoint(WiringEndpointDescription endpoint) {
    	HttpsClientEndpoint client = m_clients.get(endpoint);
    	if (client == null) {
    		client = new HttpsClientEndpoint(endpoint, m_configuration,trustService);
    		m_clients.put(endpoint.getId(), client);
    		client.setProblemListener(this);
    	}
		return new WiringSenderImpl(this, m_configuration, endpoint);
    }
    
    public void removeEndpoint(WiringEndpointDescription endpoint) {
    	m_clients.remove(endpoint);
    }

    public String sendMessage(String wireId, String message) throws Exception {
		HttpsClientEndpoint httpClientEndpoint = m_clients.get(wireId);
		if (httpClientEndpoint == null) {
	    	throw new Exception("remote endpoint not found");
		}			
		return httpClientEndpoint.sendMessage(message);
    }
    
    @Override
    public synchronized void handleEndpointError(Throwable exception) {
        if (m_problemListener != null) {
            m_problemListener.handleEndpointError(exception);
        }
    }

    @Override
    public synchronized void handleEndpointWarning(Throwable exception) {
        if (m_problemListener != null) {
            m_problemListener.handleEndpointWarning(exception);
        }
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public synchronized void setProblemListener(ClientEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

}
