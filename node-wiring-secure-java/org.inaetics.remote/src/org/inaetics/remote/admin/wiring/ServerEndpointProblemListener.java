/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

/**
 * Provides an abstraction for server endpoints to report problems.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface ServerEndpointProblemListener {

    void handleEndpointError(Throwable exception);

    void handleEndpointWarning(Throwable exception);
}
