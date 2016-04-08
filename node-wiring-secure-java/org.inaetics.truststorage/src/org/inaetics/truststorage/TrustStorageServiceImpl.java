package org.inaetics.truststorage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class TrustStorageServiceImpl implements TrustStorageService {

	private volatile TrustStorageConfiguration m_configuration;
	
	public TrustStorageServiceImpl(TrustStorageConfiguration configuration) {
		m_configuration = configuration;
	};
	
	// getters
	
	/**
	 * Get the truststore from filesystem
	 * @return the truststore
	 */
	public KeyStore getTrustStore() {
		try {
			return getKeyStoreFromFile(m_configuration.getTrustStoreFileName(), m_configuration.getTrustStorePassword());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
		}
		return null;
	};
	
	/**
	 * Get the keystore from filesystem
	 * @return the keystore
	 */
	public KeyStore getKeyStore() {
		try {
			return getKeyStoreFromFile(m_configuration.getKeyStoreFileName(), m_configuration.getKeyStorePassword());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
		}
		return null;
	};
	
	/**
	 * Get the current root CA certificate from truststore
	 * @return rootCaCert
	 */
	public X509Certificate getRootCaCert() {
		try {
			Certificate certFound = getTrustStore().getCertificate("rootcert");
			
			if(certFound != null) {
				CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
				InputStream in = new ByteArrayInputStream(certFound.getEncoded());
				return (X509Certificate)certFactory.generateCertificate(in);
			}
		} catch (KeyStoreException | CertificateException e) {
			e.printStackTrace();
		}
		return null;
	};
	
	
	// setters
	
	/**
	 * Store the CA's certificate in the truststore
	 * @param cert the CA's certificate
	 */
	public void storeRootCaCert(X509Certificate cert) {	
		try {
			KeyStore trustedStore = getTrustStore();
			trustedStore.setCertificateEntry("rootcert", cert);
			trustedStore.store(new FileOutputStream(m_configuration.getTrustStoreFileName()), m_configuration.getTrustStorePassword().toCharArray());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
		}
	};
	
	/**
	 * Store the node's private key and signed certificate.
	 */
	public void storeSignedKeyPair(X509Certificate cert, PrivateKey privateKey) {
		try {
			KeyStore keyStore = getKeyStore();
			
			KeyStore.PrivateKeyEntry keyStoreEntry = new KeyStore.PrivateKeyEntry(privateKey, new X509Certificate[]{cert});
			PasswordProtection keyPassword = new PasswordProtection(m_configuration.getKeyStoreKeyPassword().toCharArray());
			
			keyStore.setEntry("keypair", keyStoreEntry, keyPassword);
			keyStore.store(new FileOutputStream(m_configuration.getKeyStoreFileName()), m_configuration.getKeyStorePassword().toCharArray());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
		}
	};
	
	/**
	 * Store the node's private key and signed certificate.
	 */
	private KeyStore getKeyStoreFromFile(String fileName, String password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
	    File file = new File(fileName);
	 
	    final KeyStore keyStore = KeyStore.getInstance(m_configuration.getKeyStoreType());    
	    
	    if (file.exists()) {
	    	System.out.println("no keystores available, creating");
	        keyStore.load(new FileInputStream(file), password.toCharArray());    
	    } else {
	    	// create parent folders
	    	File parent = file.getParentFile();
	    	if(!parent.exists() && !parent.mkdirs()){
	    	    throw new IllegalStateException("Keystore Failure: Couldn't create dir: " + parent);
	    	}
	    	
	        keyStore.load(null, null);
	        keyStore.store(new FileOutputStream(fileName), password.toCharArray());    
	    }
	 
	    return keyStore;
	}

	@Override
	public char[] getKeyStoreKeyPassword() {
		return m_configuration.getKeyStorePassword().toCharArray();
	}

//	@Override
//	public void storeSignedKeyPair(X509Certificate cert, PrivateKey privateKey) {
//		// TODO Auto-generated method stub
//		
//	}
	
	
}
