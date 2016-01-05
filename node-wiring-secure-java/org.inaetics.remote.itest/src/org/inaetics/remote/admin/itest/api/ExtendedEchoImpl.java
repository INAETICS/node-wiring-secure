/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.itest.api;

import java.util.List;

/**
 * Simple implementation of {@link ExtendedEchoInterface}.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public class ExtendedEchoImpl implements ExtendedEchoInterface {

    @Override
    public String echo(String name) {
        return name;
    }

    @Override
    public String shout(String message) {
        return message == null ? null : message.toUpperCase();
    }

    @Override
    public EchoData echo(EchoData data) {
        return data;
    }

    @Override
    public List<EchoData> echo(List<EchoData> data) {
        return data;
    }
}
