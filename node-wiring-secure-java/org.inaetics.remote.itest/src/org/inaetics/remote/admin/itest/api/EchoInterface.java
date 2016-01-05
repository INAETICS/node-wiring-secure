/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.itest.api;

import java.util.List;

/**
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public interface EchoInterface {
    String echo(String name);

    EchoData echo(EchoData data);

    List<EchoData> echo(List<EchoData> data);
}
