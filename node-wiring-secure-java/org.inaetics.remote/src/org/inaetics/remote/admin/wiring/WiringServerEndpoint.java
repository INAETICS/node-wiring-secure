/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.wiring;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.inaetics.remote.admin.wiring.WiringAdminUtil.getMethodSignature;
import static org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent.EXPORT_ERROR;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.type.JavaType;
import org.inaetics.wiring.endpoint.WiringReceiver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

/**
 * Servlet that represents a remoted local service.
 *
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class WiringServerEndpoint {

    private static final int FATAL_ERROR_COUNT = 5;

    private static final String APPLICATION_JSON = "application/json";

    private final ObjectMapper m_objectMapper = new ObjectMapper();
    private final JsonFactory m_jsonFactory = new JsonFactory(m_objectMapper);

    private final BundleContext m_bundleContext;
    private final ServiceReference<?> m_serviceReference;
    private final Map<String, Method> m_interfaceMethods;

    private ServerEndpointProblemListener m_problemListener;
    private int m_localErrors;

    private WiringReceiver m_receiver;
    private volatile DependencyManager m_dependencyManager;
    private volatile Component m_receiverComponent;
	private volatile boolean m_wireCreated = false;

	
    public WiringServerEndpoint(RemoteServiceAdminFactory factory, final RemoteServiceAdminImpl admin, final ExportRegistration exportRegistration,
    		final BundleContext context, final ServiceReference<?> reference,
    		final Map<String, String> properties, final Class<?>... interfaceClasses) {

        m_bundleContext = context;
        m_serviceReference = reference;
        m_interfaceMethods = new HashMap<String, Method>();
        m_dependencyManager = factory.getDependencyManager();

        for (Class<?> interfaceClass : interfaceClasses) {
            for (Method method : interfaceClass.getMethods()) {
                // Although we're accessing a public (interface) method, the *service* implementation
                // itself can be non-public. This check appears to be fixed in recent Java versions...
                method.setAccessible(true);
                m_interfaceMethods.put(getMethodSignature(method), method);
            }
        }
        
        final CountDownLatch doneSignal = new CountDownLatch(1);
        m_receiver = new WiringReceiver() {
			
			@Override
			public void wiringEndpointRemoved(String wireId) {
				// unregister service
				if (m_receiverComponent != null) {
					m_dependencyManager.remove(m_receiverComponent);
					m_receiverComponent = null;
				}

				// notify TM when this happens after successful wire creation
				if (m_wireCreated) {
					admin.getEventsHandler().emitEvent(EXPORT_ERROR, context.getBundle(), exportRegistration.getExportReference(), new Exception("wire was removed"));
				}

				doneSignal.countDown();
			}
			
			@Override
			public void wiringEndpointAdded(String wireId) {
				properties.put(WiringAdminConstants.WIRE_ID, wireId);
				m_wireCreated = true;
				doneSignal.countDown();
			}
			
			@Override
			public String messageReceived(String message) throws Exception {
				
	            JsonNode tree = m_objectMapper.readTree(message);
	            if (tree == null) {
	            	throw new Exception("error reading message");
	            }

	            JsonNode serviceIdNode = tree.get("service.id");
	            if (serviceIdNode == null) {
	            	throw new Exception("error reading service.id");
	            }

	            JsonNode requestNode = tree.get("request");
	            if (requestNode == null) {
	            	throw new Exception("error reading request");
	            }
				
				return invokeService(requestNode.toString());
			}
		};
		
		// add some service properties for easier debugging...
		Dictionary<String, Object> receiverProps = new Hashtable<String, Object>();
		receiverProps.put("exportedService.id", reference.getProperty(Constants.SERVICE_ID));
		receiverProps.put("exportedService.interfaces", reference.getProperty(RemoteConstants.SERVICE_EXPORTED_INTERFACES));
        m_receiverComponent = m_dependencyManager.createComponent()
				.setInterface(WiringReceiver.class.getName(), receiverProps)
				.setImplementation(m_receiver);
        m_dependencyManager.add(m_receiverComponent);
		
		try {
			doneSignal.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// nothing to do
		}
		
		if (!m_wireCreated) {
			throw new RuntimeException("could not create wire");
		}
    }
    
    public void close() {
    	if (m_dependencyManager != null && m_receiverComponent != null) {
    		m_dependencyManager.remove(m_receiverComponent);
    	}
    }

    /**
     * @param problemListener the problem listener to set, can be <code>null</code>.
     */
    public void setProblemListener(ServerEndpointProblemListener problemListener) {
        m_problemListener = problemListener;
    }

    private String invokeService(String message) throws IOException {

        boolean ungetService = false;
        try {

            JsonNode tree = m_objectMapper.readTree(message);
            if (tree == null) {
            	// TODO how to indicate an error?
            	return null;
            }

            JsonNode signatureNode = tree.get("m");
            if (signatureNode == null) {
            	// TODO how to indicate an error?
            	return null;
            }

            JsonNode argumentsNode = tree.get("a");
            if (argumentsNode == null) {
            	// TODO how to indicate an error?
            	return null;
            }

            ArrayNode arguments = m_objectMapper.readValue(argumentsNode, ArrayNode.class);
            if (arguments == null) {
            	// TODO how to indicate an error?
            	return null;
            }

            Method method = m_interfaceMethods.get(signatureNode.asText());
            if (method == null) {
            	// TODO how to indicate an error?
            	return null;
            }

            Type[] types = method.getGenericParameterTypes();
            if (arguments.size() != types.length) {
            	// TODO how to indicate an error?
            	return null;
            }

            Object[] parameters = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                try {
                    JavaType argType =
                        m_objectMapper.getTypeFactory().constructType(types[i]);
                    parameters[i] = m_objectMapper.readValue(arguments.get(i), argType);
                }
                catch (Exception e) {
                	// TODO how to indicate an error?
                	return null;
                }
            }

            Object service = m_bundleContext.getService(m_serviceReference);
            if (service == null) {
                handleLocalException(null);
            	// TODO how to indicate an error?
            	return null;
            }
            ungetService = true;

            Object result = null;
            Exception exception = null;
            try {
                result = method.invoke(service, parameters);
            }
            catch (Exception e) {
                exception = e;
            }

            OutputStream output = new ByteArrayOutputStream();
            JsonGenerator gen = m_jsonFactory.createJsonGenerator(output);
            gen.writeStartObject();
            if (exception != null) {
                gen.writeObjectField("e", new ExceptionWrapper(unwrapException(exception)));
            }
            else if (!Void.TYPE.equals(method.getReturnType())) {
                gen.writeObjectField("r", result);
            }
            gen.close();

            // All is fine.. reset the local error count
            m_localErrors = 0;
            
            return output.toString();
        }
        finally {
            if (ungetService) {
                m_bundleContext.ungetService(m_serviceReference);
            }
        }
    }

    /**
     * Handles I/O exceptions by counting the number of times they occurred, and if a certain
     * threshold is exceeded closes the import registration for this endpoint.
     * 
     * @param e the exception to handle.
     */
    private void handleLocalException(IOException e) {
        if (m_problemListener != null) {
            if (++m_localErrors > FATAL_ERROR_COUNT) {
                m_problemListener.handleEndpointError(e);
            }
            else {
                m_problemListener.handleEndpointWarning(e);
            }
        }
    }

    /**
     * Unwraps a given {@link Exception} into a more concrete exception if it represents an {@link InvocationTargetException}.
     * 
     * @param e the exception to unwrap, should not be <code>null</code>.
     * @return the (unwrapped) throwable or exception, never <code>null</code>.
     */
    private static Throwable unwrapException(Exception e) {
        if (e instanceof InvocationTargetException) {
            return ((InvocationTargetException) e).getTargetException();
        }
        return e;
    }

    /**
     * Writes all method signatures as a flat JSON array to the given HttpServletResponse
     * 
     * @param req the HttpServletRequest
     * @param resp the HttpServletResponse
     * @throws IOException
     */
    public void listMethodSignatures(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        resp.setStatus(SC_OK);
        resp.setContentType(APPLICATION_JSON);

        JsonGenerator gen = m_jsonFactory.createJsonGenerator(resp.getOutputStream());
        gen.writeStartArray();

        for (String signature : m_interfaceMethods.keySet()) {
            gen.writeString(signature);
        }

        gen.writeEndArray();
        gen.close();

    }
}
