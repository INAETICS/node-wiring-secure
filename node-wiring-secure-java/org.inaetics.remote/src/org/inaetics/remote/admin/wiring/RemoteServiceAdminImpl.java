/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import static org.inaetics.remote.ServiceUtil.getFrameworkUUID;
import static org.inaetics.remote.admin.wiring.WiringAdminConstants.CONFIGURATION_TYPE;
import static org.osgi.service.remoteserviceadmin.EndpointPermission.IMPORT;
import static org.osgi.service.remoteserviceadmin.EndpointPermission.READ;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_REGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_UNREGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_UPDATE;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_REGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_UNREGISTRATION;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.IMPORT_UPDATE;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.inaetics.remote.AbstractComponentDelegate;
import org.inaetics.wiring.endpoint.WiringSender;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointPermission;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Remote Service Admin instance implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class RemoteServiceAdminImpl extends AbstractComponentDelegate implements RemoteServiceAdmin {

    private final Map<EndpointDescription, Set<ExportedEndpointImpl>> m_exportedEndpoints =
        new ConcurrentHashMap<EndpointDescription, Set<ExportedEndpointImpl>>();

    private final Map<EndpointDescription, Set<ImportedEndpointImpl>> m_importedEndpoints =
        new ConcurrentHashMap<EndpointDescription, Set<ImportedEndpointImpl>>();

    private final RemoteServiceAdminFactory m_manager;

    private final EndpointDescriptionBuilder m_endpointBuilder; 
    
    public RemoteServiceAdminImpl(RemoteServiceAdminFactory manager) {
        super(manager);
        m_manager = manager;
        m_endpointBuilder = new EndpointDescriptionBuilder(manager);
    }

    @Override
    protected void startComponentDelegate() throws Exception {
    }

    @Override
    protected void stopComponentDelegate() throws Exception {
    	synchronized (m_exportedEndpoints) {
    		for (Set<ExportedEndpointImpl> exportedEndpoints : m_exportedEndpoints.values()) {
    			for (ExportedEndpointImpl exportedEndpoint : exportedEndpoints) {
    				exportedEndpoint.close();
    			}
    		}
		}
    	synchronized (m_importedEndpoints) {
    		for (Set<ImportedEndpointImpl> importedEndpoints : m_importedEndpoints.values()) {
    			for (ImportedEndpointImpl importedEndpoint : importedEndpoints) {
    				importedEndpoint.close();
    			}
    		}
		}
    }

    @Override
    public Collection<ExportRegistration> exportService(final ServiceReference<?> reference,
        final Map<String, ?> properties) {

    	// TODO: where / how to check permission, the endpont description is created later now
    	
//        SecurityManager securityManager = System.getSecurityManager();
//        if (securityManager != null) {
//            securityManager.checkPermission(new EndpointPermission(endpoint, getFrameworkUUID(getBundleContext()),
//                EXPORT));
//        }

        return AccessController.doPrivileged(new PrivilegedAction<Collection<ExportRegistration>>() {

            @Override
            public Collection<ExportRegistration> run() {

                ExportedEndpointImpl exportedEndpoint = null;
                synchronized (m_exportedEndpoints) {
                	
                	// check if it's already exported, can happen when TP is restarted
                	Collection<Set<ExportedEndpointImpl>> endpointSets = m_exportedEndpoints.values();
                	for (Set<ExportedEndpointImpl> endpointSet : endpointSets) {
                		for (ExportedEndpointImpl endpoint : endpointSet) {
                			if (endpoint.getExportedService().equals(reference)) {
                				endpoint.close();
                			}
                		}
                	}
                	
                    exportedEndpoint =
                        new ExportedEndpointImpl(RemoteServiceAdminImpl.this, reference, properties, m_endpointBuilder);

                    if (exportedEndpoint == null || exportedEndpoint.getException() != null) {
                    	return Collections.emptyList();
                    }
                    
                    EndpointDescription endpoint = exportedEndpoint.getExportReference().getExportedEndpoint();
                    Set<ExportedEndpointImpl> exportedEndpoints = m_exportedEndpoints.get(endpoint);
                    if (exportedEndpoints == null) {
                    	exportedEndpoints = new HashSet<ExportedEndpointImpl>();
                    	m_exportedEndpoints.put(endpoint, exportedEndpoints);
                    }
                    
                    exportedEndpoints.add(exportedEndpoint);
                }
                m_manager.getEventsHandler().emitEvent(EXPORT_REGISTRATION, getBundleContext().getBundle(),
                    exportedEndpoint, exportedEndpoint.getException());
                return Collections.singletonList((ExportRegistration) exportedEndpoint);
            }
        });
    }

    @Override
    public ImportRegistration importService(final EndpointDescription endpoint) {

        if (endpoint == null) {
            logWarning("No valid endpoint specified. Ignoring...");
            return null;
        }

        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new EndpointPermission(endpoint,
                getFrameworkUUID(getBundleContext()), IMPORT));
        }

        if (!endpoint.getConfigurationTypes().contains(CONFIGURATION_TYPE)) {
            logInfo("No supported configuration type found. Not importing endpoint: %s", endpoint);
            return null;
        }

        if (!hasAvailableInterfaces(endpoint)) {
            logInfo("No available interfaces found. Not importing endpoint: %s", endpoint);
            return null;
        }

        if (!hasWireId(endpoint)) {
            logInfo("No wire id found. Not importing endpoint: %s", endpoint);
            return null;
        }

        return AccessController.doPrivileged(new PrivilegedAction<ImportRegistration>() {

            @Override
            public ImportRegistration run() {

            	String wirdeId = (String) endpoint.getProperties().get(WiringAdminConstants.WIRE_ID);
            	WiringSender wiringSender = m_manager.getWiringSender(wirdeId);
            	
            	if (wiringSender == null) {
            		logError("no wiring sender found for wire id %s", wirdeId);
            		return null;
            	}
            	
            	ImportedEndpointImpl importedEndpoint = null;
                synchronized (m_importedEndpoints) {
                    Set<ImportedEndpointImpl> importedEndpoints = m_importedEndpoints.get(endpoint);
                    if (importedEndpoints == null) {
                        importedEndpoints = new HashSet<ImportedEndpointImpl>();
                        m_importedEndpoints.put(endpoint, importedEndpoints);
                    }
                    importedEndpoint = new ImportedEndpointImpl(RemoteServiceAdminImpl.this, endpoint, wiringSender);
                    importedEndpoints.add(importedEndpoint);
                }
                getEventsHandler().emitEvent(IMPORT_REGISTRATION, getBundleContext().getBundle(), importedEndpoint,
                    importedEndpoint.getException());
                return importedEndpoint;
            }
        });
    }
    
	public void wiringSenderRemoved(String wireId) {
		//find and close endpoint for this wire
		synchronized (m_importedEndpoints) {
			Collection<Set<ImportedEndpointImpl>> endpointSets = m_importedEndpoints.values();
			for (Set<ImportedEndpointImpl> endpointSet : endpointSets) {
				for (ImportedEndpointImpl endpoint :endpointSet) {
					EndpointDescription description = endpoint.getImportedEndpoint();
					if (description != null) {
						Map<String, Object> properties = description.getProperties();
						if (properties != null) {
							String endpointWireId = (String) properties.get(WiringAdminConstants.WIRE_ID);
							if (endpointWireId != null) {
								if (endpointWireId.equals(wireId)) {
									endpoint.close();
								}
							}
							else {
								logWarning("wiringSenderRemoved: endpointWireId is null!");
							}
						}
						else {
							logWarning("wiringSenderRemoved: properties is null!");
						}
					}
					else {
						logWarning("wiringSenderRemoved: description is null!");
					}
				}
			}
		}
	}

    @Override
    public Collection<ImportReference> getImportedEndpoints() {

        return Collections.unmodifiableCollection(m_manager.getAllImportedEndpoints());
    }

    @Override
    public Collection<ExportReference> getExportedServices() {

        return Collections.unmodifiableCollection(m_manager.getAllExportedEndpoints());
    }

    void addExportedEndpoints(Collection<ExportReference> collection) {

        SecurityManager securityManager = System.getSecurityManager();
        String frameworkUUID = getFrameworkUUID(getBundleContext());
        synchronized (m_exportedEndpoints) {
            for (Entry<EndpointDescription, Set<ExportedEndpointImpl>> entry : m_exportedEndpoints.entrySet()) {
                try {
                    if (securityManager != null) {
                        securityManager.checkPermission(new EndpointPermission(entry.getKey(), frameworkUUID, READ));
                    }
                    collection.addAll(entry.getValue());
                }
                catch (SecurityException e) {}
            }
        }
    }

    void addImportedEndpoints(Collection<ImportReference> collection) {

        SecurityManager securityManager = System.getSecurityManager();
        String frameworkUUID = getFrameworkUUID(getBundleContext());
        synchronized (m_importedEndpoints) {
            for (Entry<EndpointDescription, Set<ImportedEndpointImpl>> entry : m_importedEndpoints.entrySet()) {
                try {
                    if (securityManager != null) {
                        securityManager.checkPermission(new EndpointPermission(entry.getKey(), frameworkUUID, READ));
                    }
                    collection.addAll(entry.getValue());
                }
                catch (SecurityException e) {}
            }
        }
    }

    void exportedEndpointUpdated(final ExportedEndpointImpl exportedEndpoint) {

        EndpointDescription endpoint = exportedEndpoint.getExportedEndpoint(true);
        synchronized (m_exportedEndpoints) {
            Set<ExportedEndpointImpl> exportedEndpoints = m_exportedEndpoints.get(endpoint);
            assert exportedEndpoints != null;
            if (exportedEndpoints != null) {
                boolean removed = exportedEndpoints.remove(exportedEndpoint);
                assert removed;
                boolean added = exportedEndpoints.add(exportedEndpoint);
                assert added;
            }
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                getEventsHandler().emitEvent(EXPORT_UPDATE, getBundleContext().getBundle(), exportedEndpoint,
                    exportedEndpoint.getException(true));
                return null;
            }
        });
    }

    void exportedEndpointClosed(final ExportedEndpointImpl exportedEndpoint) {

        EndpointDescription endpoint = exportedEndpoint.getExportedEndpoint(true);
        synchronized (m_exportedEndpoints) {
            Set<ExportedEndpointImpl> exportedEndpoints = m_exportedEndpoints.get(endpoint);
            assert exportedEndpoints != null;
            if (exportedEndpoints != null) {
                boolean removed = exportedEndpoints.remove(exportedEndpoint);
                assert removed;
                if (exportedEndpoints.isEmpty()) {
                    m_exportedEndpoints.remove(endpoint);
                }
            }
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                getEventsHandler().emitEvent(EXPORT_UNREGISTRATION, getBundleContext().getBundle(), exportedEndpoint,
                    exportedEndpoint.getException(true));
                return null;
            }
        });
    }

    void importedEndpointUpdated(final ImportedEndpointImpl importedEndpoint) {

        EndpointDescription endpoint = importedEndpoint.getImportedEndpoint(true);
        synchronized (m_importedEndpoints) {
            Set<ImportedEndpointImpl> importedEndpoints = m_importedEndpoints.get(endpoint);
            assert importedEndpoints != null;
            if (importedEndpoints != null) {
                boolean removed = importedEndpoints.remove(importedEndpoint);
                assert removed;
                boolean added = importedEndpoints.add(importedEndpoint);
                assert added;
            }
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                getEventsHandler().emitEvent(IMPORT_UPDATE, getBundleContext().getBundle(), importedEndpoint,
                    importedEndpoint.getException(true));
                return null;
            }
        });
    }

    void importedEndpointClosed(final ImportedEndpointImpl importedEndpoint) {

        EndpointDescription endpoint = importedEndpoint.getImportedEndpoint(true);
        synchronized (m_importedEndpoints) {
            Set<ImportedEndpointImpl> importedEndpoints = m_importedEndpoints.get(endpoint);
            assert importedEndpoints != null;
            if (importedEndpoints != null) {
                boolean removed = importedEndpoints.remove(importedEndpoint);
                assert removed;
                if (importedEndpoints.isEmpty()) {
                    m_importedEndpoints.remove(endpoint);
                }
            }
        }

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                getEventsHandler().emitEvent(IMPORT_UNREGISTRATION, getBundleContext().getBundle(), importedEndpoint,
                    importedEndpoint.getException(true));
                return null;
            }
        });
    }

    EventsHandlerImpl getEventsHandler() {
        return m_manager.getEventsHandler();
    }

    WiringServerEndpointHandler getServerEndpointHandler() {
        return m_manager.getServerEndpointHandler();
    }

    /**
     * Determines whether an {@link EndpointDescription} contains a wire id property.
     * 
     * @param endpoint the endpoint
     * @return {@code true} if valid, {@code false} otherwise.
     */
    private static boolean hasWireId(EndpointDescription endpoint) {
        Object wireId = endpoint.getProperties().get(WiringAdminConstants.WIRE_ID);
        if (wireId == null || !(wireId instanceof String)) {
            return false;
        }
        return true;
    }

    /**
     * Determines whether an {@link EndpointDescription} lists interfaces that are available to this
     * Remote Service Admin bundle.<br/><br/>
     * 
     * Note that the loading of classes effectively triggers dynamic imports, wiring this bundle to a
     * provider. Even though importing endpoints are registered using a {@link ServiceFactory} this seems
     * to be required for Equinox 3.10 to consider them to be assignable to the interfaces.
     * 
     * @param endpoint the endpoint
     * @return {@code true} if valid, {@code false} otherwise.
     */
    private static boolean hasAvailableInterfaces(EndpointDescription endpoint) {
        List<String> interfaces = endpoint.getInterfaces();
        if (interfaces == null || interfaces.isEmpty()) {
            return false;
        }
        try {
            for (String iface : interfaces) {
                RemoteServiceAdminImpl.class.getClassLoader().loadClass(iface);
            }
        }
        catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }
}
