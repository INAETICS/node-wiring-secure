/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.admin.https;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.base.AbstractComponentDelegate;
import org.inaetics.wiring.base.IOUtil;
import org.inaetics.wiring.endpoint.WiringReceiver;

/**
 * Wiring component that handles all server endpoints.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HttpsServerEndpointHandler extends AbstractComponentDelegate {

    private final Map<String, HttpsServerEndpoint> m_handlers =
    		new HashMap<String, HttpsServerEndpoint>();
    
    private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

    private final WiringAdminFactory m_factory;
    private final HttpsAdminConfiguration m_configuration;

    private static final String MIME_TYPE = "text/plain;charset=utf-8";

    public HttpsServerEndpointHandler(WiringAdminFactory factory, HttpsAdminConfiguration configuration) {
        super(factory);
        m_factory = factory;
        m_configuration = configuration;
    }

    @Override
    protected void startComponentDelegate() {
        try {
            m_factory.getHttpService().registerServlet(getServletAlias(), new ServerEndpointServlet(), null, null);
        }
        catch (Exception e) {
            logError("Failed to initialize due to configuration problem!", e);
            throw new IllegalStateException("Configuration problem", e);
        }
    }

    @Override
    protected void stopComponentDelegate() {
        m_factory.getHttpService().unregister(getServletAlias());
    }

    /**
     * Add a Server Endpoint.
     * 
     * @param endpoint The Endpoint Description
     * @param receiver The Wiring Receiver
     */
    public HttpsServerEndpoint addEndpoint(WiringEndpointDescription endpoint, WiringReceiver receiver) {

        HttpsServerEndpoint serverEndpoint = new HttpsServerEndpoint(endpoint, receiver);

        m_lock.writeLock().lock();
        try {
            m_handlers.put(endpoint.getId(), serverEndpoint);
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serverEndpoint;
    }

    /**
     * Remove a Server Endpoint.
     * 
     * @param endpoint The Endpoint Description
     */
    public HttpsServerEndpoint removeEndpoint(WiringEndpointDescription endpoint) {
        HttpsServerEndpoint serv;

        m_lock.writeLock().lock();
        try {
            serv = m_handlers.remove(endpoint);
        }
        finally {
            m_lock.writeLock().unlock();
        }
        return serv;
    }

    private HttpsServerEndpoint getHandler(String path) {
        m_lock.readLock().lock();
        try {
        	return m_handlers.get(path);
        }
        finally {
            m_lock.readLock().unlock();
        }
    }

    private String getServletAlias() {
        String alias = m_configuration.getBaseUrl().getPath();
        if (!alias.startsWith("/")) {
            alias = "/" + alias;
        }
        if (alias.endsWith("/")) {
            alias = alias.substring(0, alias.length() - 1);
        }
        return alias;
    }

    /**
     * Writes all endpoint ids as a flat JSON array to the given HttpServletResponse
     * 
     * @param req the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws IOException
     */
    public void listEndpointIds(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setStatus(SC_OK);
        resp.setContentType(MIME_TYPE);

        String response = "wires:\n";
        
        m_lock.readLock().lock();
        try {
            for (String wireId : m_handlers.keySet()) {
            	response += wireId + "\n";
            }
        }
        finally {
            m_lock.readLock().unlock();
        }

    	ServletOutputStream outputStream = resp.getOutputStream();
    	try {
    		outputStream.write(response.getBytes("UTF-8"));
    	}
        catch (Exception e) {
            logError("Server Endpoint Handler failed", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    	finally {
            IOUtil.closeSilently(outputStream);
    	}
        
    }

    /**
     * Internal Servlet that handles all calls.
     */
    private class ServerEndpointServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        private final Pattern PATH_PATTERN = Pattern.compile("^\\/{0,1}([A-Za-z0-9-_]+)\\/{0,1}$");

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            Matcher matcher = PATH_PATTERN.matcher(pathInfo);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + pathInfo);
                return;
            }
            String path = matcher.group(1);

            HttpsServerEndpoint handler = getHandler(path);
            if (handler != null) {
                try {
                    handler.handleMessage(req, resp);
                }
                catch (Exception e) {
                    logError("Server Endpoint Handler failed: %s", e, path);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }
            else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            // provide endpoint information via http get

            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            // request on root will return an array of endpoint ids
            if (pathInfo.equals("") || pathInfo.equals("/")) {
                listEndpointIds(req, resp);
                return;
            }

            // handle requested endpoint
            Matcher matcher = PATH_PATTERN.matcher(pathInfo);
            if (!matcher.matches()) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + pathInfo);
                return;
            }

            String endpointId = matcher.group(1);

            HttpsServerEndpoint handler = getHandler(endpointId);
            if (handler != null) {
            	ServletOutputStream outputStream = resp.getOutputStream();
                try {
                    resp.setStatus(SC_OK);
                    resp.setContentType(MIME_TYPE);
                	outputStream.write(("id: " + endpointId).getBytes("UTF-8"));
                }
                catch (Exception e) {
                    logError("Server Endpoint Handler failed: %s", e, endpointId);
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                finally {
                    IOUtil.closeSilently(outputStream);
                }
            }
            else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }
}
