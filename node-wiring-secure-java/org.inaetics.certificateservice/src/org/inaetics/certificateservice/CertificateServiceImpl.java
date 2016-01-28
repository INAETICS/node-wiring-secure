package org.inaetics.certificateservice;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.inaetics.certificateservice.api.CertificateService;
import org.inaetics.truststorage.TrustStorageService;

public class CertificateServiceImpl implements CertificateService {

	private volatile TrustStorageService trustStorageService;
	
	@Override
	public void verifyCertificate(X509Certificate certificate) throws InvalidKeyException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
		// TODO Auto-generated method stub
		PublicKey pubkey = trustStorageService.getRootCaCert().getPublicKey();
		certificate.verify(pubkey);
		certificate.checkValidity();

	}

	@Override
	public void verifyOwnCertificate(String alias) throws Exception {
		// TODO Auto-generated method stub
		X509Certificate certificate = (X509Certificate) trustStorageService.getKeyStore().getCertificate(alias);
		verifyCertificate(certificate);

	}

	@Override
	public void forceTrustRefresh() throws Exception {
		// TODO Auto-generated method stub
		CertificateServiceController.getInstance(trustStorageService).getRootCaCertificate();
		CertificateServiceController.getInstance(trustStorageService).generateKeyAndSign();

	}
	

}
