/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class IOUtil {

    public static void closeSilently(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            }
            catch (IOException e) {
                // Ignore...
            }
        }
    }
}
