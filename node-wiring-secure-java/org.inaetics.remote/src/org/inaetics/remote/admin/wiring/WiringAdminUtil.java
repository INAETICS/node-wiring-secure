/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Collection of util methods for the Http Admin Remote Sercvice Admin implementation.
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringAdminUtil {

    /**
     * Generate the method signature used to uniquely identity a method during remote
     * invocation.
     * 
     * @param method the method
     * @return the signature
     */
    public static String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder(method.getName()).append("(");
        for (Class<?> parameterType : method.getParameterTypes()) {
            appendTypeSignature(sb, parameterType);
        }
        sb.append(")");
        appendTypeSignature(sb, method.getReturnType());
        return sb.toString();
    }

    private static final Map<Class<?>, String> TYPESCODES = new HashMap<Class<?>, String>();
    static {
        TYPESCODES.put(Void.TYPE, "V");
        TYPESCODES.put(Boolean.TYPE, "Z");
        TYPESCODES.put(Character.TYPE, "C");
        TYPESCODES.put(Short.TYPE, "S");
        TYPESCODES.put(Integer.TYPE, "I");
        TYPESCODES.put(Long.TYPE, "J");
        TYPESCODES.put(Float.TYPE, "F");
        TYPESCODES.put(Double.TYPE, "D");
    }

    private static void appendTypeSignature(StringBuilder buffer, Class<?> clazz) {
        if (clazz.isArray()) {
            buffer.append("[");
            appendTypeSignature(buffer, clazz.getComponentType());
        }
        else if (clazz.isPrimitive()) {
            buffer.append(TYPESCODES.get(clazz));
        }
        else {
            buffer.append("L").append(clazz.getName().replaceAll("\\.", "/")).append(";");
        }
    }

    private WiringAdminUtil() {
    }
}
