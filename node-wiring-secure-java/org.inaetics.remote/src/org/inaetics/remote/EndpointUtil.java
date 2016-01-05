/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class EndpointUtil {

    private final static EndpointDescriptorReader m_reader = new EndpointDescriptorReader();
    private final static EndpointDescriptorWriter m_writer = new EndpointDescriptorWriter();
    private final static EndpointHashGenerator m_hasher = new EndpointHashGenerator();

    public static List<EndpointDescription> readEndpoints(Reader reader) throws IOException {
        return m_reader.parseDocument(reader);
    }

    public static void writeEndpoints(Writer writer, EndpointDescription... endpoints) throws IOException {
        m_writer.writeDocument(writer, endpoints);
    }

    public static String computeHash(EndpointDescription endpoint) {
        return m_hasher.hash(endpoint.getProperties());
    }

    private EndpointUtil() {
    }
}
