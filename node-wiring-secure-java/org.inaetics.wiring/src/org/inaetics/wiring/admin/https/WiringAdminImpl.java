/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

import java.util.HashSet;
import java.util.Set;

import org.inaetics.wiring.ExportRegistration;
import org.inaetics.wiring.ImportRegistration;
import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.WiringAdmin;
import org.inaetics.wiring.base.AbstractComponentDelegate;
import org.inaetics.wiring.endpoint.WiringReceiver;

/**
 * Wiring Admin instance implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringAdminImpl extends AbstractComponentDelegate implements WiringAdmin {

    private final Set<ExportedEndpointImpl> m_exportedEndpoints =
        new HashSet<ExportedEndpointImpl>();

    private final Set<ImportedEndpointImpl> m_importedEndpoints =
        new HashSet<ImportedEndpointImpl>();

    private final WiringAdminFactory m_manager;
    private final HttpsAdminConfiguration m_configuration;

    public WiringAdminImpl(WiringAdminFactory manager, HttpsAdminConfiguration configuration) {
        super(manager);
        m_manager = manager;
        m_configuration = configuration;
    }

    @Override
    protected void startComponentDelegate() throws Exception {
    }

    @Override
    protected void stopComponentDelegate() throws Exception {

    	for (ExportedEndpointImpl exportedEndpointImpl : m_exportedEndpoints) {
			exportedEndpointImpl.close();
		}
    	for (ImportedEndpointImpl importedEndpointImpl : m_importedEndpoints) {
			importedEndpointImpl.close();
		}
    	m_exportedEndpoints.clear();
    	m_importedEndpoints.clear();
    	
    }

	@Override
	public ExportRegistration exportEndpoint(WiringReceiver listener) {
		ExportedEndpointImpl endpointImpl = new ExportedEndpointImpl(m_manager.getServerEndpointHandler(), listener, m_configuration);
		m_exportedEndpoints.add(endpointImpl);
		return endpointImpl;
	}

	@Override
	public ImportRegistration importEndpoint(WiringEndpointDescription endpoint) {
		
		// check protocol
		if (!endpoint.getProtocolName().equals(HttpsAdminConstants.PROTOCOL_NAME)
				|| (endpoint.getProperty(HttpsWiringEndpointProperties.VERSION) != null && !endpoint.getProperty(HttpsWiringEndpointProperties.VERSION).equals(HttpsAdminConstants.PROTOCOL_VERSION))) {
			logWarning("protocol not supported: %s %s", endpoint.getProtocolName(), endpoint.getProperty(HttpsWiringEndpointProperties.VERSION));
			return null;
		}
		
		ImportedEndpointImpl endpointImpl = new ImportedEndpointImpl(m_manager.getClientEndpointFactory(), endpoint, m_configuration);
		m_importedEndpoints.add(endpointImpl);
		return endpointImpl;
	}

}
