/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.base.IOUtil;
import org.inaetics.wiring.endpoint.WiringReceiver;

/**
 * Servlet that represents a local wiring endpoint.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpsServerEndpoint {

    private static final String MIME_TYPE = "text/plain;charset=utf-8";

    private WiringEndpointDescription m_endpoint;
    private WiringReceiver m_receiver;
    private ServerEndpointProblemListener m_problemListener;

    public HttpsServerEndpoint(WiringEndpointDescription endpoint, WiringReceiver receiver) {
    	m_endpoint = endpoint;
    	m_receiver = receiver;
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public void setProblemListener(ServerEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    public void handleMessage(HttpServletRequest req, HttpServletResponse resp) throws Exception {

    	InputStream in = req.getInputStream();
    	OutputStream out = resp.getOutputStream();

    	try {

        	String message = IOUtil.convertStreamToString(in, "UTF-8");
			String result = m_receiver.messageReceived(message);

            resp.setStatus(SC_OK);
            resp.setContentType(MIME_TYPE);
            
            out.write(result.getBytes("UTF-8"));
            
        }
        finally {
            IOUtil.closeSilently(in);
            IOUtil.closeSilently(out);
        }
    }

}
