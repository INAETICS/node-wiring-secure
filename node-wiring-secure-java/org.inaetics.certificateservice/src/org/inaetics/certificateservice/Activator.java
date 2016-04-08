package org.inaetics.certificateservice;

import java.security.Security;
import java.util.Dictionary;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.inaetics.certificateservice.api.CertificateService;
import org.inaetics.truststorage.TrustStorageService;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.quartz.Job;

public class Activator extends DependencyActivatorBase implements ManagedService {

	/**
	 * Config key for the certificates CN.
	 */
	private final String POD_COMMONNAME_CONFIG_KEY = "org.inaetics.certificateservice.commonname";

	/**
	 * Default value for the certificates CN.
	 */
	private final String POD_DEFAULT_COMMONNAME = "localhost";

	private volatile BundleContext m_context;
	private volatile DependencyManager m_dependencyManager;
	
	@Override
	public void destroy(BundleContext bc, DependencyManager dm) throws Exception {
		// nothing in here yet.
	}

	@Override
	public void init(BundleContext bc, DependencyManager dm) throws Exception {
		m_context = bc;
		m_dependencyManager = dm;
		
		Security.addProvider(new BouncyCastleProvider());
		
		parseConfiguredNodeProperties(null);
		
		dm.add(createComponent().setInterface(CertificateService.class.getName(), null)
				.setImplementation(CertificateServiceImpl.class).add(createServiceDependency()
						.setService(TrustStorageService.class).setRequired(true)));
		dm.add(createComponent().setInterface(Job.class.getName(), null)
				.setImplementation(CertificateJob.class).add(createServiceDependency()
						.setService(TrustStorageService.class).setRequired(true)));
		
	}
	
	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
		parseConfiguredNodeProperties(properties);
	}

	private void parseConfiguredNodeProperties(Dictionary<String, ?> properties) throws ConfigurationException {
	    final String podCommonName = getConfigStringValue(m_context, POD_COMMONNAME_CONFIG_KEY, properties, POD_DEFAULT_COMMONNAME);
		// set the pod common name somewhere
	}

	private String getConfigStringValue(BundleContext context, String key, Dictionary<String, ?> properties,
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
}
