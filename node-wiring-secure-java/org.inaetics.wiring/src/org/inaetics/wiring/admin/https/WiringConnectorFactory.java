package org.inaetics.wiring.admin.https;

import java.net.Socket;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.apache.felix.http.jetty.ConnectorFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.inaetics.truststorage.TrustStorageService;

public class WiringConnectorFactory implements ConnectorFactory {
	private volatile TrustStorageService m_trustStorageService;

	@Override
	public Connector createConnector() {
		SslContextFactory sslContextFactory = new ShortTrustSslContextFactory();
		// sslContextFactory
		// TODO expose this factory as ManagedService as well in order to make
		// it configurable...
		sslContextFactory.setKeyStore("/inkeys/inaetics.keystore");
		sslContextFactory.setKeyStorePassword("changeit");
		sslContextFactory.setTrustStore("/inkeys/inaetics.truststore");
		sslContextFactory.setTrustStorePassword("changeit");
		sslContextFactory.setNeedClientAuth(false);

		// port = getConfigIntValue(m_context,
		// "org.osgi.service.http.port.secure", properties, 8443);
		Connector connector = new SslSocketConnector(sslContextFactory);
		connector.setPort(8555);

		return connector;
	}

	private class ShortTrustSslContextFactory extends SslContextFactory {
		@Override
		protected TrustManager[] getTrustManagers(KeyStore arg0, Collection<? extends CRL> arg1) throws Exception {
			TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					System.out.println("TRUST");
					return null;
				}

				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					System.out.println("TRUST");
				}

				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					System.out.println("TRUST");
				}
			} };
			return trustManagers;
		}

		@Override
		protected KeyManager[] getKeyManagers(KeyStore arg0) throws Exception {
			KeyManager[] keyManagers = new KeyManager[] { new X509KeyManager() {
				private static final String INAETICS_ALIAS = "INAETICS";
				private static final String INAETICS_CRYPTO_ALG = "RSA";

				@Override
				public String[] getServerAliases(String keyType, Principal[] issuers) {
					System.out.println("KEY");
					return null;
				}

				@Override
				public PrivateKey getPrivateKey(String alias) {
					if (alias.equals(INAETICS_ALIAS)) {
						try {
							Key pk = m_trustStorageService.getKeyStore().getKey("keypair",
									m_trustStorageService.getKeyStoreKeyPassword());
							return (PrivateKey) pk;
						} catch (UnrecoverableKeyException e) {
							// do nothing, connection will fail
						} catch (KeyStoreException e) {
							// do nothing, connection will fail
						} catch (NoSuchAlgorithmException e) {
							// do nothing, connection will fail
						}
					}
					return null;
				}

				@Override
				public String[] getClientAliases(String keyType, Principal[] issuers) {
					// TODO Auto-generated method stub
					System.out.println("KEY");
					return null;
				}

				@Override
				public X509Certificate[] getCertificateChain(String alias) {
					if (alias.equals(INAETICS_ALIAS)) {
						try {
							Certificate[] certs = m_trustStorageService.getKeyStore().getCertificateChain("keypair");
							if (certs != null && certs.length > 0) {
								X509Certificate[] ret = { (X509Certificate) certs[certs.length - 1] };
								return ret;
							}
						} catch (KeyStoreException e) {
							// do nothing, connection will fail.
						}
					}
					return null;
				}

				@Override
				public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
					if (keyType.equals(INAETICS_CRYPTO_ALG)) {
						return INAETICS_ALIAS;
					}
					return null;
				}

				@Override
				public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
					// TODO Auto-generated method stub
					System.out.println("KEY");
					return null;
				}
			} };
			return keyManagers;
		}
	}

}
