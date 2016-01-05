/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.junit.admin;

import static org.inaetics.remote.admin.wiring.WiringAdminConstants.CONFIGURATION_TYPE;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.PASSBYVALYE_INTENT;
import static org.mockito.Mockito.mock;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_CONFIGS_SUPPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.REMOTE_INTENTS_SUPPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_INTENTS;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.inaetics.remote.admin.itest.api.EchoImpl;
import org.inaetics.remote.admin.itest.api.EchoInterface;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Testing {@link RemoteServiceAdmin} implementations in isolation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class RemoteServiceAdminTest extends AbstractRemoteServiceAdminTest {

    /**
     * Test that combines several specification tests that can safely run sequentially in one framework.
     * This is just more efficient then starting a fresh framework for every test.
     * 
     * @throws Exception
     */
    public void testRsaSpecRequirements() throws Exception {
        doTestRsaPublishesSupportedIntentsAndConfigurationTypes();
        doTestRsaPublishesCorrectEndpointProperties();
        doTestRsaClosesExportRegistrationOnUnget();
        doTestImportLocallyExportedService();
    }

    /**
     * A very basic test that imports a locally exported service and tries to invoke it.
     */
    protected void doTestImportLocallyExportedService() throws Exception {
        logDebug("Registering local service");

        String ifaceName = EchoInterface.class.getName();
        Object echoService = createInstance(EchoImpl.class.getName());

        Dictionary<String, Object> localProperties = new Hashtable<String, Object>();
        localProperties.put(SERVICE_EXPORTED_INTERFACES, ifaceName);

        BundleContext childContext = getChildBundleContext();

        ServiceRegistration<?> localServiceRegistration =
            childContext.registerService(ifaceName, echoService, localProperties);
        ServiceReference<?> localServiceReference = localServiceRegistration.getReference();

        logDebug("Exporting local service");
        Collection<ExportRegistration> exportRegistrations =
            m_remoteServiceAdmin.exportService(localServiceReference, null);

        assertNotNull("Expected a registration for the exported service", exportRegistrations);
        assertEquals("Expected a registration for the exported service", 1, exportRegistrations.size());
        ExportRegistration exportRegistration = exportRegistrations.iterator().next();

        Collection<ExportReference> exportReferences = m_remoteServiceAdmin.getExportedServices();
        assertNotNull(exportRegistrations);
        assertEquals(1, exportReferences.size());
        ExportReference exportReference = exportReferences.iterator().next();
        EndpointDescription description = exportReference.getExportedEndpoint();

        logDebug("Importing remote service");
        ImportRegistration importRegistration = m_remoteServiceAdmin.importService(description);
        assertNotNull("Expected a registration for the imported service", importRegistration);

        logDebug("Invoking imported remote service");
        ServiceReference<?>[] serviceReferences =
            childContext.getServiceReferences(ifaceName, "(" + SERVICE_IMPORTED + "=true)");

        assertNotNull("Expected a reference for the imported service", serviceReferences);
        assertEquals("Expected a reference for the imported service", 1, serviceReferences.length);

        EchoInterface echo = (EchoInterface) childContext.getService(serviceReferences[0]);

        assertNotNull("Expected a service", echo);
        String echoResponse = echo.echo("Amdatu");

        assertNotNull("Expected a response for the imported service", echoResponse);
        assertEquals("Expected an echo response for the imported service", "Amdatu", echoResponse);
        childContext.ungetService(serviceReferences[0]);

        logDebug("Closing import registration");
        importRegistration.close();

        Collection<ImportReference> importReferences = m_remoteServiceAdmin.getImportedEndpoints();
        assertEquals("Expected no references for the imported service", 0, importReferences.size());

        serviceReferences = childContext.getServiceReferences(ifaceName, "(" + SERVICE_IMPORTED + "=true)");
        assertNull("Expected no reference for the imported service", serviceReferences);

        logDebug("Closing export registration");
        exportRegistration.close();
        exportReferences = m_remoteServiceAdmin.getExportedServices();
        assertEquals("Expected no references for the exported service", 0, exportReferences.size());

        logDebug("Unregistering local service");
        localServiceRegistration.unregister();
    }

    /**
     * Remote Service Admin 122.5.4 - Service Factory
     * <br/><br/>
     * A Remote Service Admin service must use a Service Factory for its service object to maintain separation
     * between Topology Managers. All registrations obtained through a Remote Service Admin service are life
     * cycle bound to the Topology Manager that created it. That is, if a Topology Manager ungets its Remote
     * Service Admin service, all registrations obtained through this service must automatically be closed.
     * 
     * @throws Exception
     */
    protected void doTestRsaClosesExportRegistrationOnUnget() throws Exception {
        logDebug("Registering local service");
        Dictionary<String, Object> localProperties = new Hashtable<String, Object>();
        localProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        ServiceRegistration<?> localServiceRegistration =
            getChildBundleContext().registerService(EchoInterface.class.getName(), mock(EchoInterface.class),
                localProperties);
        ServiceReference<?> localServiceReference = localServiceRegistration.getReference();

        logDebug("Exporting local service first time");
        Collection<ExportRegistration> exportRegistrations =
            m_remoteServiceAdmin.exportService(localServiceReference, null);
        assertNotNull("Expected a registration for the exported service", exportRegistrations);
        assertEquals("Expected a registration for the exported service", 1, exportRegistrations.size());
        assertEquals("Expected one exported service", 1, m_remoteServiceAdmin.getExportedServices().size());

        getChildBundleContext().ungetService(m_remoteServiceAdminReference);
        m_remoteServiceAdmin = getChildBundleContext().getService(m_remoteServiceAdminReference);

        assertEquals("Expected no exported services", 0, m_remoteServiceAdmin.getExportedServices().size());
    }

    /**
     * Remote Service Admin 122.5.2
     * <br/><br/>
     * The Remote Service Admin service must ensure that service properties are according to the Remote
     * Services chapter for an imported service. This means that it must register the following properties:
     * <ul>
     * <li>service.imported – (*) Must be set to any value.</li>
     * <li>service.imported.configs – (String+) The configuration information used to import this service.
     * Any associated properties for this configuration types must be properly mapped to the importing
     * system. For example, a URL in these properties must point to a valid resource when used in the
     * importing framework, see Resource Containment on page 310. Multiple configuration types can be
     * listed if they are synonyms for exactly the same Endpoint that is used to export this service.</li>
     * <li>service.intents – (String+) The Remote Service Admin must set this property to convey the
     * combined intents of:
     * <ul>
     * <li>The exporting service, and</li>
     * <li>The intents that the exporting distribution provider adds, and</li>
     * <li>The intents that the importing distribution provider adds.</li>
     * </ul>
     * <li>Any additional properties listed in the Endpoint Description that should not be excluded. See
     * Endpoint Description on page 306 for more details about the properties in the Endpoint Description.</li>
     * </ul>
     */
    protected void doTestRsaPublishesCorrectEndpointProperties() throws Exception {
        String ifaceName = EchoInterface.class.getName();
        Object echoService = createInstance(EchoImpl.class.getName());

        Dictionary<String, Object> localProperties = new Hashtable<String, Object>();
        localProperties.put(SERVICE_EXPORTED_INTERFACES, ifaceName);
        localProperties.put("test.passon", "should be passed on");
        localProperties.put("test.overwrite", "should be overwritten");
        localProperties.put(".test.private", "should be hidden");

        Map<String, Object> extraProperties = new Hashtable<String, Object>();
        extraProperties.put("test.overwrite", "has been overwritten");

        BundleContext childContext = getChildBundleContext();

        ServiceRegistration<?> localServiceRegistration =
            childContext.registerService(ifaceName, echoService, localProperties);
        ServiceReference<?> localServiceReference = localServiceRegistration.getReference();

        logDebug("Exporting local service");
        Collection<ExportRegistration> exportRegistrations =
            m_remoteServiceAdmin.exportService(localServiceReference, extraProperties);

        assertNotNull("Expected a registration for the exported service", exportRegistrations);
        assertEquals("Expected a registration for the exported service", 1, exportRegistrations.size());
        ExportRegistration exportRegistration = exportRegistrations.iterator().next();

        Collection<ExportReference> exportReferences = m_remoteServiceAdmin.getExportedServices();
        assertNotNull(exportRegistrations);
        assertEquals(1, exportReferences.size());
        ExportReference exportReference = exportReferences.iterator().next();
        EndpointDescription exportedEndpoint = exportReference.getExportedEndpoint();

        Map<String, Object> exportProperties = exportedEndpoint.getProperties();
        assertEquals("true", exportProperties.get(SERVICE_IMPORTED));
        assertEquals(strings(CONFIGURATION_TYPE), strings((String[]) exportProperties.get(SERVICE_IMPORTED_CONFIGS)));
        assertEquals(strings(PASSBYVALYE_INTENT), strings((String[]) exportProperties.get(SERVICE_INTENTS)));
        assertEquals("should be passed on", exportProperties.get("test.passon"));
        assertEquals("has been overwritten", exportProperties.get("test.overwrite"));
        assertNull(exportProperties.get(".test.private"));

        logDebug("Importing remote service");
        ImportRegistration importRegistration = m_remoteServiceAdmin.importService(exportedEndpoint);
        assertNotNull("Expected a registration for the imported service", importRegistration);

        ImportReference importReference = importRegistration.getImportReference();
        EndpointDescription importedEndpoint = importReference.getImportedEndpoint();

        Map<String, Object> importProperties = importedEndpoint.getProperties();
        assertEquals("true", importProperties.get(SERVICE_IMPORTED));
        assertEquals(strings(CONFIGURATION_TYPE), strings((String[]) importProperties.get(SERVICE_IMPORTED_CONFIGS)));
        assertEquals(strings(PASSBYVALYE_INTENT), strings((String[]) importProperties.get(SERVICE_INTENTS)));
        assertEquals("should be passed on", importProperties.get("test.passon"));
        assertEquals("has been overwritten", importProperties.get("test.overwrite"));
        assertNull(importProperties.get(".test.private"));

        ServiceReference<?>[] serviceReferences =
            childContext.getServiceReferences(ifaceName, "(" + SERVICE_IMPORTED + "=true)");

        assertNotNull("Expected a reference for the imported service", serviceReferences);
        assertEquals("Expected a reference for the imported service", 1, serviceReferences.length);
        ServiceReference<?> serviceReference = serviceReferences[0];

        assertEquals("true", serviceReference.getProperty(SERVICE_IMPORTED));
        assertEquals(strings(CONFIGURATION_TYPE),
            strings((String[]) serviceReference.getProperty(SERVICE_IMPORTED_CONFIGS)));
        assertEquals(strings(PASSBYVALYE_INTENT),
            strings((String[]) serviceReference.getProperty(SERVICE_INTENTS)));
        assertEquals(serviceReference.getProperty("test.passon"), "should be passed on");
        assertEquals(serviceReference.getProperty("test.overwrite"), "has been overwritten");
        assertNull(serviceReference.getProperty(".test.private"));

        exportRegistration.close();
        importRegistration.close();
        localServiceRegistration.unregister();
    }

    /**
     * Remote Services 100.5.2 / AMDATURS-8
     * <br/><br/>
     * A bundle that uses a configuration type has an implicit dependency on the distribution provider. To
     * make this dependency explicit, the distribution provider must register a service with the following
     * properties:
     * <ul>
     * <li>remote.intents.supported – (String+) The vocabulary of the given distribution provider.</li>
     * <li>remote.configs.supported – (String+) The configuration types that are implemented by the distribution provider.</li>
     * <ul>
     */
    protected void doTestRsaPublishesSupportedIntentsAndConfigurationTypes() throws Exception {
        ServiceReference<?>[] references =
            getChildBundleContext().getServiceReferences(RemoteServiceAdmin.class.getName(), null);
        assertNotNull("Expected exactly one RSA reference", references);
        assertEquals("Expected exactly one RSA reference", 1, references.length);
        assertNotNull("RSA must publish supported configs", references[0].getProperty(REMOTE_CONFIGS_SUPPORTED));
        assertNotNull("RSA must publish supported intents", references[0].getProperty(REMOTE_INTENTS_SUPPORTED));
    }
}
