/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.demo.calculator.client;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.celix.calc.api.Calculator;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

/**
 * A servlet which enables a web-based usage of the calculator demo client
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class CalculatorClientServlet extends HttpServlet {

    private static final long serialVersionUID = 7270900742899293706L;

    private static final String CONTENTTYPE_TEXT_HTML = "text/html";

    private static final String PARAM_OPERATOR = "op";
    private static final String PARAM_VAR1 = "a";
    private static final String PARAM_VAR2 = "b";

    private static final String OPERATOR_PLUS = "+";
    private static final String OPERATOR_MINUS = "-";
    private static final String OPERATOR_SQRT = "sqrt";

    private static final String SERVLET_PATH = "/calculatorClient";
    private volatile HttpService m_httpService;

    private volatile Calculator m_calc;
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

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(CONTENTTYPE_TEXT_HTML);
        PrintWriter writer = resp.getWriter();
        writer.write(getHeader());

        String operator = req.getParameter(PARAM_OPERATOR);
        String var1 = req.getParameter(PARAM_VAR1);
        String var2 = req.getParameter(PARAM_VAR2);

        if (operator != null || var1 != null || var2 != null) {
            try {
                if (m_calc == null) {
                    throw new Exception("remote calculator not available!");
                }

                String result;
                double a = Double.parseDouble(var1);

                if (OPERATOR_PLUS.equals(operator)) {
                    double b = Double.parseDouble(var2);
                    result = a + " + " + b + " = " + m_calc.add(a, b);
                }
                else if (OPERATOR_MINUS.equals(operator)) {
                    double b = Double.parseDouble(var2);
                    result = a + " - " + b + " = " + m_calc.sub(a, b);
                }
                else if (OPERATOR_SQRT.equals(operator)) {
                    if (a < 0) {
                        throw new Exception("cannot take sqrt of x < 0");
                    }
                    result = "sqrt( " + a + " ) = " + m_calc.sqrt(a);
                }
                else {
                    throw new Exception("unknown operator");
                }
                writer.write(result + "<br><br><br>");
            }
            catch (Exception e) {
                writer.write("invalid calculation request, please try again (" + e.getClass().getSimpleName() + ", "
                    + e.getMessage() + ")<br><br><br>");
            }
        }

        writer.write(getForm());
        writer.write(getFooter());
        writer.close();

    }

    private String getHeader() {
        return "<html><head><title>AMDATU Calculator Demo Client</title></head><body>";
    }

    private String getFooter() {
        return "</body></html>";
    }

    private String getForm() {
        StringBuilder sb = new StringBuilder();
        sb.append("<form method=\"GET\">");
        sb.append("Operator (+, -, sqrt): <input name=\"" + PARAM_OPERATOR + "\" /><br>");
        sb.append("Operand 1: <input name=\"" + PARAM_VAR1 + "\" /><br>");
        sb.append("Operand 2: <input name=\"" + PARAM_VAR2 + "\" /><br>");
        sb.append("<input type=\"submit\" />");
        sb.append("</form>");
        return sb.toString();
    }

}
