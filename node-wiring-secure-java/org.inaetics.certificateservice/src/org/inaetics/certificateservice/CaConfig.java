package org.inaetics.certificateservice;
/**
 * Created by Mike on 14-12-2015.
 */
public class CaConfig {
    /**
     * Configuration PID
     */
    public static final String SERVICE_PID = "org.inaetics.certificateservice";
	
    public static String CA_HOSTPORT = "localhost:8888";
    
	public static final String CA_SIGN_ENDPOINT = "sign";
	public static final String CA_INFO_ENDPOINT = "info";
	public static final String CA_BASE_URL = "http://{HOSTPORT}/api/v1/cfssl";
	public static final String CA_ENDPOINT_SYNTAX = "%s/%s";
	public static final String CA_HOST_PORT_REPLACER = "{HOSTPORT}";
    public static final String CSR_BEGIN = "-----BEGIN CERTIFICATE REQUEST-----\n";
    public static final String CSR_END = "\n-----END CERTIFICATE REQUEST-----\n";
    public static final String PRINCIPAL_STRING = "CN={CN}, L=Hengelo, ST=Overijssel, C=Netherlands";
    public static final String PRINCIPAL_STRING_CN_SELECTER = "{CN}";
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    

	public static String getCaBaseUrl() {
		return CaConfig.CA_BASE_URL
				.replace(CaConfig.CA_HOST_PORT_REPLACER, CaConfig.CA_HOSTPORT);
	}

	public static String getSignUrl() {
		return aggregateEndpointUrl(CaConfig.CA_SIGN_ENDPOINT);
	}

	public static String getRootCertUrl() {
		return aggregateEndpointUrl(CaConfig.CA_INFO_ENDPOINT);
	}

	public static String aggregateEndpointUrl(String endpoint) {
		return String.format(CaConfig.CA_ENDPOINT_SYNTAX, CaConfig.getCaBaseUrl(), endpoint);
	}
}
