/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.discovery.etcd;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import mousio.client.promises.ResponsePromise;
import mousio.client.promises.ResponsePromise.IsSimplePromiseResponseHandler;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeyAction;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;

import org.inaetics.wiring.WiringEndpointDescription;
import org.inaetics.wiring.discovery.AbstractDiscovery;

/**
 * Etcd implementation of service node based discovery.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class EtcdNodeDiscovery extends AbstractDiscovery {

	public static final String DISCOVERY_NAME = "Amdatu Wiring Node Discovery (Etcd)";
    public static final String DISCOVERY_TYPE = "etcd";

    private static final String PROPERTY_ID = "inaetics.wiring.id";
    private static final String PROPERTY_PROTOCOL_NAME = "inaetics.wiring.config";
    
    private static final String PATH_SEP = "/";
    private static final String PROP_SEP = "\n";
    private static final String PROP_ASSIGN = "=";

    private final EtcdDiscoveryConfiguration m_configuration;

    private volatile ScheduledExecutorService m_executor;
    private volatile ResponseListener m_responseListener;

    private volatile EtcdRegistrationUpdater m_updater;
    private volatile EtcdClient m_etcd;
    
    private final WiringEndpointDescription m_localEndpoint = new WiringEndpointDescription();
    
    private final Map<String, WiringEndpointDescription> m_publishedEndpoints = new HashMap<String, WiringEndpointDescription>();
    private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();
    
    public EtcdNodeDiscovery(EtcdDiscoveryConfiguration configuration) {
        super(DISCOVERY_TYPE, configuration);
        m_configuration = configuration;
    }

    @Override
    protected void startComponent() throws Exception {
        super.startComponent();

        m_executor = Executors.newSingleThreadScheduledExecutor();
        m_responseListener = new ResponseListener();

        logDebug("Connecting to %s", m_configuration.getConnectUrl());
        m_etcd = new EtcdClient(URI.create(m_configuration.getConnectUrl()));
        logDebug("Etcd version is %s", m_etcd.getVersion());
        m_updater = new EtcdRegistrationUpdater();

        // set local node properties (without enpoints)
    	m_localEndpoint.setZone(m_configuration.getZone());
    	m_localEndpoint.setNode(m_configuration.getNode());

        discoverEndpoints();
    }

    @Override
    protected void stopComponent() throws Exception {

        try {
            m_updater.cancel();
        }
        catch (Exception e) {
            logError("cancel updater failed", e);
        }

        try {
            m_etcd.close();
        }
        catch (Exception e) {
            logError("closing etcd client failed", e);
        }

        m_executor.shutdown();
        m_executor = null;

        super.stopComponent();
    }

    private void discoverEndpoints() throws Exception {
        long index = 0l;
        try {
        	
        	// create dirs if not available yet...
        	String rootPath = m_configuration.getRootPath();
        	
        	try {
				m_etcd.putDir(rootPath).send().get();
			} catch (Exception e) {
				// nothing to do, directory exists already
			}
        	
            EtcdKeysResponse response = m_etcd.getDir(rootPath).recursive().send().get();
            index = getEtcdIndex(response);
            logDebug("discovering endpoints at etcd index %s", index);
            
        	try {
	            if (response.node.dir && response.node.nodes != null) {
	                List<WiringEndpointDescription> nodes = getWiringEndpointDescriptions(response);
	                setDiscoveredEndpoints(nodes);
	            }
        	}
        	catch (Exception e) {
				logWarning("Failed to set discovered endpoint(s)", e);
			}
            
        }
        catch (EtcdException e) {
            logError("Could not discovery endpoints!", e);
        }
        finally {
            setDirectoryWatch(index + 1);
        }
    }

    private List<WiringEndpointDescription> getWiringEndpointDescriptions(EtcdKeysResponse response) {
    	
    	List<WiringEndpointDescription> endpoints = new ArrayList<>();
    	
        // zones
    	for (EtcdNode zoneNode : response.node.nodes) {
    		if(zoneNode.dir && zoneNode.nodes != null) {

    			// nodes
            	for (EtcdNode nodeNode : zoneNode.nodes) {
            		if(nodeNode.dir && nodeNode.nodes != null) {
            	
            			// wiring endpoints
                    	for (EtcdNode endpointNode : nodeNode.nodes) {
                    		if (endpointNode.value != null) {
                        		WiringEndpointDescription endpoint = getEndpointFromNode(endpointNode, true);
                        		endpoints.add(endpoint);
                    		}
                    	}
            		}
            	}
    		}
    	}
    	
    	return endpoints;
    }
    
    private void handleDiscoveryNodeChange(EtcdKeysResponse response) throws Exception {

    	long index = 0l;
        try {
            index = response.node.modifiedIndex;
            logInfo("Handling endpoint change at etcd index %s, action %s, key %s", index, response.action.toString(), response.node.key);
            
            // new / updated node
            if (response.action == EtcdKeyAction.set || response.action == EtcdKeyAction.create || response.action == EtcdKeyAction.update) {

            	WiringEndpointDescription endpoint = getEndpointFromNode(response.node, true);
                addDiscoveredEndpoint(endpoint);

            }
            
            // remove node on "delete" or "expire"
            else if ((response.action == EtcdKeyAction.delete || response.action == EtcdKeyAction.expire)) {

            	WiringEndpointDescription endpoint = getEndpointFromNode(response.node, false);
                removeDiscoveredEndpoint(endpoint);

            }
        }
        catch (Exception e) {
            logError("Could not handle endpoint change!", e);
        }
        finally {
            setDirectoryWatch(index + 1);
        }
    }
    
    private WiringEndpointDescription getEndpointFromNode(EtcdNode etcdNode, boolean doGetEndpointProperties) {

    	String all = etcdNode.key.substring(m_configuration.getRootPath().length());
    	if (all.startsWith(PATH_SEP)) {
    		all = all.substring(1);
    	}
    	
    	WiringEndpointDescription endpoint = new WiringEndpointDescription();
    	
    	// zone
    	String zone = getNextPart(all); 
    	endpoint.setZone(zone);
    	all = all.substring(zone.length() + 1);

    	// node
    	String node = getNextPart(all); 
    	endpoint.setNode(node);
    	all = all.substring(node.length() + 1);
    	
    	// id
    	String id = getNextPart(all); 
    	endpoint.setId(id);

    	if(!doGetEndpointProperties) {
    		return endpoint;
    	}

    	Map<String, String> properties = parseEtcdNodeValueToProperties(etcdNode);
    	Set<String> keySet = properties.keySet();
    	for (String key : keySet) {
			switch(key) {
				case PROPERTY_PROTOCOL_NAME: endpoint.setProtocolName(properties.get(key)); break;
				default: endpoint.setProperty(key,properties.get(key));
			}
		}
    	
    	return endpoint;
    }
    
    private String getNextPart(String s) {
    	return s.contains(PATH_SEP) ? s.substring(0, s.indexOf(PATH_SEP)) : s;
    }

    private Map<String, String> parseEtcdNodeValueToProperties(EtcdNode node) {
    	Map<String, String> map = new HashMap<String, String>();
    	String[] properties = node.value.split(PROP_SEP);
    	for (String property : properties) {
			String[] propertyParts = property.split(PROP_ASSIGN);
			map.put(propertyParts[0], propertyParts[1]);
		}
    	return map;
    }
    
    private long getEtcdIndex(EtcdKeysResponse response) {

        long index = 0l;
        if (response != null) {
            // get etcdIndex with fallback to modifiedIndex
            // see https://github.com/coreos/etcd/pull/1082#issuecomment-56444616
            if (response.etcdIndex != null) {
                index = response.etcdIndex;
            }
            else if (response.node.modifiedIndex != null) {
                index = response.node.modifiedIndex;
            }
            // potential bug fallback
            // see https://groups.google.com/forum/?hl=en#!topic/etcd-dev/S12405PCKaU
            if (response.node.dir && response.node.nodes != null) {
                for (EtcdNode node : response.node.nodes) {
                    if (node.modifiedIndex > index) {
                        index = node.modifiedIndex;
                    }
                }
            }
        }
        return index;
    }

    private void setDirectoryWatch(long index) {

        logDebug("Setting watch for index %s", index);
        try {
            m_etcd.get(m_configuration.getRootPath())
                .waitForChange((int) index)
                .recursive()
                .send()
                .addListener(m_responseListener);
        }
        catch (IOException e) {
            // TODO How do we recover from this?
            logError("Failed to set new watch on discovery directory!", e);
        }
    }

    private String getRootPath() {
    	String rootPath = m_configuration.getRootPath();
    	if (!rootPath.endsWith("/")) {
    		rootPath += "/";
    	}
    	return rootPath;
    }

    private String getZonePath(WiringEndpointDescription endpoint) {
    	return getRootPath() + endpoint.getZone() + "/";
    }
    
    private String getNodePath(WiringEndpointDescription endpoint) {
    	return getZonePath(endpoint) + endpoint.getNode() + "/";
    }

    private String getEndpointPath(WiringEndpointDescription endpoint) {
    	return getNodePath(endpoint) + endpoint.getId() + "/";
    }

    private class EtcdRegistrationUpdater implements Runnable {

        private static final int ETCD_REGISTRATION_TTL = 30;

        private final ScheduledFuture<?> m_future;

        public EtcdRegistrationUpdater() throws Exception {
            m_future =
                m_executor.scheduleAtFixedRate(this, 0, ETCD_REGISTRATION_TTL - 10,
                    TimeUnit.SECONDS);
        }

        private void putPublishedEndpoints() throws Exception {

        	m_lock.readLock().lock();
        	
        	for (WiringEndpointDescription endpoint : m_publishedEndpoints.values()) {
        		putPublishedEndpoint(endpoint, false);
        	}
        	
        	m_lock.readLock().unlock();
        }

		public void putPublishedEndpoint(WiringEndpointDescription endpoint, boolean isAdded) throws Exception {
			
			String key = getEndpointPath(endpoint);
			String value = getEndpointValue(endpoint);
			
        	m_etcd.put(key, value).prevExist(!isAdded).ttl(ETCD_REGISTRATION_TTL).send();
        }

		private String getEndpointValue(WiringEndpointDescription endpoint) {
			
			String value = addProperty("", PROPERTY_PROTOCOL_NAME, endpoint.getProtocolName());
			
			Map<String, String> properties = endpoint.getProperties();
			Set<String> keys = properties.keySet();
			for (String key : keys) {
				value = addProperty(value, key, properties.get(key));
			}
			
			return value;
		}
		
		private String addProperty(String s, String key, String value) {
			return s + key + PROP_ASSIGN + value + PROP_SEP;
		}
		
		@Override
        public void run() {
            try {
            	putPublishedEndpoints();
            }
            catch (Exception e) {
                logError("Etcd registration update failed", e);
            }
        }

        public void cancel() {
            try {
                m_future.cancel(false);
                deleteLocalEndpoints();
            }
            catch (Exception e) {
                logError("Etcd deregistration update failed", e);
            }
        }
        
        private void deleteLocalEndpoints() throws Exception {
        	m_lock.readLock().lock();
        	for (WiringEndpointDescription endpoint : m_publishedEndpoints.values()) {
        		deleteEndpoint(endpoint);
        	}
        	m_lock.readLock().unlock();
        }
        
        public void deleteEndpoint(WiringEndpointDescription endpoint) throws Exception {
        	m_etcd.delete(getEndpointPath(endpoint)).send();
        }
        
    }

    private class ResponseListener implements IsSimplePromiseResponseHandler<EtcdKeysResponse> {

		@Override
		public void onResponse(ResponsePromise<EtcdKeysResponse> promise) {
			try {
				if (promise.getException() != null) {
					logWarning("etcd watch received exception: %s", promise.getException().getMessage());
					discoverEndpoints();
					return;
				}
				handleDiscoveryNodeChange(promise.get());
			} catch (Exception e) {
				logWarning("Could not get node(s)", e);
			}
		}
    }

	@Override
	protected void addPublishedEndpoint(WiringEndpointDescription endpoint) {
		m_lock.writeLock().lock();
		m_publishedEndpoints.put(endpoint.getId(), endpoint);
		try {
			m_updater.putPublishedEndpoint(endpoint, true);
		} catch (Exception e) {
			logError("error publishing endpoint %s", e, endpoint);
		}
		m_lock.writeLock().unlock();
	}

	@Override
	protected void removePublishedEndpoint(WiringEndpointDescription endpoint) {
		m_lock.writeLock().lock();
		m_publishedEndpoints.remove(endpoint.getId());
		try {
			m_updater.deleteEndpoint(endpoint);
		} catch (Exception e) {
			logError("error unpublishing endpoint %s", e, endpoint);
		}
		m_lock.writeLock().unlock();
	}

}
