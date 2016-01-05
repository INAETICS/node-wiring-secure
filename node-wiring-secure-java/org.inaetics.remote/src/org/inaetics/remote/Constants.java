/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote;

import org.osgi.service.log.LogService;

/**
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class Constants {

    /**
     * Property key prefix for the log level. Default is {@link LogService#LOG_INFO}.
     */
    public final static String LOGGING_PROP_PRE = "amdatu.remote.logging";

    /**
     * Property key prefix for the console level. Default is {@link LogService#LOG_ERROR} - 1.
     */
    public final static String CONSOLE_PROP_PRE = "amdatu.remote.console";

    /**
     * Manifest header key
     */
    public final static String MANIFEST_REMOTE_SERVICE_HEADER = "Remote-Service";

}
