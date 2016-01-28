package org.inaetics.wiring.admin.https;

import java.io.IOException;

import javax.security.cert.X509Certificate;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * A Servlet Filter that enforces the client to have a valid CA signed certificate in order
 * to establish a connection to the secure wire.
 *
 * @author <a href="mailto:contact@inaetics.org">Inaetics Project Secure Wiring Team</a>
 */
public class ClientCertificateEnforcementFilter implements Filter {
	
	private static final String CERT_KEY = "java.servlet.request.X509Certificate";
	
	private static final int HTTP_CODE_UNAUTHORIZED = 401;

	@Override
	public void destroy() {
		// nothing yet.
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
    	X509Certificate[] certificates = (X509Certificate[]) request.getAttribute(CERT_KEY);
    	
    	boolean authorized = (certificates != null && certificates.length > 0);
		if (authorized) {
			System.out.println("Client cert name: " + certificates[0].getSubjectDN().getName());
			chain.doFilter(request, response);
		} else {
			System.out.println("no client cert supplied");
	        HttpServletResponse httpResponse = ((HttpServletResponse) response);
	        httpResponse.setStatus(HTTP_CODE_UNAUTHORIZED);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// nothing yet.
	}

}
