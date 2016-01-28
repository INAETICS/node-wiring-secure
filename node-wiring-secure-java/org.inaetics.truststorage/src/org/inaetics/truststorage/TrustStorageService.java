package org.inaetics.truststorage;

import java.security.KeyStore;
import java.security.PrivateKey;

import java.security.cert.X509Certificate;

public interface TrustStorageService {
	
	// getter
	
	/**
	 * Get the truststore from filesystem
	 * @return the truststore
	 */
	public KeyStore getTrustStore();
	
	/**
	 * Get the keystore from filesystem
	 * @return the keystore
	 */
	public KeyStore getKeyStore();
	
	/**
	 * Get the current root CA certificate from truststore
	 * @return rootCaCert
	 */
	public X509Certificate getRootCaCert();

	
	// setter
	
	/**
	 * Store the CA's certificate in the truststore
	 * @param cert the CA's certificate
	 */
	public void storeRootCaCert(X509Certificate cert);
	
	/**
	 * Store the node's private key and signed certificate.
	 */
	public void storeSignedKeyPair(X509Certificate cert, PrivateKey privateKey);
	
	
}
