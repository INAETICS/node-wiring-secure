/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.junit.admin;

import static org.mockito.Mockito.mock;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.CONFIGURATION_TYPE;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.inaetics.remote.admin.itest.api.EchoInterface;

/**
 * RSA export tests related to configuration types.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class ExportConfigurationTypesTest extends AbstractRemoteServiceAdminTest {

    /**
     * Remote Service Admin 122.5.1
     * <br/><br/>
     * service.exported.configs – (String+ ) A list of configuration types that should be used to export
     * this service. Each configuration type represents the configuration parameters for an Endpoint. A
     * Remote Service Admin service should create an Endpoint for each configuration type that it sup-
     * ports and ignore the types it does not recognize. If this property is not set, then the Remote Service
     * Admin implementation must choose a convenient configuration type that then must be reported
     * on the Endpoint Description with the service.imported.configs associated with the returned
     * Export Registration.
     */
    public void testRsaHandlesExportedConfigurationTypes() throws Exception {
        doTestRsaHandlesImplicitDefinedConfigurationTypeOk();
        doTestRsaHandlesExplicitDefinedConfigurationTypeOk();
        doTestRsaHandlesExplicitDefinedConfigurationTypeWithEmptyExtraServicePropertiesOk();
        doTestRsaIgnoresUnsupportedConfigurationTypeOk();
        doTestRsaHandlesOverriddenConfigurationTypeOk();

        doTestRsaHandlesUnsupportedConfigurationTypeFail();
        doTestRsaHandlesUnsupportedExtraConfigurationTypeFail();
    }

    protected void doTestRsaHandlesExplicitDefinedConfigurationTypeOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_CONFIGS, CONFIGURATION_TYPE);

        assertExportRegistrationSucceeds(serviceProperties, null, strings(CONFIGURATION_TYPE), null,
            mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesExplicitDefinedConfigurationTypeWithEmptyExtraServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_CONFIGS, CONFIGURATION_TYPE);

        assertExportRegistrationSucceeds(serviceProperties, new HashMap<String, Object>(), strings(CONFIGURATION_TYPE),
            null, mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesImplicitDefinedConfigurationTypeOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());

        assertExportRegistrationSucceeds(serviceProperties, null, strings(CONFIGURATION_TYPE), null,
            mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesOverriddenConfigurationTypeOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_CONFIGS, "NotSupportedByRSA");

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_CONFIGS, CONFIGURATION_TYPE);

        assertExportRegistrationSucceeds(serviceProperties, extraProperties, strings(CONFIGURATION_TYPE), null,
            mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesUnsupportedConfigurationTypeFail() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_CONFIGS, "NotSupportedByRSA");

        assertExportRegistrationFails(serviceProperties, new HashMap<String, Object>(), null, mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesUnsupportedExtraConfigurationTypeFail() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_CONFIGS, "NotSupportedByRSA");

        assertExportRegistrationFails(serviceProperties, extraProperties, null, mock(EchoInterface.class));
    }

    protected void doTestRsaIgnoresUnsupportedConfigurationTypeOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_CONFIGS, new String[] { "NotSupportedByRSA", CONFIGURATION_TYPE });

        assertExportRegistrationSucceeds(serviceProperties, new HashMap<String, Object>(), strings(CONFIGURATION_TYPE),
            null, mock(EchoInterface.class));
    }
}
