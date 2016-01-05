/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

/**
 * An Wiring Endpoint Event.
 * <p/>
 * 
 * {@code WiringEndpointEvent} objects are delivered to all registered
 * {@link WiringEndpointEventListener} services.
 * <p/>
 * 
 * A type code is used to identify the type of event. The following event types
 * are defined:
 * <ul>
 * <li>{@link #ADDED}</li>
 * <li>{@link #REMOVED}</li>
 * </ul>
 * Additional event types may be defined in the future.
 * <p/>
 * 
 * @see WiringEndpointEventListener
 * @Immutable
 * @since 1.1
 */
public class WiringEndpointEvent {
	/**
	 * An endpoint has been added.
	 * <p/>
	 * 
	 * This {@code WiringEndpointEvent} type indicates that a new endpoint has been
	 * added. The endpoint is represented by the associated
	 * {@link WiringEndpointDescription} object.
	 */
	public static final int				ADDED				= 0x00000001;

	/**
	 * An endpoint has been removed.
	 * <p/>
	 * 
	 * This {@code WiringEndpointEvent} type indicates that an endpoint has been
	 * removed. The endpoint is represented by the associated
	 * {@link WiringEndpointDescription} object.
	 */
	public static final int				REMOVED				= 0x00000002;

	/**
	 * Reference to the associated endpoint description.
	 */
	private final WiringEndpointDescription	endpoint;

	/**
	 * Type of the event.
	 */
	private final int					type;

	/**
	 * Constructs a {@code WiringEndpointEvent} object from the given arguments.
	 * 
	 * @param type The event type. See {@link #getType()}.
	 * @param endpoint The endpoint associated with the event.
	 */
	public WiringEndpointEvent(int type, WiringEndpointDescription endpoint) {
		this.endpoint = endpoint;
		this.type = type;
	}

	/**
	 * Return the endpoint associated with this event.
	 * 
	 * @return The endpoint associated with the event.
	 */
	public WiringEndpointDescription getEndpoint() {
		return endpoint;
	}

	/**
	 * Return the type of this event.
	 * <p/>
	 * The type values are:
	 * <ul>
	 * <li>{@link #ADDED}</li>
	 * <li>{@link #REMOVED}</li>
	 * </ul>
	 * 
	 * @return The type of this event.
	 */
	public int getType() {
		return type;
	}
}
