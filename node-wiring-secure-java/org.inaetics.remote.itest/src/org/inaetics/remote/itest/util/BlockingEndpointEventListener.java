/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;

/**
 * Self-registering utility that uses an {@link EndpointEventListener} to await an added or removed callback
 * for a specific {@link EndpointDescription}
 * 
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class BlockingEndpointEventListener extends AbstractBlockingEndpointListener<EndpointEventListener> {

    private final String[] m_objectClass = new String[] { EndpointEventListener.class.getName() };

    private final Object m_listenerInstance = new InternalEndpointEventListener();

    public BlockingEndpointEventListener(final BundleContext context, final EndpointDescription description) {

        this(context, description, "(" + Constants.OBJECTCLASS + "=*)");
    }

    public BlockingEndpointEventListener(final BundleContext context, final EndpointDescription description,
        final String scopeFilter) {
        super(context, scopeFilter, description);
    }

    @Override
    protected Object getListener() {
        return m_listenerInstance;
    }

    @Override
    protected String getScopeKey() {
        return EndpointEventListener.ENDPOINT_LISTENER_SCOPE;
    }

    @Override
    protected String[] getObjectClass() {
        return m_objectClass;
    }

    private class InternalEndpointEventListener implements EndpointEventListener {

        @Override
        public void endpointChanged(EndpointEvent event, String matchedFilter) {
            synchronized (BlockingEndpointEventListener.this) {
                switch (event.getType()) {
                    case EndpointEvent.ADDED:
                        BlockingEndpointEventListener.this.endpointAdded(event.getEndpoint());
                        break;
                    case EndpointEvent.REMOVED:
                        BlockingEndpointEventListener.this.endpointRemoved(event.getEndpoint());
                        break;
                    case EndpointEvent.MODIFIED:
                        BlockingEndpointEventListener.this.endpointModified(event.getEndpoint());
                        break;
                    case EndpointEvent.MODIFIED_ENDMATCH:
                        BlockingEndpointEventListener.this.endpointEndmatch(event.getEndpoint());
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }
    }
}