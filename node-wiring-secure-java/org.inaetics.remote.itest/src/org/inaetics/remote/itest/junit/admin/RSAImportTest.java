/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.junit.admin;

import static org.inaetics.remote.ServiceUtil.getStringPlusValue;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.CONFIGURATION_TYPE;
import static org.inaetics.remote.itest.util.ITestUtil.stringArrayEquals;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_INTENTS;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.inaetics.remote.admin.itest.api.EchoInterface;
import org.inaetics.remote.admin.wiring.WiringAdminConstants;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportRegistration;

/**
 * RSA import tests.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class RSAImportTest extends AbstractRemoteServiceAdminTest {

    public void testRemoteServiceAdminImport() throws Exception {
        doTestRsaImportReturnsNullForUnrecognizedConfig();
        doTestRsaImportReturnsNullForUnavailableClasses();
        // won't work, needs wire doTestRsaImportProperties();
        // won't work, needs wire doTestRsaImportUpdate();
        // won't work, needs wire doTestRsaImportConfigurationChanged();
    }

    /**
     * Remote Service Admin 122.5.2
     * <br/><br/>
     * If the Remote Service Admin service does not recognize any of the configuration types then it must
     * return null. If there are multiple configuration types recognized then the Remote Service Admin is
     * free to select any one of the recognized types.
     */
    protected void doTestRsaImportReturnsNullForUnrecognizedConfig() throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Constants.OBJECTCLASS, new String[] { EchoInterface.class.getName() });
        properties.put(Constants.SERVICE_ID, 999l);
        properties.put(ENDPOINT_ID, "123");
        properties.put(ENDPOINT_FRAMEWORK_UUID, "xyz");
        properties.put(SERVICE_IMPORTED_CONFIGS, "UnknownType");

        EndpointDescription description = new EndpointDescription(properties);
        ImportRegistration registration = m_remoteServiceAdmin.importService(description);
        assertNull("Expected null registration for unsupported config type", registration);

        properties.put(SERVICE_IMPORTED_CONFIGS, new String[] { "UnknownType", "UnknownType2" });
        description = new EndpointDescription(properties);
        registration = m_remoteServiceAdmin.importService(description);
        assertNull("Expected null registration for unsupported config type", registration);
    }

    /**
     * Remote Service Admin / AMDATURS-107
     * <br/><br/>
     * If the Remote Service Admin can not load the interface classes it can not import the service and
     * must therefore return null.
     */
    protected void doTestRsaImportReturnsNullForUnavailableClasses() throws Exception {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Constants.OBJECTCLASS, new String[] { "no.such.class.available" });
        properties.put(Constants.SERVICE_ID, 999l);
        properties.put(ENDPOINT_ID, "123");
        properties.put(ENDPOINT_FRAMEWORK_UUID, "xyz");
        properties.put(SERVICE_IMPORTED_CONFIGS, CONFIGURATION_TYPE);

        EndpointDescription description = new EndpointDescription(properties);
        ImportRegistration registration = m_remoteServiceAdmin.importService(description);
        assertNull("Expected null registration for unavailable class", registration);

        properties.put(Constants.OBJECTCLASS, new String[] { EchoInterface.class.getName(), "no.such.class.available" });
        description = new EndpointDescription(properties);
        registration = m_remoteServiceAdmin.importService(description);
        assertNull("Expected null registration for unavailable class", registration);
    }

    /**
     * Remote Service Admin 122.5.2 / AMDATURS-9
     * <br/><br/>
     * The Remote Service Admin service must ensure that service properties are according to the Remote
     * Services chapter for an imported service. This means that it must register the following properties:
     * <ul>
     * <li>service.imported – (*) Must be set to any value.</li>
     * <li>service.imported.configs – (String+) The configuration information used to import this service.</li>
     * <li>service.intents – (String+) The Remote Service Admin must set this property to convey the combined intents</li>
     * <li>Any additional properties listed in the Endpoint Description that should not be excluded</li>
     * </ul>
     */
    protected void doTestRsaImportProperties() throws Exception {

        Long serviceId = 321l;
        String endpointId = "123";
        String frameworkUUID = "xyz";
        String wireId = "qwe";
        String packageVersion = "1.0";

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(OBJECTCLASS, new String[] { EchoInterface.class.getName() });
        properties.put(ENDPOINT_ID, endpointId);
        properties.put(ENDPOINT_SERVICE_ID, serviceId);
        properties.put(ENDPOINT_FRAMEWORK_UUID, frameworkUUID);
        properties.put(WiringAdminConstants.WIRE_ID, wireId);
        properties.put("endpoint.package." + EchoInterface.class.getPackage().getName(), packageVersion);
        properties.put(SERVICE_IMPORTED_CONFIGS, CONFIGURATION_TYPE);
        properties.put(SERVICE_INTENTS, new String[] { "passByValue" });

        EndpointDescription endpointDescription = new EndpointDescription(properties);
        ImportRegistration importRegistration = m_remoteServiceAdmin.importService(endpointDescription);

        try {
            assertNotNull("Expected an import registration for endpoint", importRegistration);

            Collection<ServiceReference<EchoInterface>> serviceReferences =
                getChildBundleContext().getServiceReferences(EchoInterface.class, null);
            assertEquals("Expected one service reference", 1, serviceReferences.size());
            ServiceReference<EchoInterface> serviceReference = serviceReferences.iterator().next();

            assertNotNull("Service Property " + OBJECTCLASS + " must be set correctly",
                serviceReference.getProperty(OBJECTCLASS));

            assertEquals("Service Property " + ENDPOINT_SERVICE_ID + " must be set correctly", serviceId,
                serviceReference.getProperty(ENDPOINT_SERVICE_ID));

            assertEquals("Service Property " + ENDPOINT_FRAMEWORK_UUID + " must be set correctly", frameworkUUID,
                serviceReference.getProperty(ENDPOINT_FRAMEWORK_UUID));

            assertEquals("Service Property " + ENDPOINT_ID + " must be set correctly", endpointDescription.getId(),
                serviceReference.getProperty(ENDPOINT_ID));

            assertEquals("Service Property endpoint.package." + EchoInterface.class.getPackage().getName()
                + " must be set correctly", packageVersion,
                serviceReference.getProperty("endpoint.package." + EchoInterface.class.getPackage().getName()));

            assertTrue("Service Property " + SERVICE_INTENTS + " must match Endpoint Description",
                stringArrayEquals(new String[] { "passByValue" },
                    getStringPlusValue(serviceReference.getProperty(SERVICE_INTENTS))));

            assertNotNull("Service Property " + SERVICE_IMPORTED + " must be set to any value",
                serviceReference.getProperty(SERVICE_IMPORTED));

            assertTrue("Service Property " + SERVICE_IMPORTED_CONFIGS + " must match Endpoint Description",
                stringArrayEquals(new String[] { CONFIGURATION_TYPE },
                    getStringPlusValue(serviceReference.getProperty(SERVICE_IMPORTED_CONFIGS))));

        }
        finally {
            if (importRegistration != null) {
                importRegistration.close();
            }
        }
    }

    protected void doTestRsaImportUpdate() throws Exception {

        Long serviceId = 321l;
        String endpointId = "123";
        String frameworkUUID = "xyz";
        String wireId = "qwe";
        String packageVersion = "1.0";

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(OBJECTCLASS, new String[] { EchoInterface.class.getName() });
        properties.put(ENDPOINT_ID, endpointId);
        properties.put(ENDPOINT_SERVICE_ID, serviceId);
        properties.put(ENDPOINT_FRAMEWORK_UUID, frameworkUUID);
        properties.put(WiringAdminConstants.WIRE_ID, wireId);
        properties.put("endpoint.package." + EchoInterface.class.getPackage().getName(), packageVersion);
        properties.put(SERVICE_IMPORTED_CONFIGS, CONFIGURATION_TYPE);
        properties.put(SERVICE_INTENTS, new String[] { "passByValue" });
        properties.put("some.property", "123");

        EndpointDescription endpointDescription = new EndpointDescription(properties);
        ImportRegistration importRegistration = m_remoteServiceAdmin.importService(endpointDescription);

        try {

            assertNotNull("Expected an import registration for endpoint", importRegistration);

            Collection<ServiceReference<EchoInterface>> serviceReferences =
                getChildBundleContext().getServiceReferences(EchoInterface.class, null);
            assertEquals("Expected one service reference", 1, serviceReferences.size());
            ServiceReference<EchoInterface> serviceReference = serviceReferences.iterator().next();

            assertEquals("Service Property some.property must be set correctly", "123",
                serviceReference.getProperty("some.property"));

            properties.put("some.property", "456");
            properties.put("some.other.property", 10l);

            endpointDescription = new EndpointDescription(properties);

            importRegistration.update(endpointDescription);

            serviceReferences =
                getChildBundleContext().getServiceReferences(EchoInterface.class, null);
            assertEquals("Expected one service reference", 1, serviceReferences.size());
            serviceReference = serviceReferences.iterator().next();

            assertEquals("Service Property some.property must be set correctly", "456",
                serviceReference.getProperty("some.property"));

            assertEquals("Service Property some.property must be set correctly", 10l,
                serviceReference.getProperty("some.other.property"));
        }
        finally {
            if (importRegistration != null) {
                importRegistration.close();
            }
        }
    }

    /**
     * The Remote Service Admin service must ensure that existing export- and import registrations are closed
     * when configuration changes.
     * 
     * @throws Exception
     */
    protected void doTestRsaImportConfigurationChanged() throws Exception {

        Long serviceId = 321l;
        String endpointId = "123";
        String frameworkUUID = "xyz";
        String wireId = "qwe";
        String packageVersion = "1.0";

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(OBJECTCLASS, new String[] { EchoInterface.class.getName() });
        properties.put(ENDPOINT_ID, endpointId);
        properties.put(ENDPOINT_SERVICE_ID, serviceId);
        properties.put(ENDPOINT_FRAMEWORK_UUID, frameworkUUID);
        properties.put(WiringAdminConstants.WIRE_ID, wireId);
        properties.put("endpoint.package." + EchoInterface.class.getPackage().getName(), packageVersion);
        properties.put(SERVICE_IMPORTED_CONFIGS, CONFIGURATION_TYPE);
        properties.put(SERVICE_INTENTS, new String[] { "passByValue" });
        properties.put("some.property", "123");

        EndpointDescription endpointDescription = new EndpointDescription(properties);
        ImportRegistration importRegistration = m_remoteServiceAdmin.importService(endpointDescription);

        try {
            assertNotNull("Expected an import registration for endpoint", importRegistration);

            getChildContext().configure(WiringAdminConstants.SERVICE_PID, WiringAdminConstants.PATH_CONFIG_KEY, "qqq");

            // FIXME wait for event
            Thread.sleep(1000);
            assertNull(importRegistration.getImportReference());
        }
        finally {
            if (importRegistration != null) {
                importRegistration.close();
            }
        }
    }
}
