/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.itest.api;

import java.util.List;

/**
 * Simple implementation of {@link EchoInterface}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class EchoImpl implements EchoInterface {
    @Override
    public String echo(String name) {
        return name;
    }

    @Override
    public List<EchoData> echo(List<EchoData> data) {
        return data;
    }

    @Override
    public EchoData echo(EchoData data) {
        return data;
    }
}
