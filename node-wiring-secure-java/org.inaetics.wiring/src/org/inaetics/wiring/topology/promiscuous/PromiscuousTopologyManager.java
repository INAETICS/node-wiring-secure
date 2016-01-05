/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.topology.promiscuous;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.inaetics.wiring.ExportRegistration;
import org.inaetics.wiring.ImportReference;
import org.inaetics.wiring.ImportRegistration;
import org.inaetics.wiring.WiringAdmin;
import org.inaetics.wiring.WiringAdminEvent;
import org.inaetics.wiring.WiringAdminListener;
import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.WiringEndpointEvent;
import org.inaetics.wiring.WiringEndpointEventListener;
import org.inaetics.wiring.base.AbstractWiringEndpointPublishingComponent;
import org.inaetics.wiring.endpoint.WiringConstants;
import org.inaetics.wiring.endpoint.WiringReceiver;
import org.inaetics.wiring.endpoint.WiringSender;
import org.inaetics.wiring.endpoint.WiringTopologyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * {@link PromiscuousTopologyManager} implements a <i>Topology Manager</i> with of a promiscuous strategy. It will import
 * any discovered remote endpoints and export any locally available endpoints.<p>
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class PromiscuousTopologyManager extends AbstractWiringEndpointPublishingComponent implements
    WiringAdminListener, WiringEndpointEventListener, ManagedService, WiringTopologyManager {

    public final static String SERVICE_PID = "org.amdatu.remote.topology.promiscuous";

    private final Set<WiringReceiver> m_exportableReceivers = Collections.newSetFromMap(new ConcurrentHashMap<WiringReceiver, Boolean>());
    private final Map<WiringReceiver, Map<WiringAdmin, ExportRegistration>> m_exportedReceivers =
            new ConcurrentHashMap<WiringReceiver, Map<WiringAdmin, ExportRegistration>>();

    private final Set<WiringEndpointDescription> m_importableEndpoints = Collections.newSetFromMap(new ConcurrentHashMap<WiringEndpointDescription, Boolean>());
    private final Map<WiringEndpointDescription, Map<WiringAdmin, ImportRegistration>> m_importedEndpoints =
        new ConcurrentHashMap<WiringEndpointDescription, Map<WiringAdmin, ImportRegistration>>();
    
    private final Map<ImportRegistration, Component> m_registeredSenders =
            new ConcurrentHashMap<ImportRegistration, Component>();

    private final Set<WiringAdmin> m_wiringAdmins = Collections.newSetFromMap(new ConcurrentHashMap<WiringAdmin, Boolean>());

	private volatile BundleContext m_context;

	private DependencyManager m_manager;

    public PromiscuousTopologyManager(DependencyManager manager) {
        super("topology", "promiscuous");
        m_manager = manager;
    }

    @Override
    public void updated(Dictionary<String, ?> configuration) throws ConfigurationException {

    	// TODO use filters as in RSA TM ?
    
    }

    // Dependency Manager callback method
    public void wiringAdminAdded(ServiceReference<WiringAdmin> reference, WiringAdmin admin) {
    	m_wiringAdmins.add(admin);
    	exportEndpoints(admin);
    	importEndpoints(admin);
    }
    
    // Dependency Manager callback method
    public void wiringAdminRemoved(ServiceReference<WiringAdmin> reference, WiringAdmin admin) {
    	m_wiringAdmins.remove(admin);
    	unExportEndpoints(admin);
    	unImportEndpoints(admin);
    }
    
    // Dependency Manager callback method
    public void wiringReceiverAdded(ServiceReference<WiringReceiver> reference, WiringReceiver receiver) {
    	m_exportableReceivers.add(receiver);
    	exportEndpoints(receiver);
    }

    // Dependency Manager callback method
    public void wiringReceiverRemoved(ServiceReference<WiringReceiver> reference, WiringReceiver receiver) {
    	m_exportableReceivers.remove(receiver);
    	unExportEndpoints(receiver);
    }
    
	@Override
	public void endpointChanged(WiringEndpointEvent event) {
		switch (event.getType()) {
			case WiringEndpointEvent.ADDED:
				importEndpoint(event.getEndpoint());
				break;
			case WiringEndpointEvent.REMOVED:
				unImportEndpoint(event.getEndpoint());
				break;
			default:
				logError("unknown wiring endpoint event type: %s", event.getType());
		}
	}

	@Override
	public void wiringAdminEvent(final WiringAdminEvent event) {
        executeTask(new Runnable() {
            @Override
            public void run() {

                switch (event.getType()) {
                    case WiringAdminEvent.EXPORT_ERROR: {
                        ExportRegistration registration = event.getExportRegistration();
                        unExport(registration);
                        break;
                    }
                    case WiringAdminEvent.IMPORT_ERROR: {
                        ImportRegistration registration = event.getImportRegistration();
                        unImportEndpoint(registration);
                        break;
                    }
                    default:
                        break;
                }
            }
        });
	}

	private void exportEndpoints(WiringAdmin admin) {
		for (WiringReceiver wiringReceiver : m_exportableReceivers) {
    		exportEndpoint(admin, wiringReceiver);
		}
	}
	
	private void exportEndpoints(WiringReceiver receiver) {
		for (WiringAdmin admin : m_wiringAdmins) {
			exportEndpoint(admin, receiver);
		}
	}

	private void exportEndpoint(WiringAdmin admin, WiringReceiver receiver) {

		// export wiring receiver
		ExportRegistration exportRegistration = admin.exportEndpoint(receiver);
		Map<WiringAdmin, ExportRegistration> adminMap = m_exportedReceivers.get(receiver);
		if (adminMap == null) {
			adminMap = new HashMap<WiringAdmin, ExportRegistration>();
			m_exportedReceivers.put(receiver, adminMap);
		}
		adminMap.put(admin, exportRegistration);
		
		WiringEndpointDescription endpointDescription = exportRegistration.getExportReference().getEndpointDescription();
		
		// notify endpoint listeners
		endpointAdded(endpointDescription);
		
		// notify receiver
		receiver.wiringEndpointAdded(endpointDescription.getId());
	}
	
	private void importEndpoints(WiringAdmin admin) {
    	for (WiringEndpointDescription endpointDescription : m_importableEndpoints) {
    		importEndpoint(admin, endpointDescription);
		}
	}

	private void importEndpoint(WiringEndpointDescription endpointDescription) {
		m_importableEndpoints.add(endpointDescription);
		for (WiringAdmin admin : m_wiringAdmins) {
			importEndpoint(admin, endpointDescription);
		}
	}
	
	private void importEndpoint(WiringAdmin admin, WiringEndpointDescription endpointDescription) {
		
		// import endpoints
	    ImportRegistration importRegistration = admin.importEndpoint(endpointDescription);
	    
	    if (importRegistration != null) {
			Map<WiringAdmin, ImportRegistration> adminMap = m_importedEndpoints.get(endpointDescription);
			if (adminMap == null) {
				adminMap = new HashMap<WiringAdmin, ImportRegistration>();
				m_importedEndpoints.put(endpointDescription, adminMap);
			}
			adminMap.put(admin, importRegistration);
			registerService(importRegistration);
	    }
	}
	
	private void registerService(ImportRegistration registration) {

		ImportReference importReference = registration.getImportReference();
		WiringEndpointDescription endpointDescription = importReference.getEndpointDescription();
		WiringSender wiringSender = importReference.getWiringSender();
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(WiringConstants.PROPERTY_ZONE_ID, endpointDescription.getZone());
        properties.put(WiringConstants.PROPERTY_NODE_ID, endpointDescription.getNode());
        properties.put(WiringConstants.PROPERTY_WIRE_ID, endpointDescription.getId());
        String secureDescription = endpointDescription.getProperty(WiringConstants.PROPERTY_SECURE);
        String secureProperty = secureDescription != null ? secureDescription : "no";
        properties.put(WiringConstants.PROPERTY_SECURE, secureProperty);

        Component wiringSenderComponent = m_manager.createComponent()
        	.setInterface(WiringSender.class.getName(), properties)
        	.setImplementation(wiringSender);
        m_manager.add(wiringSenderComponent);
        m_registeredSenders.put(registration, wiringSenderComponent);
	}
	
	private void unExportEndpoints(WiringAdmin admin) {
		
		// close and remove registration, notify endpoint listeners
		Collection<Map<WiringAdmin, ExportRegistration>> adminMaps = m_exportedReceivers.values();
		for (Map<WiringAdmin, ExportRegistration> adminMap : adminMaps) {
			ExportRegistration exportRegistration = adminMap.remove(admin);
			if (exportRegistration != null) {

				// notify receiver
				WiringReceiver wiringReceiver = exportRegistration.getExportReference().getWiringReceiver();
				WiringEndpointDescription endpointDescription = exportRegistration.getExportReference().getEndpointDescription();
				wiringReceiver.wiringEndpointRemoved(endpointDescription.getId());
				
				unExport(exportRegistration);
			}
		}
	}

	private void unExportEndpoints(WiringReceiver listener) {
		
		// close and remove registration, notify endpoint listeners
		Map<WiringAdmin, ExportRegistration> adminMap = m_exportedReceivers.remove(listener);
		Collection<ExportRegistration> registrations = adminMap.values();
		for (ExportRegistration registration : registrations) {
			unExport(registration);
		}
	}
	
	private void unExport(ExportRegistration registration) {
		endpointRemoved(registration.getExportReference().getEndpointDescription());
		registration.close();
	}
	
	private void unImportEndpoints(WiringAdmin admin) {
		
		// close and remove registration
		Collection<Map<WiringAdmin, ImportRegistration>> adminMaps = m_importedEndpoints.values();
		for (Map<WiringAdmin, ImportRegistration> adminMap : adminMaps) {
			ImportRegistration importRegistration = adminMap.remove(admin);
			if (importRegistration != null) {
				importRegistration.close();
			}
		}
	}
	
	private void unImportEndpoint(WiringEndpointDescription endpointDescription) {
		logInfo("unimport wiring endpoint %s", endpointDescription.getId());
		m_importableEndpoints.remove(endpointDescription);
		Map<WiringAdmin, ImportRegistration> adminMap = m_importedEndpoints.remove(endpointDescription);
		Collection<ImportRegistration> registrations = adminMap.values();
		for (ImportRegistration registration : registrations) {
			unImportEndpoint(registration);
		}
	}
	
	private void unImportEndpoint(ImportRegistration registration) {
		logInfo("unimport registration %s", registration.getImportReference().getEndpointDescription().getId());
		unregisterService(registration);
		registration.close();
	}

	private void unregisterService(ImportRegistration registration) {
		Component component = m_registeredSenders.get(registration);
		logInfo("unregistering WiringsSender %s", component.getService());
		m_manager.remove(component);
		m_registeredSenders.remove(registration);
	}
}
