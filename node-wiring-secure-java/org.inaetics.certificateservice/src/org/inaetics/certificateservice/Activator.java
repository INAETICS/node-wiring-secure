package org.inaetics.certificateservice;

import java.security.Security;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.inaetics.certificateservice.api.CertificateService;
import org.inaetics.truststorage.TrustStorageService;
import org.osgi.framework.BundleContext;
import org.quartz.Job;

public class Activator extends DependencyActivatorBase {

	@Override
	public void destroy(BundleContext bc, DependencyManager dm) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(BundleContext bc, DependencyManager dm) throws Exception {
		// TODO Auto-generated method stub
		Security.addProvider(new BouncyCastleProvider());
		
		dm.add(createComponent().setInterface(CertificateService.class.getName(), null)
				.setImplementation(CertificateServiceImpl.class).add(createServiceDependency()
						.setService(TrustStorageService.class).setRequired(true)));
		dm.add(createComponent().setInterface(Job.class.getName(), null)
				.setImplementation(CertificateJob.class).add(createServiceDependency()
						.setService(TrustStorageService.class).setRequired(true)));
		
	}

}
