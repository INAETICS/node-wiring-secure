/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

/**
 * A {@link WiringAdminEvent} listener is notified synchronously of any
 * export or import registrations and unregistrations.
 * 
 * @see WiringAdminEvent
 * @ThreadSafe
 */
public interface WiringAdminListener {
	/**
	 * Receive notification of any export or import registrations and
	 * unregistrations as well as errors and warnings.
	 * 
	 * @param event The {@link WiringAdminEvent} object.
	 */
	void wiringAdminEvent(WiringAdminEvent event);
}
