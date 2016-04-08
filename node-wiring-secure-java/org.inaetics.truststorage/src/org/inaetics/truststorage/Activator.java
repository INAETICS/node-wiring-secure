/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.truststorage;

import static org.inaetics.truststorage.TrustStorageConstants.CONFIG_KEYSTORE_FILE_NAME;
import static org.inaetics.truststorage.TrustStorageConstants.CONFIG_KEYSTORE_KEY_PASSWORD;
import static org.inaetics.truststorage.TrustStorageConstants.CONFIG_KEYSTORE_PASSWORD;
import static org.inaetics.truststorage.TrustStorageConstants.CONFIG_KEYSTORE_TYPE;
import static org.inaetics.truststorage.TrustStorageConstants.CONFIG_TRUSTSTORE_FILE_NAME;
import static org.inaetics.truststorage.TrustStorageConstants.CONFIG_TRUSTSTORE_PASSWORD;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;

public class Activator extends DependencyActivatorBase implements TrustStorageConfiguration {
	
	private volatile BundleContext m_context;
	private volatile DependencyManager m_dependencymanager;
	
	private volatile String m_keystore_type;
	private volatile String m_keystore_file_name;
	private volatile String m_keystore_password;
	private volatile String m_keystore_key_password;
	private volatile String m_truststore_file_name;
	private volatile String m_truststore_password;

	@Override
	public void init(BundleContext context, DependencyManager dm) throws Exception {
		
		m_context = context;
		m_dependencymanager = dm;
		
		try{
			m_keystore_type = getConfiguredKeyStoreType(null);
			m_keystore_file_name = getConfiguredKeyStoreFileName(null);
			m_keystore_password = getConfiguredKeyStorePassword(null);
			m_keystore_key_password = getConfiguredKeyStoreKeyPassword(null);
			m_truststore_file_name = getConfiguredTrustStoreFileName(null);
			m_truststore_password = getConfiguredTrustStorePassword(null);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		
		registerTrustStorageService();
	}
	
	@Override
	public void destroy(BundleContext arg0, DependencyManager arg1) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private String getConfiguredKeyStoreType(Dictionary<String, ?> properties) throws ConfigurationException {
		return getConfigStringValue(m_context, CONFIG_KEYSTORE_TYPE, properties, "JKS");
	}
	
	private String getConfiguredKeyStoreFileName(Dictionary<String, ?> properties) throws ConfigurationException {
		return getConfigStringValue(m_context, CONFIG_KEYSTORE_FILE_NAME, properties, "/tmp/in-keys/client/inaetics.keystore");
	}

	private String getConfiguredKeyStorePassword(Dictionary<String, ?> properties) throws ConfigurationException {
		return getConfigStringValue(m_context, CONFIG_KEYSTORE_PASSWORD, properties, "changeit");
	}

	private String getConfiguredKeyStoreKeyPassword(Dictionary<String, ?> properties) throws ConfigurationException {
		return getConfigStringValue(m_context, CONFIG_KEYSTORE_KEY_PASSWORD, properties, "changeit");
	}
	
	private String getConfiguredTrustStoreFileName(Dictionary<String, ?> properties) throws ConfigurationException {
		return getConfigStringValue(m_context, CONFIG_TRUSTSTORE_FILE_NAME, properties, "/tmp/in-keys/client/inaetics.truststore");
	}

	private String getConfiguredTrustStorePassword(Dictionary<String, ?> properties) throws ConfigurationException {
		return getConfigStringValue(m_context, CONFIG_TRUSTSTORE_PASSWORD, properties, "changeit");
	}

	@Override
	public String getKeyStoreType() {
		return m_keystore_type;
	}

	@Override
	public String getKeyStoreFileName() {
		return m_keystore_file_name;
	}

	@Override
	public String getKeyStorePassword() {
		return m_keystore_password;
	}

	@Override
	public String getKeyStoreKeyPassword() {
		return m_keystore_key_password;
	}
	
	@Override
	public String getTrustStoreFileName() {
		return m_truststore_file_name;
	}

	@Override
	public String getTrustStorePassword() {
		return m_truststore_password;
	}
	
	private static String getConfigStringValue(BundleContext context, String key, Dictionary<String, ?> properties,
        String defaultValue) throws ConfigurationException {

        String value = null;
        if (properties != null && properties.get(key) != null) {
            value = properties.get(key).toString();
        }
        if (context != null && value == null) {
            value = context.getProperty(key);
        }
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
	
	private void registerTrustStorageService() {
		TrustStorageServiceImpl serviceImpl = new TrustStorageServiceImpl(this);
		

		Properties props = new Properties();
		m_dependencymanager.add(createComponent()
				.setInterface(TrustStorageService.class.getName(), props)
				.setImplementation(serviceImpl));
	}
}
