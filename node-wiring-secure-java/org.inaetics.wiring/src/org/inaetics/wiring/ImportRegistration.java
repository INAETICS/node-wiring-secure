/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.wiring;

public interface ImportRegistration {

	/**
	 * Return the Import Reference for the imported service.
	 * 
	 * @return The Import Reference for this registration, or <code>null</code>
	 *         if this Import Registration is closed.
	 * @throws IllegalStateException When this registration was not properly
	 *         initialized. See {@link #getException()}.
	 */
	ImportReference getImportReference();

	/**
	 * Close this Import Registration. This must close the connection to the
	 * endpoint and unregister the proxy. After this method returns, all other
	 * methods must return {@code null}.
	 * 
	 * This method has no effect when this registration has already been closed
	 * or is being closed.
	 */
	void close();

	/**
	 * Return the exception for any error during the import process.
	 * 
	 * If the Wiring Admin for some reasons is unable to properly
	 * initialize this registration, then it must return an exception from this
	 * method. If no error occurred, this method must return {@code null}.
	 * 
	 * The error must be set before this Import Registration is returned.
	 * Asynchronously occurring errors must be reported to the log.
	 * 
	 * @return The exception that occurred during the initialization of this
	 *         registration or {@code null} if no exception occurred.
	 */
	Throwable getException();	
}
