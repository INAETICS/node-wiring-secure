package org.inaetics.certificateservice.api;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * A Service to handle all CA related functionality.
 * 
 * @author <a href="mailto:contact@inaetics.org">Inaetics Project Secure Wiring Team</a>
 */
public interface CertificateService {
	/**
	 * Verifies that this certificate was signed using the private key that corresponds to the specified public key.
	 * Also all other verifycation like validity and if the certificate was revoked is executed here.
	 * @throws Exception TODO
	 */
	// TODO change exceptions to match the correct ones.
	public void verifyCertificate(X509Certificate certificate) throws Exception;
	
	/**
	 * Verify your own certificate to ensure you will be able to connect.
	 * @param String alias the alias of the cert that you want to check.
	 * @throws Exception TODO
	 */
	public void verifyOwnCertificate(String alias) throws Exception;
	
	/**
	 * Enforce a trust refresh to be able to communicate again. 
	 * @throws Exception TODO
	 */
	public void forceTrustRefresh() throws Exception;
}
