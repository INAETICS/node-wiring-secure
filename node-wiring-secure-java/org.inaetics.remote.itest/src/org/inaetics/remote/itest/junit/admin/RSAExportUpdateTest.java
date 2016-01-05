/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.junit.admin;

import static org.inaetics.remote.admin.wiring.WiringAdminConstants.CONFIGURATION_TYPE;
import static org.mockito.Mockito.mock;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.inaetics.remote.admin.itest.api.EchoInterface;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

/**
 * RSA Export update tests.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class RSAExportUpdateTest extends AbstractRemoteServiceAdminTest {

    public void testRemoteServiceAdminExport() throws Exception {
        doTestExportUpdateSimpleSuccess();
        doTestExportUpdateFailsOnInvalidConfigType();
        doTestExportUpdateOnClosedRegistrationFails();
    }

    protected void doTestExportUpdateSimpleSuccess() throws Exception {

        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_CONFIGS, CONFIGURATION_TYPE);

        ServiceRegistration<?> serviceRegistration =
            getChildBundleContext().registerService(EchoInterface.class.getName(), mock(EchoInterface.class),
                serviceProperties);

        Map<String, Object> extraProperies = new HashMap<String, Object>();
        extraProperies.put("some.property", "123");

        Collection<ExportRegistration> exportRegistrations =
            m_remoteServiceAdmin.exportService(serviceRegistration.getReference(), extraProperies);
        assertEquals("Expected one export registration", 1, exportRegistrations.size());

        ExportRegistration exportRegistration = exportRegistrations.iterator().next();
        EndpointDescription endpointDescription = exportRegistration.getExportReference().getExportedEndpoint();

        assertEquals("Expected one or more export registration", "123",
            endpointDescription.getProperties().get("some.property"));

        extraProperies = new HashMap<String, Object>();
        extraProperies.put("some.property", "456");

        exportRegistration.update(extraProperies);

        endpointDescription = exportRegistration.getExportReference().getExportedEndpoint();

        assertEquals("Expected one or more export registration", "456",
            endpointDescription.getProperties().get("some.property"));

        extraProperies = new HashMap<String, Object>();
        extraProperies.put("another.property", "456");

        exportRegistration.update(extraProperies);

        endpointDescription = exportRegistration.getExportReference().getExportedEndpoint();

        assertNull("Expected some property to be unset", endpointDescription.getProperties()
            .get("some.property"));

        assertEquals("Expected another property to be set", "456",
            endpointDescription.getProperties().get("another.property"));

        exportRegistration.close();
        serviceRegistration.unregister();
    }

    protected void doTestExportUpdateFailsOnInvalidConfigType() throws Exception {

        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_CONFIGS, CONFIGURATION_TYPE);

        ServiceRegistration<?> serviceRegistration =
            getChildBundleContext().registerService(EchoInterface.class.getName(), mock(EchoInterface.class),
                serviceProperties);

        Map<String, Object> extraProperies = new HashMap<String, Object>();
        extraProperies.put("some.property", "123");

        Collection<ExportRegistration> exportRegistrations =
            m_remoteServiceAdmin.exportService(serviceRegistration.getReference(), extraProperies);
        assertEquals("Expected one export registration", 1, exportRegistrations.size());

        ExportRegistration exportRegistration = exportRegistrations.iterator().next();
        EndpointDescription endpointDescription = exportRegistration.getExportReference().getExportedEndpoint();

        assertEquals("Expected some.property to be set", "123",
            endpointDescription.getProperties().get("some.property"));

        extraProperies = new HashMap<String, Object>();
        extraProperies.put("some.property", "123");
        extraProperies.put(SERVICE_EXPORTED_CONFIGS, "NOSUCHCONFIGTYPE");

        endpointDescription = exportRegistration.update(extraProperies);

        assertNull("Expected some.property to be set", endpointDescription);

        // FIXME we need an exception here
// assertNotNull("Expected exception to be set", exportRegistration.getException());

        exportRegistration.close();
        serviceRegistration.unregister();
    }

    protected void doTestExportUpdateOnClosedRegistrationFails() throws Exception {

        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_CONFIGS, CONFIGURATION_TYPE);

        ServiceRegistration<?> serviceRegistration =
            getChildBundleContext().registerService(EchoInterface.class.getName(), mock(EchoInterface.class),
                serviceProperties);

        Map<String, Object> extraProperies = new HashMap<String, Object>();
        extraProperies.put("some.property", "123");

        Collection<ExportRegistration> exportRegistrations =
            m_remoteServiceAdmin.exportService(serviceRegistration.getReference(), extraProperies);
        assertEquals("Expected one export registration", 1, exportRegistrations.size());

        ExportRegistration exportRegistration = exportRegistrations.iterator().next();
        exportRegistration.close();
        try {
            exportRegistration.update(extraProperies);
            serviceRegistration.unregister();
            fail("Expected update on closed export registration to throw IllegalStateException");
        }
        catch (IllegalStateException e) {
            serviceRegistration.unregister();
            // expected
        }
        catch (Exception e) {
            serviceRegistration.unregister();
            fail("Expected update on closed export registration to throw IllegalStateException");
        }
    }
}
