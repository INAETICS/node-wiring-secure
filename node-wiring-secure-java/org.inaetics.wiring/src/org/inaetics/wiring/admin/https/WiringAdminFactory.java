/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.certificateservice.api.CertificateService;
import org.inaetics.truststorage.TrustStorageService;
import org.inaetics.wiring.WiringAdmin;
import org.inaetics.wiring.base.AbstractComponent;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 * Factory for the Wiring Admin service implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringAdminFactory extends AbstractComponent implements ServiceFactory<WiringAdmin> {

    private final ConcurrentHashMap<Bundle, WiringAdminImpl> m_instances =
        new ConcurrentHashMap<Bundle, WiringAdminImpl>();

    private final HttpsAdminConfiguration m_configuration;
    
    private final WiringAdminListenerHandler m_wiringAdminListenerhandler;
    
    private final HttpsServerEndpointHandler m_serverEndpointHandler;
    private final HttpsClientEndpointFactory m_clientEndpointFactory;
    
    private volatile TrustStorageService m_trustStorageService;
    private volatile HttpService m_httpService;
    private volatile CertificateService m_certificateService;
    
    private volatile boolean m_started = false;

    public WiringAdminFactory(HttpsAdminConfiguration configuration) {
        super("admin ", "https");
        m_configuration = configuration;
        m_serverEndpointHandler = new HttpsServerEndpointHandler(this, m_configuration);
        m_clientEndpointFactory = new HttpsClientEndpointFactory(this, m_configuration);
        m_wiringAdminListenerhandler = new WiringAdminListenerHandler(this, m_configuration, m_serverEndpointHandler);
    }

    @Override
    protected void startComponent() throws Exception {
    	
    	if(m_started) return;
    	m_started = true;
        
    	super.startComponent();
    	m_clientEndpointFactory.setTrustStorageService(m_trustStorageService);
    	m_certificateService.forceTrustRefresh();
        m_serverEndpointHandler.start();
        m_clientEndpointFactory.start();
        m_wiringAdminListenerhandler.start();
        System.out.println(m_trustStorageService == null);
    }

    @Override
    protected void stopComponent() throws Exception {
    	
    	if(!m_started) return;
    	m_started = false;
    	
        m_serverEndpointHandler.stop();
        m_clientEndpointFactory.stop();
        m_wiringAdminListenerhandler.stop();

        super.stopComponent();
    }

    @Override
    public WiringAdmin getService(Bundle bundle, ServiceRegistration<WiringAdmin> registration) {

        WiringAdminImpl instance = new WiringAdminImpl(this, m_configuration);
        try {
            instance.start();
            WiringAdminImpl previous = m_instances.put(bundle, instance);
            assert previous == null; // framework should guard against this
            return instance;
        }
        catch (Exception e) {
            logError("Exception while instantiating admin instance!", e);
            return null;
        }
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<WiringAdmin> registration,
    		WiringAdmin service) {

        WiringAdminImpl instance = m_instances.remove(bundle);
        try {
            instance.stop();
        }
        catch (Exception e) {}
    }

    HttpService getHttpService() {
        return m_httpService;
    }

    HttpsServerEndpointHandler getServerEndpointHandler() {
        return m_serverEndpointHandler;
    }
    
    HttpsClientEndpointFactory getClientEndpointFactory() {
    	return m_clientEndpointFactory;
    }

    WiringAdminListenerHandler getWiringAdminListenerHandler() {
    	return m_wiringAdminListenerhandler;
    }
    
    TrustStorageService getTrustStorageService()
    {
    	System.out.println("getStrustService: " + m_trustStorageService == null );
    	return m_trustStorageService;
    }

}
