/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.junit.admin;

import static org.inaetics.remote.itest.config.Configs.configs;
import static org.inaetics.remote.itest.config.Configs.frameworkConfig;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.inaetics.remote.admin.itest.api.EchoInterface;
import org.inaetics.remote.itest.config.Config;
import org.inaetics.remote.itest.config.FrameworkConfig;
import org.inaetics.remote.itest.junit.RemoteServicesTestBase;
import org.inaetics.remote.itest.util.FrameworkContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Abstract test base for testing RSA functionality.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractRemoteServiceAdminTest extends RemoteServicesTestBase {

    protected volatile ServiceReference<RemoteServiceAdmin> m_remoteServiceAdminReference;
    protected volatile RemoteServiceAdmin m_remoteServiceAdmin;

    protected static boolean stringListEquals(List<String> left, List<String> right) {
        if (left.size() != right.size()) {
            return false;
        }
        if (left.size() == 0) {
            return true;
        }
        List<String> copyOfLeft = new ArrayList<String>(left);
        List<String> copyOfRight = new ArrayList<String>(right);
        Collections.sort(copyOfLeft);
        Collections.sort(copyOfRight);
        for (int i = 0; i < left.size(); i++) {
            if (!copyOfLeft.get(i).equals(copyOfRight.get(i))) {
                return false;
            }
        }
        return true;
    }

    protected static List<String> strings(String... values) {
        List<String> list = new ArrayList<String>();
        for (String value : values) {
            list.add(value);
        }
        return list;
    }

    protected void assertExportRegistrationEmpty(Dictionary<String, Object> serviceProperties,
        Map<String, Object> extraProperies, Object serviceInstance) throws Exception {
        assertExportRegistrationEmpty(serviceProperties, extraProperies, serviceInstance,
            EchoInterface.class);
    }

    protected void assertExportRegistrationEmpty(Dictionary<String, Object> serviceProperties,
        Map<String, Object> extraProperies, Object serviceInstance, Class<?>... interfaces)
        throws Exception {

        String[] ifaces = new String[interfaces.length];
        for (int i = 0; i < ifaces.length; i++) {
            ifaces[i] = interfaces[i].getName();
        }

        ServiceRegistration<?> serviceRegistration =
            getChildBundleContext().registerService(ifaces, serviceInstance, serviceProperties);

        Collection<ExportRegistration> exportRegistrations = null;
        try {
            exportRegistrations =
                m_remoteServiceAdmin.exportService(serviceRegistration.getReference(), extraProperies);

            assertNotNull("Export resulted in null return", exportRegistrations);
            assertTrue("Export returned unexpected registrations", exportRegistrations.isEmpty());
        }
        finally {
            if (exportRegistrations != null) {
                for (ExportRegistration exportRegistration : exportRegistrations) {
                    exportRegistration.close();
                }
            }
            serviceRegistration.unregister();
        }
    }

    protected void assertExportRegistrationFails(Dictionary<String, Object> serviceProperties,
        Map<String, Object> extraProperies, Class<?> exceptionClass, Object serviceInstance) throws Exception {
        assertExportRegistrationFails(serviceProperties, extraProperies, exceptionClass, serviceInstance,
            EchoInterface.class);
    }

    protected void assertExportRegistrationFails(Dictionary<String, Object> serviceProperties,
        Map<String, Object> extraProperies, Class<?> exceptionClass, Object serviceInstance, Class<?>... interfaces)
        throws Exception {

        String[] ifaces = new String[interfaces.length];
        for (int i = 0; i < ifaces.length; i++) {
            ifaces[i] = interfaces[i].getName();
        }

        ServiceRegistration<?> serviceRegistration =
            getChildBundleContext().registerService(ifaces, serviceInstance, serviceProperties);

        Collection<ExportRegistration> exportRegistrations = null;
        Exception exception = null;
        try {
            exportRegistrations =
                m_remoteServiceAdmin.exportService(serviceRegistration.getReference(), extraProperies);
        }
        catch (Exception e) {
            exception = e;
        }
        finally {
            serviceRegistration.unregister();
        }

        if (exception != null) {
            if (exceptionClass == null || exception.getClass() != exceptionClass) {
                fail("Export resulted in unexpected exception of type " + exception.getClass().getName());
            }
        }
        else {
            if (exceptionClass != null) {
                fail("Export did not throw expected exception of type " + exceptionClass.getName());
            }
            assertTrue("Expected no export registration", exportRegistrations.isEmpty());
        }
    }

    protected void assertExportRegistrationSucceeds(Dictionary<String, Object> serviceProperties,
        Map<String, Object> extraProperies, List<String> expectedConfigTypes, List<String> expectedIntents,
        Object serviceInstance) throws Exception {
        assertExportRegistrationSucceeds(serviceProperties, extraProperies, expectedConfigTypes, expectedIntents,
            serviceInstance, EchoInterface.class);
    }

    protected void assertExportRegistrationSucceeds(Dictionary<String, Object> serviceProperties,
        Map<String, Object> extraProperies, List<String> expectedConfigTypes, List<String> expectedIntents,
        Object serviceInstance, Class<?>... interfaces) throws Exception {

        String[] ifaces = new String[interfaces.length];
        for (int i = 0; i < ifaces.length; i++) {
            ifaces[i] = interfaces[i].getName();
        }

        ServiceRegistration<?> serviceRegistration =
            getChildBundleContext().registerService(ifaces, serviceInstance, serviceProperties);

        Collection<ExportRegistration> exportRegistrations = null;

        try {
            exportRegistrations =
                m_remoteServiceAdmin.exportService(serviceRegistration.getReference(), extraProperies);

            assertTrue("Expected one or more export registration", exportRegistrations.size() > 0);

            for (ExportRegistration exportRegistration : exportRegistrations) {
                EndpointDescription endpointDescription = exportRegistration.getExportReference().getExportedEndpoint();

                if (expectedConfigTypes != null) {
                    assertTrue(stringListEquals(expectedConfigTypes, endpointDescription.getConfigurationTypes()));
                }
                if (expectedIntents != null) {
                    assertTrue(stringListEquals(expectedIntents, endpointDescription.getIntents()));
                }
            }

            for (Class<?> iface : interfaces) {
                assertEquals(1, countExportedServices(iface));
            }
        }
        finally {
            if (exportRegistrations != null) {
                for (ExportRegistration exportRegistration : exportRegistrations) {
                    exportRegistration.close();
                }
            }
            serviceRegistration.unregister();
        }
    }

    @Override
    protected void cleanupTest() throws Exception {
        if (m_remoteServiceAdminReference != null) {
            getChildContext("CHILD1").getBundleContext().ungetService(m_remoteServiceAdminReference);
            m_remoteServiceAdminReference = null;
        }
        m_remoteServiceAdmin = null;
    }

    @Override
    protected Config[] configureFramework(FrameworkContext parent) throws Exception {
        BundleContext parentBC = getParentContext().getBundleContext();
        String systemPackages = parentBC.getProperty("itest.systempackages");
        String defaultBundles = parentBC.getProperty("itest.bundles.default");
        String topologyManagerBundles = parentBC.getProperty("itest.bundles.admin.wiring");

        parent.setLogLevel(LogService.LOG_DEBUG);
        parent.setServiceTimout(30000);

        FrameworkConfig child1 = frameworkConfig("CHILD1")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(10000)
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .frameworkProperty("org.osgi.service.http.port", "8089")
            .frameworkProperty("org.amdatu.remote.admin.http.path", "an.rsa.path")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.zone", "zone1")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.node", "node1")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.connecturl", "http://docker:4001")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.rootpath", "/inaetics/discovery")
            .frameworkProperty("org.inaetics.wiring.admin.https.zone", "zone1")
            .frameworkProperty("org.inaetics.wiring.admin.https.node", "node1")
            .bundlePaths(defaultBundles, topologyManagerBundles);

        return configs(child1);
    }

    @Override
    protected void configureServices() throws Exception {

        m_remoteServiceAdminReference =
            getChildContext("CHILD1").getBundleContext().getServiceReference(RemoteServiceAdmin.class);
        for (int i = 0; i < 1000 && m_remoteServiceAdminReference == null; i++) {
            Thread.sleep(10);
            m_remoteServiceAdminReference =
                getChildContext("CHILD1").getBundleContext().getServiceReference(RemoteServiceAdmin.class);
        }
        assertNotNull("Unable to locate RemoteServiceAdmin reference", m_remoteServiceAdminReference);
        m_remoteServiceAdmin = getChildContext("CHILD1").getBundleContext().getService(m_remoteServiceAdminReference);

        assertNotNull("Unable to locate RemoteServiceAdmin service", m_remoteServiceAdmin);
    }

    protected int countImportedServices(Class<?> type) throws Exception {
        ServiceReference<?>[] serviceReferences =
            getChildBundleContext().getServiceReferences(type.getName(), "(" + SERVICE_IMPORTED + "=*)");
        if (serviceReferences == null) {
            return 0;
        }
        return serviceReferences.length;
    }

    protected int countExportedServices(Class<?> type) throws Exception {
        ServiceReference<?>[] serviceReferences =
            getChildBundleContext().getServiceReferences(type.getName(), "(" + SERVICE_EXPORTED_INTERFACES + "=*)");
        if (serviceReferences == null) {
            return 0;
        }
        return serviceReferences.length;
    }

    protected Object createInstance(String typeName) throws Exception {
        Bundle b = getChildBundleContext().getBundle();
        Class<?> type = b.loadClass(typeName);
        return type.newInstance();
    }

    protected final FrameworkContext getChildContext() {
        return getChildContext("CHILD1");
    }

    protected final BundleContext getChildBundleContext() {
        return getChildContext("CHILD1").getBundleContext();
    }

    protected final void logDebug(String message) {
        getParentContext().getLogService().log(LogService.LOG_DEBUG, message);
    }
}
