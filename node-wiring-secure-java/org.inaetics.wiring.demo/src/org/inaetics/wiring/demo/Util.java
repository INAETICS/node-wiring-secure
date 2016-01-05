/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring.demo;

import java.util.Collection;

import org.inaetics.wiring.endpoint.WiringConstants;
import org.inaetics.wiring.endpoint.WiringSender;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class Util {
	
	public static WiringSender getWiringSender(BundleContext context, String wireId) throws InvalidSyntaxException {

		String filterString = "(&";
		filterString += "(" + Constants.OBJECTCLASS + "=" + WiringSender.class.getName() + ")";
		filterString += "(" + WiringConstants.PROPERTY_WIRE_ID + "=" + wireId + ")";
		filterString += ")";
		
		Collection<ServiceReference<WiringSender>> senderReferences = context.getServiceReferences(WiringSender.class, filterString);
		if(senderReferences.size() == 0) {
			return null;
		}
		
		ServiceReference<WiringSender> senderReference = senderReferences.iterator().next();
		WiringSender sender = context.getService(senderReference);
		
		return sender;
		
	}

}
