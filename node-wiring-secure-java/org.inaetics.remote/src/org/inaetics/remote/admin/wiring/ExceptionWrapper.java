/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.node.ArrayNode;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
@JsonDeserialize(using = ExceptionWrapper.ExceptionDeserializer.class)
@JsonSerialize(using = ExceptionWrapper.ExceptionSerializer.class)
public class ExceptionWrapper {
    public static class ExceptionSerializer extends JsonSerializer<ExceptionWrapper> {
        @Override
        public void serialize(ExceptionWrapper obj, JsonGenerator gen, SerializerProvider sp) throws IOException {
            Throwable ex = obj.m_exception;

            gen.writeStartObject();
            gen.writeStringField("type", ex.getClass().getName());
            gen.writeStringField("msg", ex.getMessage());
            gen.writeObjectField("stacktrace", ex.getStackTrace());
            gen.writeEndObject();
        }
    }

    public static class ExceptionDeserializer extends JsonDeserializer<ExceptionWrapper> {
        @Override
        public ExceptionWrapper deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode node = parser.readValueAsTree();

            String typeName = node.get("type").getTextValue();
            String msg = node.get("msg").getTextValue();
            ArrayNode stackTraceNodes = (ArrayNode) node.get("stacktrace");

            Throwable t;

            try {
                Class<?> type;
                Bundle b = FrameworkUtil.getBundle(getClass());
                if (b != null) {
                    type = b.loadClass(typeName);
                }
                else {
                    type = getClass().getClassLoader().loadClass(typeName);
                }

                try {
                    // Try to create one with a message...
                    t = (Throwable) type.getConstructor(String.class).newInstance(msg);
                }
                catch (NoSuchMethodException e) {
                    // Try to create using the default constructor...
                    t = (Throwable) type.newInstance();
                }

                StackTraceElement[] stacktrace = new StackTraceElement[stackTraceNodes.size()];
                for (int i = 0; i < stacktrace.length; i++) {
                    stacktrace[i] = parser.getCodec().treeToValue(stackTraceNodes.get(i), StackTraceElement.class);
                }

                t.setStackTrace(stacktrace);
            }
            catch (Exception e) {
                e.printStackTrace();
                t = new RuntimeException(msg);
            }

            return new ExceptionWrapper(t);
        }
    }

    private final Throwable m_exception;

    public ExceptionWrapper(Throwable e) {
        m_exception = e;
    }

    public Throwable getException() {
        return m_exception;
    }
}