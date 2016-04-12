/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

import static org.inaetics.wiring.admin.https.HttpsAdminConstants.CONFIG_KEYSTORE_FILE_NAME;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.CONFIG_KEYSTORE_KEY_PASSWORD;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.CONFIG_KEYSTORE_PASSWORD;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.CONFIG_TRUSTSTORE_FILE_NAME;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.CONFIG_TRUSTSTORE_PASSWORD;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.CONFIG_TRUSTSTORE_TYPE;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.CLIENT_CERT_ENFORCE_KEY;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.CONNECT_TIMEOUT_CONFIG_KEY;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.NODE_CONFIG_KEY;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.PATH_CONFIG_KEY;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.PROTOCOL_NAME;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.PROTOCOL_VERSION;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.READ_TIMEOUT_CONFIG_KEY;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.SERVICE_PID;
import static org.inaetics.wiring.admin.https.HttpsAdminConstants.ZONE_CONFIG_KEY;
import static org.inaetics.wiring.base.ServiceUtil.getConfigIntValue;
import static org.inaetics.wiring.base.ServiceUtil.getConfigStringValue;
import static org.inaetics.wiring.base.ServiceUtil.getConfigBoolValue;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.jetty.ConnectorFactory;
import org.inaetics.certificateservice.api.CertificateService;
import org.inaetics.truststorage.TrustStorageService;
import org.inaetics.wiring.WiringAdmin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

