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
import org.inaetics.truststorage.TrustStorageConfiguration;
import org.inaetics.truststorage.TrustStorageService;

public class WiringConnectorFactory implements ConnectorFactory {
	private volatile TrustStorageService m_trustStorageService;
	private volatile HttpsAdminConfiguration m_configuration;
	
	public WiringConnectorFactory(HttpsAdminConfiguration configuration) {
		m_configuration = configuration;
	};

	@Override
	public Connector createConnector() {
		SslContextFactory sslContextFactory = new ShortTrustSslContextFactory();
		try {
			// this are just dummy keystores to trick the SslContextFactory
			sslContextFactory.setKeyStore(KeyStore.getInstance(KeyStore.getDefaultType()));
			sslContextFactory.setTrustStore(KeyStore.getInstance(KeyStore.getDefaultType()));
		} catch (KeyStoreException e) {
			// do nothing.
		}
		boolean enforceClientAuth = m_configuration.shouldEenforceClientCertValidation();
		sslContextFactory.setNeedClientAuth(enforceClientAuth);
		Connector connector = new SslSocketConnector(sslContextFactory);
		int securePort = m_configuration.getSecurePort();
		connector.setPort(securePort);

		return connector;
	}

	private class ShortTrustSslContextFactory extends SslContextFactory {
		private static final String INAETICS_ALIAS = "INAETICS";
		private static final String INAETICS_CRYPTO_ALG = "RSA";
		
		@Override
		protected TrustManager[] getTrustManagers(KeyStore arg0, Collection<? extends CRL> arg1) throws Exception {
			TrustManager[] trustManagers = new TrustManager[] { new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					X509Certificate cert = m_trustStorageService.getRootCaCert();
					X509Certificate[] ret = { cert };
					return ret;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					System.out.println("for the breakpoint.");
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
					System.out.println("for the breakpoint.");
				}
			} };
			return trustManagers;
		}

		@Override
		protected KeyManager[] getKeyManagers(KeyStore arg0) throws Exception {
			KeyManager[] keyManagers = new KeyManager[] { new X509KeyManager() {
				@Override
				public String[] getServerAliases(String keyType, Principal[] issuers) {
					// not (yet) required!
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
					// not (yet) required!
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
					// not (yet)required
					return null;
				}
			} };
			return keyManagers;
		}
	}

}
