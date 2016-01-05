/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.endpoint;

public interface WiringConstants {
	
	/**
	 * the zone of the endpoint 
	 */
	public static final String PROPERTY_ZONE_ID = "inaetics.wiring.zoneid";

	/**
	 * the node of the endpoint 
	 */
	public static final String PROPERTY_NODE_ID = "inaetics.wiring.nodeid";
	
	/**
	 * the name of the endpoint 
	 */
	public static final String PROPERTY_WIRE_ID = "inaetics.wiring.id";
	
	/**
	 * the security level of the wire. Possible values: "yes", "no" 
	 */
	public static final String PROPERTY_SECURE = "inaetics.wiring.secure";
	
}
