package org.inaetics.certificateservice;

import java.util.concurrent.ConcurrentHashMap;

import org.amdatu.scheduling.annotations.RepeatForever;
import org.amdatu.scheduling.annotations.RepeatInterval;
import org.inaetics.truststorage.TrustStorageService;
import org.osgi.framework.ServiceRegistration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@RepeatForever
@RepeatInterval(period=RepeatInterval.SECOND, value=30)
public class CertificateJob implements Job  {
	
	private volatile TrustStorageService trustStorageService;
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		// TODO Auto-generated method stub
		System.out.println("JOBBBB");
		CertificateServiceController.getInstance(trustStorageService).checkAndUpdateCertificates();
		
	}
	
	

}
