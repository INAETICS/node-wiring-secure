/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author msluiter
 *
 */
public class WiringEndpointDescription {
	
	private String m_id;
	private String m_zone;
	private String m_node;
	private String m_protocolName;
	private volatile Map<String, String> m_properties = new ConcurrentHashMap<String, String>();
	
	public WiringEndpointDescription() {
	}
	
	public String getId() {
		if (m_id == null) {
			m_id = getCalculatedId();
		}
		return m_id;
	}
	
	private String getCalculatedId() {
		return UUID.randomUUID().toString();
	}
	
	public void setId(String id) {
		m_id = id;
	}
	
	public String getZone() {
		return m_zone;
	}

	public void setZone(String zone) {
		this.m_zone = zone;
	}

	public String getNode() {
		return m_node;
	}

	public void setNode(String node) {
		this.m_node = node;
	}

	public String getProtocolName() {
		return m_protocolName;
	}

	public void setProtocolName(String protocolName) {
		this.m_protocolName = protocolName;
	}

	public String getProperty(String key) {
		return m_properties.get(key);
	}
	
	public void setProperty(String key, String value) {
		m_properties.put(key, value);
	}
	
	public Map<String, String> getProperties() {
		return m_properties;
	}
	
	public void setProperties(Map<String, String> properties) {
		m_properties.clear();
		m_properties.putAll(properties);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_id == null) ? 0 : m_id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WiringEndpointDescription other = (WiringEndpointDescription) obj;
		if (m_id == null) {
			if (other.m_id != null)
				return false;
		} else if (!m_id.equals(other.m_id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "WiringEndpointDescription [m_zone=" + m_zone + ", m_node="
				+ m_node + ", m_id=" + m_id + ", m_protocolName="
				+ m_protocolName
				+ ", m_properties=" + m_properties + "]";
	}
		
}
