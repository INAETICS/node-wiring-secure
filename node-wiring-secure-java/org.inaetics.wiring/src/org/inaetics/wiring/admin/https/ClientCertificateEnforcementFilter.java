package org.inaetics.wiring.admin.https;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A Servlet Filter that enforces the client to have a valid CA signed certificate in order
 * to establish a connection to the secure wire.
 *
 * @author <a href="mailto:contact@inaetics.org">Inaetics Project Secure Wiring Team</a>
 */
public class ClientCertificateEnforcementFilter implements Filter {
	
	private static final String CERT_KEY = "javax.servlet.request.X509Certificate";
	
	//TODO: make this configurable!
	private static final String[] DISCOVERY_WHITELIST = {
			"/org.amdatu.remote.discovery.etcd"
	};
	
	private static final int HTTP_CODE_UNAUTHORIZED = 401;

	@Override
	public void destroy() {
		// nothing yet.
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		if (requestIsWhitelisted(request)) {
			System.out.println("whitelisted request, may proceed");
			chain.doFilter(request, response);
			return;
		}
		
		// TODO: apply better filter logic!!
    	X509Certificate[] certificates = (X509Certificate[]) request.getAttribute(CERT_KEY);
    	boolean authorized = validateClientCertificates(certificates);
		if (authorized) {
			System.out.println("Client cert name: " + certificates[0].getSubjectDN().getName());
			chain.doFilter(request, response);
			return;
		} else {
			System.out.println("no client cert supplied");
	        HttpServletResponse httpResponse = ((HttpServletResponse) response);
	        httpResponse.setStatus(HTTP_CODE_UNAUTHORIZED);
	        return;
		}
	}
	
	/**
	 * Check if request was whitelisted (for unsecure communication).
	 * May be used f.e. for service discovery.
	 * @param request The current request object.
	 * @return true, if request is whitelisted, otherwise false
	 */
	private boolean requestIsWhitelisted(ServletRequest request) {
		String uri = null;
		if (request instanceof HttpServletRequest) {
			uri = ((HttpServletRequest)request).getRequestURI();
		} else {
			// not a http/https request
			return false;
		}
		
		if (uri != null && !uri.isEmpty()) {
			for (String wlEntry : DISCOVERY_WHITELIST) {
				if (uri.startsWith(wlEntry)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Performs the client certificate validation.
	 * @param certificates Array of supplied client certificates
	 * @return true, if certificate(s) were valid
	 */
	private boolean validateClientCertificates(X509Certificate[] certificates) {
		// TODO: apply better filter logic!
		return (certificates != null && certificates.length > 0);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// nothing.
	}

}
