/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.junit.admin;

import static org.inaetics.remote.admin.wiring.WiringAdminConstants.PASSBYVALYE_INTENT;
import static org.mockito.Mockito.mock;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.inaetics.remote.admin.itest.api.EchoInterface;

/**
 * Tests the export of intents works correctly.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class ExportIntentsTest extends AbstractRemoteServiceAdminTest {

    /**
     * Remote Service Admin 122.5.1
     * <br/><br/>
     * service.exported.intents – ( String+) A list of intents that the Remote Service Admin service must
     * implement to distribute the given service.<br/>
     * service.exported.intents.extra – (String+) This property is merged with the service.exported.intents
     * property.
     */
    public void testRsaHandlesExportedIntents() throws Exception {
        doTestRsaHandlesExportedIntentsWithoutExtraServicePropertiesOk();
        doTestRsaHandlesExportedIntentsWithEmptyServicePropertiesOk();
        doTestRsaHandlesExportedIntentsThroughExtraServicePropertiesOk();
        doTestRsaHandlesExportedIntentsThroughAdditionalIntentsOk();
        doTestRsaHandlesExportedIntentsExtraServicePropertiesOverrideServicePropertiesOk();
        doTestRsaHandlesExportedIntentsExtraIntentsOverrideServicePropertiesOk();

        doTestRsaHandlesUnsupportedExportedIntentsFails();
        doTestRsaHandlesUnsupportedMultipleIntentsFails();
        doTestRsaHandlesUnsupportedOverriddenExtraIntentsFails();
    }

    protected void doTestRsaHandlesExportedIntentsExtraIntentsOverrideServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS_EXTRA, "NotSupportedByRSA");

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_INTENTS_EXTRA, PASSBYVALYE_INTENT);

        assertExportRegistrationSucceeds(serviceProperties, extraProperties, null,
            strings(PASSBYVALYE_INTENT), mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesExportedIntentsExtraServicePropertiesOverrideServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS, "NotSupportedByRSA");

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_INTENTS, PASSBYVALYE_INTENT);

        assertExportRegistrationSucceeds(serviceProperties, extraProperties, null,
            strings(PASSBYVALYE_INTENT), mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesExportedIntentsThroughAdditionalIntentsOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_INTENTS_EXTRA, PASSBYVALYE_INTENT);

        assertExportRegistrationSucceeds(serviceProperties, extraProperties, null, strings(PASSBYVALYE_INTENT), mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesExportedIntentsThroughExtraServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_INTENTS, PASSBYVALYE_INTENT);

        assertExportRegistrationSucceeds(serviceProperties, extraProperties, null, strings(PASSBYVALYE_INTENT), mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesExportedIntentsWithEmptyServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS_EXTRA, PASSBYVALYE_INTENT);

        assertExportRegistrationSucceeds(serviceProperties, new HashMap<String, Object>(), null, strings(PASSBYVALYE_INTENT), mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesExportedIntentsWithoutExtraServicePropertiesOk() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS, PASSBYVALYE_INTENT);
        serviceProperties.put("QQQ", "123");
        serviceProperties.put(".QQQ", "123");

        assertExportRegistrationSucceeds(serviceProperties, null, null, strings(PASSBYVALYE_INTENT), mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesUnsupportedExportedIntentsFails() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS, "NotSupportedByRSA");

        assertExportRegistrationFails(serviceProperties, null, null, mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesUnsupportedMultipleIntentsFails() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS, new String[] { "NotSupportedByRSA", PASSBYVALYE_INTENT });

        assertExportRegistrationFails(serviceProperties, null, null, mock(EchoInterface.class));
    }

    protected void doTestRsaHandlesUnsupportedOverriddenExtraIntentsFails() throws Exception {
        Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>();
        serviceProperties.put(SERVICE_EXPORTED_INTERFACES, EchoInterface.class.getName());
        serviceProperties.put(SERVICE_EXPORTED_INTENTS, PASSBYVALYE_INTENT);

        Map<String, Object> extraProperties = new HashMap<String, Object>();
        extraProperties.put(SERVICE_EXPORTED_INTENTS_EXTRA, "NotSupportedByRSA");

        assertExportRegistrationFails(serviceProperties, extraProperties, null, mock(EchoInterface.class));
    }
}
