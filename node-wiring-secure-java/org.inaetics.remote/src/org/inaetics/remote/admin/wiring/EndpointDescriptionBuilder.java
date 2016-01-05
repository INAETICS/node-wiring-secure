/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import static org.inaetics.remote.ServiceUtil.getFrameworkUUID;
import static org.inaetics.remote.ServiceUtil.getStringPlusValue;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.CONFIGURATION_TYPE;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.PASSBYVALYE_INTENT;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.SUPPORTED_INTENTS;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTENTS_EXTRA;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_INTENTS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.inaetics.remote.AbstractComponentDelegate;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class EndpointDescriptionBuilder extends AbstractComponentDelegate {

    public static final Set<String> MY_SUPPORTED_INTENTS_SET = new HashSet<String>(
            Arrays.asList(SUPPORTED_INTENTS));
	
    public EndpointDescriptionBuilder(RemoteServiceAdminFactory manager) {
		super(manager);
	}

    public EndpointDescription createEndpointDescription(String endpointId, ServiceReference<?> reference,
        Map<String, ?> extraProperties, Map<String, String> moreExtraProperties) {
    	Map<String, Object> mergedProperties = getMergedProperties(reference, extraProperties);
    	if (moreExtraProperties != null) {
    		mergedProperties.putAll(moreExtraProperties);
    	}
        return createEndpointDescription(endpointId, mergedProperties);
    }

    private EndpointDescription createEndpointDescription(String endpointId, Map<String, Object> properties) {
        String[] configurationTypes = getStringPlusValue(properties.get(SERVICE_EXPORTED_CONFIGS));
        if (configurationTypes.length > 0) {
            if (!Arrays.asList(configurationTypes).contains(CONFIGURATION_TYPE)) {
                logDebug("Can not export service (no supported configuration type specified): %s", properties);
                return null;
            }
        }

        if (properties.get(SERVICE_EXPORTED_INTERFACES) == null) {
            logWarning("Can not export service (no exported interfaces): %s", properties);
            throw new IllegalArgumentException("Can not export service (no exported interfaces)");
        }

        String[] exportedInterfaces = getExportedInterfaces(properties);
        if (exportedInterfaces.length == 0) {
            logWarning("Can not export service (no exported interfaces): %s", properties);
            return null;
        }

        String[] exportedIntents = getExportedIntents(properties);
        if (!isExportedIntentsSupported(exportedIntents)) {
            logDebug("Can not export service (unsupported intent specified): %s", properties);
            return null;
        }

        properties.put(ENDPOINT_ID, endpointId);
        properties.put(OBJECTCLASS, exportedInterfaces);
        properties.put(SERVICE_IMPORTED_CONFIGS, new String[] { CONFIGURATION_TYPE });
        properties.put(SERVICE_INTENTS, exportedIntents);
        properties.put(ENDPOINT_SERVICE_ID, properties.get(SERVICE_ID));
        properties.put(ENDPOINT_FRAMEWORK_UUID, getFrameworkUUID(getBundleContext()));

        return new EndpointDescription(properties);
    }
	
    /**
     * Returns a list of exported interface names as declared by the SERVICE_EXPORTED_INTERFACES property
     * using the following rules.
     * <ul>
     * <li>A single value of '*' means the OBJECTCLASS must be used </li>
     * <li>Any interface must be listed in the OBJECTCLASS</li>
     * </ul>
     * 
     * @param exportProperties the map of export properties
     * @return a list of exported interfaces names
     * @throws IllegalArgumentException if an interfaces is listed as export but it is not in the OBJECTCLASS.
     */
    private String[] getExportedInterfaces(Map<String, ?> exportProperties) {
        String[] providedInterfaces = getStringPlusValue(exportProperties.get(OBJECTCLASS));
        String[] exportedInterfaces = getStringPlusValue(exportProperties.get(SERVICE_EXPORTED_INTERFACES));
        if (exportedInterfaces.length == 1 && exportedInterfaces[0].equals("*")) {
            exportedInterfaces = providedInterfaces;
        }
        else {
            for (String exportedInterface : exportedInterfaces) {
                if ("*".equals(exportedInterface)) {
                    throw new IllegalArgumentException(
                        "Cannot accept wildcard together with other exported interfaces!");
                }
                boolean contained = false;
                for (String providedInterface : providedInterfaces) {
                    contained |= providedInterface.equals(exportedInterface);
                }
                if (!contained) {
                    logWarning("Exported interface %s not implemented by service: %s", exportedInterface,
                        providedInterfaces);
                    return new String[] {};
                }
            }
        }
        return exportedInterfaces;
    }
    
    
    /**
     * Returns an array exported intents based on the {@link SERVICE_EXPORTED_INTENTS} and {@link SERVICE_EXPORTED_INTENTS_EXTRA}<br>
     * property values as well as the default {@link HTTP_PASSBYVALYE_INTENT}.
     * 
     * @param properties the properties
     * @return an array of intents
     */
    private static String[] getExportedIntents(Map<String, Object> properties) {
        Object exportedIntents = properties.get(SERVICE_EXPORTED_INTENTS);
        Object exportedIntentsExtra = properties.get(SERVICE_EXPORTED_INTENTS_EXTRA);
        if (exportedIntents == null && exportedIntentsExtra == null) {
            return new String[] { PASSBYVALYE_INTENT };
        }
        Set<String> set = new HashSet<String>();
        if (exportedIntents != null) {
            for (String exportedIntent : getStringPlusValue(exportedIntents)) {
                set.add(exportedIntent);
            }
        }
        if (exportedIntentsExtra != null) {
            for (String exportedIntent : getStringPlusValue(exportedIntentsExtra)) {
                set.add(exportedIntent);
            }
        }
        set.add(PASSBYVALYE_INTENT);
        return set.toArray(new String[set.size()]);
    }

    /**
     * Returns a map of merged properties from the specified Service Reference and an optional map with
     * extra properties. Merging is done under the following rules:
     * <ul>
     * <li>Properties with a key starting with a '.' are private and thus ignored</li>
     * <li>Extra properties override service properties irrespective of casing</li>
     * </ul>
     * 
     * @param reference a Service Reference
     * @param extraProperties a map of extra properties, can be {@link null}
     * @return a map of merged properties
     */
    private static Map<String, Object> getMergedProperties(ServiceReference<?> reference, Map<String, ?> extraProperties) {
        Map<String, Object> serviceProperties = new HashMap<String, Object>();
        for (String propertyKey : reference.getPropertyKeys()) {
            if (propertyKey.startsWith(".")) {
                continue;
            }
            serviceProperties.put(propertyKey, reference.getProperty(propertyKey));
        }

        if (extraProperties == null) {
            return serviceProperties;
        }
        Set<String> removeServicePropertyKeys = new HashSet<String>();
        for (String extraPropertyKey : extraProperties.keySet()) {
            if (extraPropertyKey.startsWith(".") || extraPropertyKey.equalsIgnoreCase(SERVICE_ID)
                || extraPropertyKey.equalsIgnoreCase(OBJECTCLASS)) {
                continue;
            }

            for (String servicePropertyKey : serviceProperties.keySet()) {
                if (servicePropertyKey.equalsIgnoreCase(extraPropertyKey)) {
                    removeServicePropertyKeys.add(servicePropertyKey);
                }
            }
            for (String removeServicePropertyKey : removeServicePropertyKeys) {
                serviceProperties.remove(removeServicePropertyKey);
            }
            removeServicePropertyKeys.clear();
            serviceProperties.put(extraPropertyKey, extraProperties.get(extraPropertyKey));
        }
        return serviceProperties;
    }
    
    /**
     * Determines whether an array of intents is supported by this implementation.
     * 
     * @param exportedIntents the intents
     * @return {@code true} if supported, {@code false} otherwise.
     */
    private static boolean isExportedIntentsSupported(String[] exportedIntents) {
        if (exportedIntents == null) {
            return false;
        }
        for (String exportedIntent : exportedIntents) {
            if (!MY_SUPPORTED_INTENTS_SET.contains(exportedIntent)) {
                return false;
            }
        }
        return true;
    }    
    
}
