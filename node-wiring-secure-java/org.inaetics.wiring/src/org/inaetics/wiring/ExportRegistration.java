/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

public interface ExportRegistration {
	/**
	 * Return the Export Reference for the exported service.
	 * 
	 * @return The Export Reference for this registration, or <code>null</code>
	 *         if this Export Registration is closed.
	 * @throws IllegalStateException When this registration was not properly
	 *         initialized. See {@link #getException()}.
	 */
	ExportReference getExportReference();

	/**
	 * Delete the local endpoint and close all resources.
	 * After this method returns, all methods must return
	 * {@code null}.
	 * 
	 * This method has no effect when this registration has already been closed
	 * or is being closed.
	 */
	void close();

	/**
	 * Return the exception for any error during the export process.
	 * 
	 * If the Wiring Admin for some reasons is unable to properly
	 * initialize this registration, then it must return an exception from this
	 * method. If no error occurred, this method must return {@code null}.
	 * 
	 * The error must be set before this Export Registration is returned.
	 * Asynchronously occurring errors must be reported to the log.
	 * 
	 * @return The exception that occurred during the initialization of this
	 *         registration or {@code null} if no exception occurred.
	 */
	Throwable getException();
}