/**
 * Activator and configuration manager for the Amdatu HTTP Wiring Admin service implementation.
 * <p>
 * Configuration can be provided through cm as well as system properties. The former take precedence and
 * in addition some fallbacks and defaults are provided. See {@link HttpsAdminConstants} for supported
 * configuration properties.
 * <p>
 * Note that any effective configuration change will close all existing import- and export registrations.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class Activator extends DependencyActivatorBase implements ManagedService, HttpsAdminConfiguration {
   
	private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 60000;
    private static final int DEFAULT_SECURE_PORT = 8443;

    private volatile BundleContext m_context;
    private volatile DependencyManager m_dependencyManager;

    private volatile Component m_configurationComponent;
    private volatile Component m_adminComponent;
    private volatile Component m_listenerComponent;
    private volatile Component m_wiringConnectorFactory;
    
    private volatile URL m_baseUrl;
    private volatile int m_securePort;
    private volatile int m_connectTimeout;
    private volatile int m_readTimeout;
    private volatile boolean m_clientCertValidation;
    private volatile String m_zone;
    private volatile String m_node;
    
    private volatile String m_truststore_file_name;
    private volatile String m_truststore_password;
    private volatile String m_truststore_type;
    
    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {    	
    	m_context = context;
    	m_dependencyManager = manager;

        int connectTimeout = getConfigIntValue(context, CONNECT_TIMEOUT_CONFIG_KEY, null, DEFAULT_CONNECT_TIMEOUT);
        int readTimeout = getConfigIntValue(context, READ_TIMEOUT_CONFIG_KEY, null, DEFAULT_READ_TIMEOUT);
        String zone = getConfiguredZone(null);
        String node = getConfiguredNode(null);
        String truststoreFileName = getConfiguredTruststoreFileName(null);
        String truststorePassword = getConfiguredTruststorePassword(null);
        String truststoreType = getConfiguredTruststoreType(null);
        int securePort = getConfigIntValue(m_context, HttpsAdminConstants.PORT_CONFIG_KEY, null, DEFAULT_SECURE_PORT);
        boolean enforceClientCert = getConfiguredClientCertValidation(null);
        
        try {
            m_baseUrl = parseConfiguredBaseUrl(null);
            m_connectTimeout = connectTimeout;
            m_readTimeout = readTimeout;
            m_zone = zone;
            m_node = node;
            m_truststore_file_name = truststoreFileName;
            m_truststore_password = truststorePassword;
            m_truststore_type = truststoreType;
            m_securePort = securePort;
            m_clientCertValidation = enforceClientCert;
            registerFactoryService();
            registerConfigurationService();
            registerWiringConnectorFactory();
        }
        catch (Exception e) {
            throw new ConfigurationException("base url", "invalid url", e);
        }
    }

    @Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		// this enforcement filter is required to make the discovery available via standard http ( not encrypted )
		ServiceReference sRef = context.getServiceReference(ExtHttpService.class.getName());

		if (sRef != null) {
		Dictionary properties = new Hashtable();
		    properties.put("service.pid", ClientCertificateEnforcementFilter.class.getName());
		    properties.put("init.charencoding.filter.encoding", "UTF-8");
		    properties.put("init.charencoding.filter.forceencoding", "true");
		    ExtHttpService service = (ExtHttpService) context.getService(sRef);
		    service.registerFilter(new ClientCertificateEnforcementFilter(), "/.*", properties, 0, null);
		}
	}

	@Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        unregisterConfigurationService();
        unregisterWiringConnectorFactory();
        unregisterFactoryService();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        
        // first parse timeout to local variables, in order to make this method "transactional"
        // assign values to fields after baseUrl was successfully
        int connectTimeout = getConfigIntValue(m_context, CONNECT_TIMEOUT_CONFIG_KEY, properties, DEFAULT_CONNECT_TIMEOUT);
        int readTimeout = getConfigIntValue(m_context, READ_TIMEOUT_CONFIG_KEY, properties, DEFAULT_READ_TIMEOUT);
        String zone = getConfiguredZone(properties);
        String node = getConfiguredNode(properties);
        String truststoreFileName = getConfiguredTruststoreFileName(properties);
        String truststorePassword = getConfiguredTruststorePassword(properties);
        String truststoreType = getConfiguredTruststoreType(properties);
        
        URL baseUrl = parseConfiguredBaseUrl(properties);

        try {
            m_connectTimeout = connectTimeout;
            m_readTimeout = readTimeout;
            m_zone = zone;
            m_node = node;
            m_truststore_file_name = truststoreFileName;
            m_truststore_password = truststorePassword;
            m_truststore_type = truststoreType;
            m_securePort = getConfigIntValue(m_context, HttpsAdminConstants.PORT_CONFIG_KEY, properties, DEFAULT_SECURE_PORT);
            m_clientCertValidation = getConfiguredClientCertValidation(properties);
            
            if (!baseUrl.equals(m_baseUrl)) {
                m_baseUrl = baseUrl;

                unregisterWiringConnectorFactory();
                unregisterFactoryService();
                Thread.sleep(100);
                registerFactoryService();
                registerWiringConnectorFactory();
            }
        }
        catch (Exception e) {
            throw new ConfigurationException("base url", "invalid url", e);
        }
    }

    private void registerConfigurationService() {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, HttpsAdminConstants.SERVICE_PID);

        Component component = createComponent()
            .setInterface(ManagedService.class.getName(), properties)
            .setImplementation(this)
            .setAutoConfig(DependencyManager.class, false)
            .setAutoConfig(Component.class, false);

        m_configurationComponent = component;
        m_dependencyManager.add(component);
    }

    private void unregisterConfigurationService() {
        Component component = m_configurationComponent;
        m_configurationComponent = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }
    }
    
    private void registerWiringConnectorFactory() {
    	Component component = createComponent()
				.setInterface(ConnectorFactory.class.getName(), null).setImplementation(new WiringConnectorFactory(this))   	
				.add(createServiceDependency().setService(TrustStorageService.class).setRequired(true))
				.add(createServiceDependency().setService(LogService.class).setRequired(false))
				.add(createServiceDependency().setService(CertificateService.class).setRequired(true));
    	
    	m_wiringConnectorFactory = component;
    	m_dependencyManager.add(component);
    }
    
    private void unregisterWiringConnectorFactory() {
        Component component = m_wiringConnectorFactory;
        m_wiringConnectorFactory = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }
    }

	private void registerFactoryService() {

		WiringAdminFactory factory = new WiringAdminFactory(this);

		Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(HttpsAdminConstants.ADMIN, true);
        properties.put(HttpsAdminConstants.ADMIN_TYPE, PROTOCOL_NAME + ";" + PROTOCOL_VERSION);

		Component listenerComponent = createComponent()
				.setInterface(WiringAdmin.class.getName(), properties)
				.setImplementation(factory)
				.add(createServiceDependency().setService(HttpService.class).setRequired(true))
				.add(createServiceDependency().setService(TrustStorageService.class).setRequired(true))
				.add(createServiceDependency().setService(LogService.class).setRequired(false))
				.add(createServiceDependency().setService(CertificateService.class).setRequired(true));

		m_listenerComponent = listenerComponent;
		m_dependencyManager.add(listenerComponent);

	}

    private void unregisterFactoryService() {
        Component component = m_adminComponent;
        m_adminComponent = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }

        component = m_listenerComponent;
        m_listenerComponent = null;
        if (component != null) {
            m_dependencyManager.remove(component);
        }
    
    }

    private URL parseConfiguredBaseUrl(Dictionary<String, ?> properties) throws ConfigurationException {
        String host = getConfigStringValue(m_context, HttpsAdminConstants.HOST_CONFIG_KEY, properties, null);
        if (host == null) {
            host = getConfigStringValue(m_context, "org.apache.felix.http.host", properties, "localhost");
        }

        int port = getConfigIntValue(m_context, HttpsAdminConstants.PORT_CONFIG_KEY, properties, -1);
        if (port == -1) {
            port = getConfigIntValue(m_context, HttpsAdminConstants.PORT_CONFIG_KEY, properties, 8443);
        }

        String path = getConfigStringValue(m_context, PATH_CONFIG_KEY, properties, SERVICE_PID);
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        try {
            return new URL("https", host, port, path);
        }
        catch (Exception e) {
            throw new ConfigurationException("unknown", e.getMessage(), e);
        }
    }

    private String getConfiguredZone(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, ZONE_CONFIG_KEY, properties, "");
    }

    private String getConfiguredNode(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, NODE_CONFIG_KEY, properties, "");
    }
    
    private String getConfiguredKeystoreFileName(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_KEYSTORE_FILE_NAME, properties, "inaetics.keystore");
    }
	
    private String getConfiguredKeystorePassword(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_KEYSTORE_PASSWORD, properties, "changeit");
    }

    private String getConfiguredKeystoreKeyPassword(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_KEYSTORE_KEY_PASSWORD, properties, "changeit");
    }
    
    private String getConfiguredTruststoreFileName(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_TRUSTSTORE_FILE_NAME, properties, "inaetics.truststore");
    }
	
    private String getConfiguredTruststorePassword(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_TRUSTSTORE_PASSWORD, properties, "changeit");
    }

    private String getConfiguredTruststoreType(Dictionary<String, ?> properties) throws ConfigurationException {
        return getConfigStringValue(m_context, CONFIG_TRUSTSTORE_TYPE, properties, "JKS");
    }
    
    private boolean getConfiguredClientCertValidation(Dictionary<String, ?> properties) throws ConfigurationException {
    	return getConfigBoolValue(m_context, CLIENT_CERT_ENFORCE_KEY, properties, true);
    }
    
    @Override
    public int getSecurePort() {
    	return m_securePort;
    }
    
    @Override
    public boolean shouldEenforceClientCertValidation() {
    	return m_clientCertValidation;
    }
    
    @Override
    public URL getBaseUrl() {
        return m_baseUrl;
    }
    
    @Override
    public int getConnectTimeout() {
        return m_connectTimeout;
    }

    @Override
    public int getReadTimeout() {
        return m_readTimeout;
    }

	@Override
	public String getZone() {
		return m_zone;
	}

    @Override
    public String getNode() {
        return m_node;
    }

	@Override
	public String getTruststoreFileName() {
		return m_truststore_file_name;
	}

	@Override
	public String getTruststorePassword() {
		return m_truststore_password;
	}

	@Override
	public String getTruststoreType() {
		return m_truststore_type;
	}
}

