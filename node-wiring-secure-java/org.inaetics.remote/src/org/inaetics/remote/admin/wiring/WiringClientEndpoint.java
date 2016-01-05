/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import static org.inaetics.remote.IOUtil.closeSilently;
import static org.inaetics.remote.admin.wiring.WiringAdminUtil.getMethodSignature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.inaetics.wiring.endpoint.WiringConstants;
import org.inaetics.wiring.endpoint.WiringSender;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Implementation of an {@link InvocationHandler} that represents a remoted service for one or more service interfaces.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringClientEndpoint implements InvocationHandler {

    private static final int FATAL_ERROR_COUNT = 5;

    private final ObjectMapper m_objectMapper = new ObjectMapper();
    private final JsonFactory m_JsonFactory = new JsonFactory(m_objectMapper);

    private final Map<Method, String> m_interfaceMethods;
    private final EndpointDescription m_endpoint;
    private final Object m_proxy;

    private ClientEndpointProblemListener m_problemListener;
    private int m_remoteErrors;
    
    private volatile WiringSender m_sender;

    public WiringClientEndpoint(BundleContext bundleContext, EndpointDescription endpoint, WiringSender wiringSender, Class<?>... interfaceClasses) throws Exception {
        if (interfaceClasses.length == 0) {
            throw new IllegalArgumentException("Need at least one interface to expose!");
        }
        m_interfaceMethods = new HashMap<Method, String>();
        m_endpoint = endpoint;
        m_proxy = Proxy.newProxyInstance(getClass().getClassLoader(), interfaceClasses, this);
        m_remoteErrors = 0;
        m_sender = wiringSender;
        
        for (Class<?> interfaceClass : interfaceClasses) {
            for (Method method : interfaceClass.getMethods()) {
                m_interfaceMethods.put(method, getMethodSignature(method));
            }
        }
        
    }

    @SuppressWarnings("unchecked")
    public final <T> T getServiceProxy() {
        return (T) m_proxy;
    }

    @Override
    public final Object invoke(Object serviceProxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if ("equals".equals(methodName)) {
            // Compare by identity, should be sufficient for the general contract without the massive overhead of doing remote calls...
            return serviceProxy == args[0];
        }
        else if ("hashCode".equals(methodName)) {
            // Should be sufficient for the general contract without the massive overhead of doing remote calls...
            return System.identityHashCode(serviceProxy);
        }
        else if (m_interfaceMethods.containsKey(method)) {
            return invokeRemoteMethod(method, args);
        }
        return method.invoke(args);
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public void setProblemListener(ClientEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    /**
     * Handles I/O exceptions by counting the number of times they occurred, and if a certain
     * threshold is exceeded closes the import registration for this endpoint.
     * 
     * @param e the exception to handle.
     */
    private void handleRemoteException(IOException e) {
        if (m_problemListener != null) {
            if (++m_remoteErrors > FATAL_ERROR_COUNT) {
                m_problemListener.handleEndpointError(e);
            }
            else {
                m_problemListener.handleEndpointWarning(e);
            }
        }
    }

    /**
     * Does the invocation of the remote method adhering to any security managers that might be installed.
     * 
     * @param method the actual method to invoke;
     * @param arguments the arguments of the method to invoke;
     * @return the result of the method invocation, can be <code>null</code>.
     * @throws Exception in case the invocation failed in some way.
     */
    private Object invokeRemoteMethod(final Method method, final Object[] arguments) throws Throwable {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            try {
                return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        try {
                            return invokeRemoteMethodSecure(method, arguments);
                        }
                        catch (Throwable e) {
                            throw new ServiceException("TRANSPORT WRAPPER", e);
                        }
                    }
                });
            }
            catch (ServiceException e) {
                // All exceptions are wrapped in this exception, so we need to rethrow its cause to get the actual exception back...
                throw e.getCause();
            }
        }
        else {
            return invokeRemoteMethodSecure(method, arguments);
        }
    }

    /**
     * Does the actual invocation of the remote method.
     * <p>
     * This method assumes that all security checks (if needed) are processed!
     * </p>
     * 
     * @param method the actual method to invoke;
     * @param arguments the arguments of the method to invoke;
     * @return the result of the method invocation, can be <code>null</code>.
     * @throws Exception in case the invocation failed in some way.
     */
    private Object invokeRemoteMethodSecure(Method method, Object[] arguments) throws Throwable {

        OutputStream outputStream = null;
        InputStream inputStream = null;
        Object result = null;
        ExceptionWrapper exception = null;
        try {
            outputStream = new ByteArrayOutputStream();
            writeMethodInvocationJSON(outputStream, method, arguments);
            String message = outputStream.toString();
            
            String response = m_sender.sendMessage(message);
            
            JsonNode tree = m_objectMapper.readTree(response);
            if (tree != null) {
                JsonNode exceptionNode = tree.get("e");
                if (exceptionNode != null) {
                    exception = m_objectMapper.readValue(exceptionNode, ExceptionWrapper.class);
                }
                else {
                    JsonNode responseNode = tree.get("r");
                    if (responseNode != null) {
                        JavaType returnType =
                            m_objectMapper.getTypeFactory().constructType(method.getGenericReturnType());
                        result = m_objectMapper.readValue(responseNode, returnType);
                    }
                }
            }

            // Reset this error counter upon each successful request...
            m_remoteErrors = 0;
        }
        catch (IOException e) {
            handleRemoteException(e);
            throw new ServiceException("Remote service invocation failed: " + e.getMessage(), ServiceException.REMOTE, e);
        }
        finally {
            closeSilently(inputStream);
            closeSilently(outputStream);
        }

        if (exception != null) {
            throw exception.getException();
        }
        return result;
    }

    /**
     * Writes out the the invocation payload as a JSON object with with two fields. The m-field holds the method's signature
     * and the a-field hold the arguments array.
     * 
     * @param out the output stream to write to
     * @param method the method in question
     * @param arguments the arguments
     * @throws IOException if a write operation fails
     */
    private void writeMethodInvocationJSON(OutputStream out, Method method, Object[] arguments) throws IOException {
        JsonGenerator gen = m_JsonFactory.createJsonGenerator(out);
        gen.writeStartObject();
        gen.writeNumberField("service.id", m_endpoint.getServiceId());

        gen.writeObjectFieldStart("request");
        
        gen.writeStringField("m", m_interfaceMethods.get(method));
        gen.writeArrayFieldStart("a");
        if (arguments != null) {
            for (int i = 0; i < arguments.length; i++) {
                gen.writeObject(arguments[i]);
            }
        }
        gen.writeEndArray();
        
        gen.writeEndObject();
        
        gen.flush();
        gen.close();
    }
}
