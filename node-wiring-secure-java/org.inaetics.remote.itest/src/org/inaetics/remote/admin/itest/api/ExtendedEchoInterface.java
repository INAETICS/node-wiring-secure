/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.itest.api;

/**
 * Extended version of the {@link EchoInterface}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface ExtendedEchoInterface extends EchoInterface {

    String shout(String message);
}
