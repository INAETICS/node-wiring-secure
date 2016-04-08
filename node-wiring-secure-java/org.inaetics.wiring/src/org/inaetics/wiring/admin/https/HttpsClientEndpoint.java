/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.inaetics.wiring.base.IOUtil.closeSilently;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.inaetics.truststorage.TrustStorageService;
import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.base.IOUtil;
import org.osgi.framework.ServiceException;

/**
 * Implementation of an secure http client that can send messages to remote wiring endpoints.
 * This implementation uses https/tls and is forked from the http wiring.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * @author <a href="mailto:contact@inaetics.org">Inaetics Project Secure Wiring Team</a>
 */
public final class HttpsClientEndpoint {

    private static final int FATAL_ERROR_COUNT = 5;
    private volatile TrustStorageService m_trustService;

    private final WiringEndpointDescription m_endpoint;
    private final HttpsAdminConfiguration m_configuration;

    private ClientEndpointProblemListener m_problemListener;
    private int m_remoteErrors;

    public HttpsClientEndpoint(WiringEndpointDescription endpoint, HttpsAdminConfiguration configuration , TrustStorageService trustService) {
        m_endpoint = endpoint;
        m_configuration = configuration;
        m_remoteErrors = 0;
        m_trustService = trustService;
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public void setProblemListener(ClientEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    /**
     * Handles I/O exceptions by counting the number of times they occurred, and if a certain
     * threshold is exceeded closes the import registration for this endpoint.
     * 
     * @param e the exception to handle.
     */
    private void handleRemoteException(IOException e) {
        if (m_problemListener != null) {
            if (++m_remoteErrors > FATAL_ERROR_COUNT) {
                m_problemListener.handleEndpointError(e);
            }
            else {
                m_problemListener.handleEndpointWarning(e);
            }
        }
    }


    /**
     * Does the actual invocation of the remote method.
     * <p>
     * This method assumes that all security checks (if needed) are processed!
     * </p>
     * 
     * @param method the actual method to invoke;
     * @param arguments the arguments of the method to invoke;
     * @return the result of the method invocation, can be <code>null</code>.
     * @throws Exception in case the invocation failed in some way.
     */
    String sendMessage(String message) throws Exception {

        HttpsURLConnection connection = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        String result = null;
        try {
        	
        	URL url = new URL(m_endpoint.getProperty(HttpsWiringEndpointProperties.URL));
            connection = (HttpsURLConnection) url.openConnection();
           
            // trust
            KeyStore trustStore = m_trustService.getTrustStore();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    		tmf.init(trustStore);
    		
    		// keys
    		KeyStore keyStore = m_trustService.getKeyStore();
    		final char[] keyStoreKeyPassword = m_trustService.getKeyStoreKeyPassword();
    		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    		kmf.init(keyStore, keyStoreKeyPassword);
    		
            HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            	private static final String HOSTNAME_VERIFICATION_PATTERN = "\\A(CN=){1}(%s){1}(,ST){1}(.)*\\Z";
            	
				@Override
				public boolean verify(String hostname, SSLSession session) {
					try {
						Pattern verifyPattern = Pattern.compile(String.format(HOSTNAME_VERIFICATION_PATTERN, hostname));						
						String peerPrincipal = session.getPeerPrincipal().getName();
						Matcher matcher = verifyPattern.matcher(peerPrincipal);
						return matcher.matches();
					} catch (Exception e) {
						return false;
					}
				}
            };
    		
    		SSLContext ctx = SSLContext.getInstance("TLS");
    		ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    		
    		SSLSocketFactory sslFactory = ctx.getSocketFactory();
            connection.setSSLSocketFactory(sslFactory);
            connection.setHostnameVerifier(hostnameVerifier);
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setConnectTimeout(m_configuration.getConnectTimeout());
            connection.setReadTimeout(m_configuration.getReadTimeout());
            connection.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
            connection.connect();
            System.out.println(connection.getCipherSuite());
            outputStream = connection.getOutputStream();
            
            System.out.println(message.getBytes("UTF-8"));
            
            outputStream.write(message.getBytes("UTF-8"));
            outputStream.flush();

            int rc = connection.getResponseCode();
            switch (rc) {
                case HTTP_OK:
                    inputStream = connection.getInputStream();
                    result = IOUtil.convertStreamToString(inputStream, "UTF-8");
                    break;
                default:
                    throw new IOException("Unexpected HTTP response: " + rc + " " + connection.getResponseMessage());
            }
            // Reset this error counter upon each successful request...
            m_remoteErrors = 0;
        }
        catch (IOException e) {
            handleRemoteException(e);
            throw new ServiceException("Remote service invocation failed: " + e.getMessage(), ServiceException.REMOTE, e);
        }
        finally {
            closeSilently(inputStream);
            closeSilently(outputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }

        return result;
    }

}
