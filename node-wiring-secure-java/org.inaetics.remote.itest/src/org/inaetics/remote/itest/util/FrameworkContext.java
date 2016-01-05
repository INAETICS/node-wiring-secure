/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.util;

import static org.inaetics.remote.itest.util.ITestUtil.join;
import static org.inaetics.remote.itest.util.ITestUtil.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class FrameworkContext {

    private static final int SERVICE_TIMEOUT_DEFAULT = 30;
    private static final int LOG_LEVEL_DEFAULT = LogService.LOG_WARNING;

    private final List<Bundle> m_installedBundles = new ArrayList<Bundle>();

    private final String m_name;
    private final BundleContext m_bundleContext;
    private final DependencyManager m_dependencyManager;

    private volatile Framework m_framework;

    private ServiceRegistration<?> m_logServiceRegistration;
    private LogService m_logService = new InternalLogService();
    private int m_logLevel = LOG_LEVEL_DEFAULT;
    private long m_serviceTimeout = SERVICE_TIMEOUT_DEFAULT;

    /**
     * Create a Framework Context that wraps a Bundle Context. This is used for the parent framework.
     * Calling {@link #destroy()} will not shut down the actual OSGi framework.
     * 
     * @param name The Framework Context name
     * @param bundleContext The context
     */
    public FrameworkContext(String name, BundleContext bundleContext) throws Exception {
        m_name = name;
        m_bundleContext = bundleContext;
        m_dependencyManager = new DependencyManager(m_bundleContext);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_RANKING, Integer.valueOf(1000));
        m_logServiceRegistration =
            m_bundleContext.registerService(LogService.class.getName(), m_logService, properties);
    }

    /**
     * Create a Framework Context that wraps an OSGi Framework. This is used for child frameworks.
     * Calling {@link #destroy()} will shut down the actual OSGi framework.
     * 
     * @param name The Framework Context name
     * @param framewokr The OSGi Framework
     */
    public FrameworkContext(String name, Framework framework) throws Exception {
        m_name = name;
        m_framework = framework;
        m_framework.start();
        for (int i = 0; i < 100 && (framework.getState() != Framework.ACTIVE); i++) {
            Thread.sleep(10);
            if (i >= 99) {
                throw new IllegalStateException("Failed to start framework");
            }
        }
        m_bundleContext = m_framework.getBundleContext();
        m_dependencyManager = new DependencyManager(m_bundleContext);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_RANKING, Integer.valueOf(1000));
        m_logServiceRegistration =
            m_bundleContext.registerService(LogService.class.getName(), m_logService, properties);
    }

    public String getName() {
        return m_name;
    }

    public BundleContext getBundleContext() {
        return m_bundleContext;
    }

    public String getFrameworkUUID() {
        String uuid = getBundleContext().getProperty("org.osgi.framework.uuid");
        if (uuid != null) {
            return uuid;
        }
        synchronized ("org.osgi.framework.uuid") {
            uuid = getBundleContext().getProperty("org.osgi.framework.uuid");
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                System.setProperty("org.osgi.framework.uuid", uuid);
            }
            return uuid;
        }    	
    }

    public DependencyManager getDependencyManager() {
        return m_dependencyManager;
    }

    public LogService getLogService() {
        return m_logService;
    }

    public int getLogLevel() {
        return m_logLevel;
    }

    public void setLogLevel(int level) {
        m_logLevel = level;
    }

    public final long getServiceTimeout() {
        return m_serviceTimeout;
    }

    public final void setServiceTimout(long millis) {
        m_serviceTimeout = millis;
    }

    public Bundle[] installBundles(String... bundlePaths) throws Exception {
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (String bundlePath : join(",", bundlePaths).split(",")) {
            getLogService().log(LogService.LOG_DEBUG, "Installing bundle location: " + bundlePath);
            InputStream fis = new FileInputStream(new File(bundlePath));
            Bundle bundle = getBundleContext().installBundle(bundlePath, fis);
            bundles.add(bundle);
            fis.close();
            getLogService().log(LogService.LOG_DEBUG,
                "Installed bundle: " + bundle.getSymbolicName() + "/" + bundle.getVersion());
        }
        m_installedBundles.addAll(bundles);
        return bundles.toArray(new Bundle[bundles.size()]);
    }

    public Bundle[] startBundles(String... bundlePaths) throws Exception {
        Bundle[] bundles = installBundles(bundlePaths);
        for (Bundle bundle : bundles) {
            getLogService().log(LogService.LOG_DEBUG,
                "Starting bundle: " + bundle.getSymbolicName() + "/" + bundle.getVersion());
            bundle.start();
            getLogService().log(LogService.LOG_DEBUG,
                "Started bundle: " + bundle.getSymbolicName() + "/" + bundle.getVersion());
        }
        return bundles;
    }

    public void destroy() throws Exception {

        for (Bundle bundle : m_installedBundles) {
            if (bundle.getState() != Bundle.ACTIVE) {
                continue;
            }
            getLogService().log(LogService.LOG_DEBUG,
                "Stopping bundle: " + bundle.getSymbolicName() + "/" + bundle.getVersion());
            try {
                bundle.stop();
                if (bundle.getSymbolicName().equals("org.apache.felix.http.jetty")) {
                    // async felix jetty needs more time
                    Thread.sleep(2000);
                }
                getLogService().log(LogService.LOG_DEBUG,
                    "Stopped bundle: " + bundle.getSymbolicName() + "/" + bundle.getVersion());
            }
            catch (Exception e) {
                getLogService().log(LogService.LOG_ERROR,
                    "Exception stopping bundle : " + bundle.getSymbolicName() + "/" + bundle.getVersion(), e);
            }
        }
        m_installedBundles.clear();
        m_logServiceRegistration.unregister();

        // child framework only
        if (m_framework != null) {
            m_framework.stop();
            FrameworkEvent result = m_framework.waitForStop(1000);
            if (result.getType() == FrameworkEvent.WAIT_TIMEDOUT) {
                System.err.println("[WARNING] Framework did not stop in time...");
            }
            m_framework = null;
        }
    }

    public void configure(String pid, String... configuration) throws IOException {
        Dictionary<String, Object> props = properties(configuration);
        Configuration config = getConfiguration(pid);
        config.update(props);
    }

    public Configuration getConfiguration(String pid) throws IOException {
        ConfigurationAdmin admin = getService(ConfigurationAdmin.class);
        return admin.getConfiguration(pid, null);
    }

    public String configureFactory(String factoryPid, String... configuration) throws IOException {
        Dictionary<String, Object> props = properties(configuration);
        Configuration config = createFactoryConfiguration(factoryPid);
        config.update(props);
        return config.getPid();
    }

    public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
        ConfigurationAdmin admin = getService(ConfigurationAdmin.class);
        return admin.createFactoryConfiguration(factoryPid, null);
    }

    public <T> T getService(Class<T> serviceClass) {
        try {
            return getService(serviceClass, null);
        }
        catch (InvalidSyntaxException e) {
            return null;
            // Will not happen, since we don't pass in a filter.
        }
    }

    public <T> T getService(Class<T> serviceClass, String filterString) throws InvalidSyntaxException {
        T serviceInstance = null;

        ServiceTracker<T, ? extends T> serviceTracker;
        if (filterString == null) {
            serviceTracker = new ServiceTracker<T, T>(m_bundleContext, serviceClass.getName(), null);
        }
        else {
            String classFilter = "(" + Constants.OBJECTCLASS + "=" + serviceClass.getName() + ")";
            filterString = "(&" + classFilter + filterString + ")";
            serviceTracker =
                new ServiceTracker<T, T>(m_bundleContext, m_bundleContext.createFilter(filterString), null);
        }
        serviceTracker.open();
        try {
            serviceInstance = serviceTracker.waitForService(m_serviceTimeout);
            if (serviceInstance == null) {
                throw new ServiceException("Service not available: " + serviceClass.getName() + " " + filterString);
            }
            else {
                return serviceInstance;
            }
        }
        catch (InterruptedException e) {
            throw new ServiceException("Service not available: " + serviceClass.getName() + " " + filterString);
        }
    }

    /**
     * Internal LogService that logs to the console
     */
    private class InternalLogService implements LogService {

        @Override
        public void log(int level, String message) {
            log(null, level, message, null);
        }

        @Override
        public void log(int level, String message, Throwable exception) {
            log(null, level, message, exception);
        }

        @Override
        public void log(@SuppressWarnings("rawtypes") ServiceReference serviceReference, int level, String message) {
            log(serviceReference, level, message, null);
        }

        @Override
        public void log(@SuppressWarnings("rawtypes") ServiceReference serviceReference, int level, String message,
            Throwable exception) {
            if (level <= m_logLevel) {
                System.out.println("[" + m_name + "] " +
                    (serviceReference == null ? "" : serviceReference + " ") +
                    getLevel(level) + " " +
                    message + " " +
                    (exception == null ? "" : exception));
            }
        }

        private String getLevel(int level) {
            switch (level) {
                case 1:
                    return "[ERROR]";
                case 2:
                    return "[WARN ]";
                case 3:
                    return "[INFO ]";
                case 4:
                    return "[DEBUG]";
                default:
                    return "[?????]";
            }
        }
    }
}