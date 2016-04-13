package org.inaetics.certificateservice;

import org.amdatu.scheduling.annotations.RepeatForever;
import org.amdatu.scheduling.annotations.RepeatInterval;
import org.inaetics.truststorage.TrustStorageService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@RepeatForever
@RepeatInterval(period=RepeatInterval.SECOND, value=30)
public class CertificateJob implements Job  {
	
	private volatile TrustStorageService trustStorageService;
	
	@Override
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		CertificateServiceController.getInstance(trustStorageService).checkAndUpdateCertificates();
	}
	
	

}
