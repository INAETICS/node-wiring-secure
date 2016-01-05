/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

import org.osgi.framework.Bundle;

/**
 * Provides the event information for a Wiring Admin event.
 * 
 * @Immutable
 */
public class WiringAdminEvent {
	/**
	 * Add an import registration. 
	 */
	public static final int			IMPORT_REGISTRATION		= 1;

	/**
	 * Add an export registration.
	 */
	public static final int			EXPORT_REGISTRATION		= 2;

	/**
	 * Remove an export registration. 
	 */
	public static final int			EXPORT_UNREGISTRATION	= 3;

	/**
	 * Remove an import registration. 
	 */
	public static final int			IMPORT_UNREGISTRATION	= 4;

	/**
	 * A fatal importing error occurred. The Import Registration has been
	 * closed.
	 */
	public static final int			IMPORT_ERROR			= 5;

	/**
	 * A fatal exporting error occurred. The Export Registration has been
	 * closed.
	 */
	public static final int			EXPORT_ERROR			= 6;

	/**
	 * A problematic situation occurred, the export is still active.
	 */
	public static final int			EXPORT_WARNING			= 7;
	/**
	 * A problematic situation occurred, the import is still active.
	 */
	public static final int			IMPORT_WARNING			= 8;

	private final ImportRegistration	importRegistration;
	private final ExportRegistration	exportRegistration;
	private final Throwable			exception;
	private final int				type;
	private final Bundle			source;

	/**
	 * Private constructor.
	 * 
	 * @param type The event type
	 * @param source The source bundle, must not be {@code null}.
	 * @param importRegistration
	 * @param exportReference
	 * @param exception Any exceptions encountered, can be {@code null}
	 */
	public WiringAdminEvent(int type, Bundle source, ImportRegistration importRegistration, ExportRegistration exportRegistration, Throwable exception) {
		this.type = type;
		this.source = source;
		this.importRegistration = importRegistration;
		this.exportRegistration = exportRegistration;
		this.exception = exception;
	}

	/**
	 * Return the Import Registration for this event.
	 * 
	 * @return The Import Registration or {@code null}.
	 */
	public ImportRegistration getImportRegistration() {
		return importRegistration;
	}

	/**
	 * Return the Export Registration for this event.
	 * 
	 * @return The Export Registration or {@code null}.
	 */
	public ExportRegistration getExportRegistration() {
		return exportRegistration;
	}

	/**
	 * Return the exception for this event.
	 * 
	 * @return The exception or {@code null}.
	 */
	public Throwable getException() {
		return exception;
	}

	/**
	 * Return the type of this event.
	 * 
	 * @return The type of this event.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Return the bundle source of this event.
	 * 
	 * @return The bundle source of this event.
	 */
	public Bundle getSource() {
		return source;
	}
}
