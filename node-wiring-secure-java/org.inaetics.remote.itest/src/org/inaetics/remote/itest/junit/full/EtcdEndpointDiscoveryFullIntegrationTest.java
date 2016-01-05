/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.itest.junit.full;

import static org.inaetics.remote.itest.config.Configs.configs;
import static org.inaetics.remote.itest.config.Configs.frameworkConfig;

import org.inaetics.remote.itest.config.Config;
import org.inaetics.remote.itest.config.FrameworkConfig;
import org.inaetics.remote.itest.util.FrameworkContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 * 
 */
public class EtcdEndpointDiscoveryFullIntegrationTest extends AbstractFullIntegrationTest {

    // Skip itest unless you have Etcd running
    private final boolean SKIP = true;
    private static final String ETCD = "http://docker:4001";
    private static final String ETCD_ROOTPATH_WIRING = "/wiringDiscovery";
    private static final String ETCD_ROOTPATH_RSA = "/rsaDiscovery";
    
    @Override
    protected Config[] configureFramework(FrameworkContext parent) throws Exception {
        BundleContext parentBC = getParentContext().getBundleContext();

        String systemPackages = parentBC.getProperty("itest.systempackages");
        String defaultBundles = parentBC.getProperty("itest.bundles.default");
        String remoteServiceAdminBundles = parentBC.getProperty("itest.bundles.admin.wiring");
        String topologyManagerBundles = parentBC.getProperty("itest.bundles.topology.promiscuous");
        String discoveryBundles = parentBC.getProperty("itest.bundles.discovery.etcd");

        parent.setLogLevel(LogService.LOG_DEBUG);
        parent.setServiceTimout(30000);

        FrameworkConfig child1 = frameworkConfig("CHILD1")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(30000)
            .frameworkProperty("felix.cm.loglevel", "4")
            .frameworkProperty("org.apache.felix.http.host", "localhost")
            .frameworkProperty("org.osgi.service.http.port.secure", "8443")
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.zone", "zone1")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.node", "node1")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.connecturl", ETCD)
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.rootpath", ETCD_ROOTPATH_WIRING)
            .frameworkProperty("org.inaetics.wiring.admin.https.zone", "zone1")
            .frameworkProperty("org.inaetics.wiring.admin.https.node", "node1")
            .bundlePaths(defaultBundles, remoteServiceAdminBundles, topologyManagerBundles, discoveryBundles);

        FrameworkConfig child2 = frameworkConfig("CHILD2")
            .logLevel(LogService.LOG_DEBUG)
            .serviceTimeout(30000)
            .frameworkProperty("felix.cm.loglevel", "4")
            .frameworkProperty("org.apache.felix.http.host", "localhost")
            .frameworkProperty("org.osgi.service.http.port.secure", "8444")
            .frameworkProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages)
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.zone", "zone1")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.node", "node2")
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.connecturl", ETCD)
            .frameworkProperty("org.inaetics.wiring.discovery.etcd.rootpath", ETCD_ROOTPATH_WIRING)
            .frameworkProperty("org.inaetics.wiring.admin.https.zone", "zone1")
            .frameworkProperty("org.inaetics.wiring.admin.https.node", "node2")
            .bundlePaths(defaultBundles, remoteServiceAdminBundles, topologyManagerBundles, discoveryBundles);

        return configs(child1, child2);
    }

    @Override
    protected void configureServices() throws Exception {

        if (SKIP) {
            System.out.println("--------------------------------------------");
            System.out.println("Skipping Etcd integration test!");
            System.out.println("--------------------------------------------");

        }
        else {
            // Set connect strings so the clients connect to the freshly created server.
            getChildContext("CHILD1").configure("org.amdatu.remote.discovery.etcd",
                "org.amdatu.remote.discovery.etcd.connecturl", ETCD,
                "org.amdatu.remote.discovery.etcd.rootpath", ETCD_ROOTPATH_RSA);

            getChildContext("CHILD2").configure("org.amdatu.remote.discovery.etcd",
                "org.amdatu.remote.discovery.etcd.connecturl", ETCD,
                "org.amdatu.remote.discovery.etcd.rootpath", ETCD_ROOTPATH_RSA);
        }
    }

    @Override
    protected void cleanupTest() throws Exception {
    }

    public void testBasicServiceExportImportInvoke() throws Exception {

        if (!SKIP) {
            super.testBasicServiceExportImportInvoke();
        }
    }

}
