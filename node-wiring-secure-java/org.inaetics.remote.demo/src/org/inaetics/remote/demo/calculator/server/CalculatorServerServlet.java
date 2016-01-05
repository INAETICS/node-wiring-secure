/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.demo.calculator.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

/**
 * A very simple servlet which just returns a string saying who we are
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class CalculatorServerServlet extends HttpServlet {

    private static final long serialVersionUID = 5397656219531103611L;
    private static final String CONTENTTYPE_TEXT_PLAIN = "text/plain";

    private static final String SERVLET_PATH = "/calculatorServer";
    private volatile HttpService m_httpService;

    private volatile LogService m_logService;

    public void start() {
        try {
            m_httpService.registerServlet(SERVLET_PATH, this, null, null);
        }
        catch (Exception e) {
            m_logService.log(LogService.LOG_ERROR, "could not register servlet", e);
        }
    }

    public void stop() {
        try {
            m_httpService.unregister(SERVLET_PATH);
        }
        catch (Exception e) {
            m_logService.log(LogService.LOG_ERROR, "could not unregister servlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // just return that we are the Calculator Demo Server
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(CONTENTTYPE_TEXT_PLAIN);
        PrintWriter writer = resp.getWriter();
        writer.write("This is the AMDATU Calculator Demo Server");
        writer.close();

    }

}
