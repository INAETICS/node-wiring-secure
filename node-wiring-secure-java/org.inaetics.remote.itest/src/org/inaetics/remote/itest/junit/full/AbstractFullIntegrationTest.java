/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.junit.full;

import static org.inaetics.remote.ServiceUtil.getStringPlusValue;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.CONFIGURATION_TYPE;
import static org.inaetics.remote.itest.util.ITestUtil.stringArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_INTENTS;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.inaetics.remote.admin.itest.api.EchoData;
import org.inaetics.remote.admin.itest.api.EchoInterface;
import org.inaetics.remote.itest.junit.RemoteServicesTestBase;
import org.mockito.Matchers;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

/**
 * Abstract test for full integration with an RemoteServiceAdmin, a TopologyManager and a Discovery
 * implementation. Concrete implementation must provision from frameworks.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public abstract class AbstractFullIntegrationTest extends RemoteServicesTestBase {

    /**
     * Very specific helper for {@link #testBasicServiceExportImportInvoke()} that captures the Endpoint
     * Description of the first Export Registration event it sees.
     * 
     * TODO move/integrate to a reusable BlockingEndpointEventListener that takes a filter and returns
     * the received endpoint
     */
    private static class ExportedEndpointListener implements RemoteServiceAdminListener {

        private final AtomicBoolean m_open = new AtomicBoolean(true);
        private volatile EndpointDescription m_endpointDescription;

        @Override
        public void remoteAdminEvent(RemoteServiceAdminEvent event) {
            switch (event.getType()) {
                case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
                    if (!m_open.compareAndSet(true, false)) {
                        throw new IllegalStateException("we're closed...");
                    }
                    m_endpointDescription = event.getExportReference().getExportedEndpoint();
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        public EndpointDescription getEndpointDescription() {
            for (int i = 0; i < 1000 && m_endpointDescription == null; i++) {
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return m_endpointDescription;
        }
    }

    /**
     * Registers an {@code EchoInterface} service in CHILD1 and assert that it being imported in CHILD2
     * with the correct service properties.
     */
    public void testBasicServiceExportImportInvoke() throws Exception {

        getParentContext().getLogService().log(LogService.LOG_INFO, "Registering exported service");

        ExportedEndpointListener exportedEndpointListener = new ExportedEndpointListener();
        ServiceRegistration<RemoteServiceAdminListener> listenerRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(RemoteServiceAdminListener.class,
                exportedEndpointListener, null);

        Dictionary<String, Object> exportedServiceProperties = new Hashtable<String, Object>();
        exportedServiceProperties.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        exportedServiceProperties
            .put("endpoint.package.version." + EchoInterface.class.getPackage().getName(), "1.0.0");
        exportedServiceProperties.put(SERVICE_EXPORTED_CONFIGS, CONFIGURATION_TYPE);
        exportedServiceProperties.put(SERVICE_INTENTS, new String[] { "passByValue" });
        exportedServiceProperties.put("arbitrary.prop", "bla");
        exportedServiceProperties.put(".private.prop", "bla");

        EchoInterface exportedEchoService = mock(EchoInterface.class);
        EchoData data = new EchoData(0, "nul");
        List<EchoData> dataList = new LinkedList<>();
        dataList.add(new EchoData(1, "een"));
        dataList.add(new EchoData(2, "twee"));

        when(exportedEchoService.echo("Amdatu")).thenReturn("Amdatu");
        when(exportedEchoService.echo(Matchers.<EchoData> any())).thenReturn(data);
        when(exportedEchoService.echo(Matchers.<List<EchoData>> any())).thenReturn(dataList);

        ServiceRegistration<?> exportedServiceRegistration =
            getChildContext("CHILD1").getBundleContext().registerService(EchoInterface.class.getName(),
                exportedEchoService, exportedServiceProperties);

        try {
            ServiceReference<?> exportedServiceReference = exportedServiceRegistration.getReference();
            assertNotNull(exportedServiceReference);

            EndpointDescription exportedEndpointDescription = exportedEndpointListener.getEndpointDescription();
            assertNotNull(exportedEndpointDescription);
            listenerRegistration.unregister();
            listenerRegistration = null;

            getParentContext().getLogService().log(LogService.LOG_INFO, "Invoking imported service");

            EchoInterface importedService = getChildContext("CHILD2").getService(EchoInterface.class);
            assertNotNull("No imported for service found!", importedService);

            assertEquals("Amdatu", importedService.echo("Amdatu"));

            EchoData result = importedService.echo(data);
            assertEquals(data.getX(), result.getX());
            assertEquals(data.getY(), result.getY());

            List<EchoData> resultList = importedService.echo(dataList);
            assertEquals(dataList.get(0).getX(), resultList.get(0).getX());
            assertEquals(dataList.get(0).getY(), resultList.get(0).getY());
            assertEquals(dataList.get(1).getX(), resultList.get(1).getX());
            assertEquals(dataList.get(1).getY(), resultList.get(1).getY());

            getParentContext().getLogService().log(LogService.LOG_INFO, "Checking imported service");

            Collection<ServiceReference<EchoInterface>> serviceReferences =
                getChildContext("CHILD2").getBundleContext().getServiceReferences(EchoInterface.class, null);
            assertEquals("Expected one service reference", 1, serviceReferences.size());
            ServiceReference<EchoInterface> importedServiceReference = serviceReferences.iterator().next();

            assertTrue("Imported Service Property " + OBJECTCLASS + " must match Endpoint Description",
                stringArrayEquals(new String[] { EchoInterface.class.getName() },
                    getStringPlusValue(importedServiceReference.getProperty(OBJECTCLASS))));

            assertEquals("Imported Service Property " + ENDPOINT_ID + " must be set correctly",
                exportedEndpointDescription.getId(),
                importedServiceReference.getProperty(ENDPOINT_ID));

            assertEquals("Imported Service Property " + ENDPOINT_SERVICE_ID + " must be set correctly",
                exportedServiceReference.getProperty(SERVICE_ID),
                importedServiceReference.getProperty(ENDPOINT_SERVICE_ID));

            assertEquals(
                "Imported Service Property endpoint.package." + EchoInterface.class.getPackage().getName()
                    + " must be set correctly",
                "1.0.0",
                importedServiceReference.getProperty("endpoint.package.version."
                    + EchoInterface.class.getPackage().getName()));

            assertNotNull("Imported Service Property " + SERVICE_IMPORTED + " must be set to any value",
                importedServiceReference.getProperty(SERVICE_IMPORTED));

            assertTrue("Imported Service Property " + SERVICE_INTENTS + " must match Endpoint Description",
                stringArrayEquals(new String[] { "passByValue" },
                    getStringPlusValue(importedServiceReference.getProperty(SERVICE_INTENTS))));

            assertTrue("Imported Service Property " + SERVICE_IMPORTED_CONFIGS + " must match Endpoint Description",
                stringArrayEquals(new String[] { CONFIGURATION_TYPE },
                    getStringPlusValue(importedServiceReference.getProperty(SERVICE_IMPORTED_CONFIGS))));

            assertEquals("Imported Service Property arbitrary.prop must be set correctly", "bla",
                importedServiceReference.getProperty("arbitrary.prop"));

            assertNull("Imported Service Property .private.prop must not be set",
                importedServiceReference.getProperty(".private.prop"));

            // update
            getParentContext().getLogService().log(LogService.LOG_INFO, "Updating exported service");

            final CountDownLatch latch = new CountDownLatch(1);
            ServiceRegistration<?> reg =
                getChildContext("CHILD2").getBundleContext().registerService(RemoteServiceAdminListener.class,
                    new RemoteServiceAdminListener() {

                        @Override
                        public void remoteAdminEvent(RemoteServiceAdminEvent event) {
                            latch.countDown();
                        }
                    }, null);

            Dictionary<String, Object> updatedServiceProperties = new Hashtable<String, Object>();
            for (String propertyKey : exportedServiceReference.getPropertyKeys()) {
                updatedServiceProperties.put(propertyKey, exportedServiceReference.getProperty(propertyKey));
            }
            updatedServiceProperties.put("someNewProperty", "aValue");
            exportedServiceRegistration.setProperties(updatedServiceProperties);

            if (!latch.await(30, TimeUnit.SECONDS)) {
                reg.unregister();
                fail("no event");
            }
            reg.unregister();

            getParentContext().getLogService().log(LogService.LOG_INFO, "Checking updated service");
            serviceReferences =
                getChildContext("CHILD2").getBundleContext().getServiceReferences(EchoInterface.class, null);
            for (int i = 0; i < 100 && serviceReferences.size() == 0; i++) {
                Thread.sleep(100);
                serviceReferences =
                    getChildContext("CHILD2").getBundleContext().getServiceReferences(EchoInterface.class, null);
            }
            assertEquals("Expected one service reference", 1, serviceReferences.size());
            importedServiceReference = serviceReferences.iterator().next();

            assertEquals("Imported Service Property someNewProperty must be set correctly", "aValue",
                importedServiceReference.getProperty("someNewProperty"));

        }
        finally {
            if (listenerRegistration != null) {
                listenerRegistration.unregister();
                listenerRegistration = null;
            }
            exportedServiceRegistration.unregister();
        }
    }


}
