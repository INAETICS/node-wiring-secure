/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.demo.calculator.client;

import java.util.Properties;

import org.apache.celix.calc.api.Calculator;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class Activator extends DependencyActivatorBase {

    public static final String SCOPE = "calc";
    public static final String[] FUNCTIONS = { "add", "sub", "sqrt" };

    private volatile Calculator m_calc;

    public void add(CommandSession session, double a, double b) {
        session.getConsole().printf("%f + %f = %f%n", a, b, m_calc.add(a, b));
    }

    public void sub(CommandSession session, double a, double b) {
        session.getConsole().printf("%f - %f = %f%n", a, b, m_calc.sub(a, b));
    }

    public void sqrt(CommandSession session, double a) {
        session.getConsole().printf("sqrt(%f) = %f%n", a, m_calc.sqrt(a));
    }

    public void init(BundleContext context, DependencyManager manager) throws Exception {
        Properties props = new Properties();
        props.put(CommandProcessor.COMMAND_SCOPE, SCOPE);
        props.put(CommandProcessor.COMMAND_FUNCTION, FUNCTIONS);
        manager.add(createComponent()
            .setInterface(Object.class.getName(), props)
            .setImplementation(this)
            .add(createServiceDependency().setService(Calculator.class).setRequired(true)));

        final CalculatorClientServlet calculatorClientServlet = new CalculatorClientServlet();
        manager.add(createComponent()
            .setImplementation(calculatorClientServlet)
            .add(createServiceDependency().setService(Calculator.class).setRequired(true))
            .add(createServiceDependency().setService(HttpService.class).setRequired(true))
            .add(createServiceDependency().setService(LogService.class).setRequired(false)));
    }

    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }
}
