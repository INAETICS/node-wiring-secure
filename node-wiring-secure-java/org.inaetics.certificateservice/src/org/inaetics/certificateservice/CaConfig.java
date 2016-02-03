package org.inaetics.certificateservice;
/**
 * Created by Mike on 14-12-2015.
 */
public class CaConfig {
    private static final String CA_BASE_URL = "http://82.75.221.245/api/v1/cfssl/";
    public static final String CA_SIGN_URL = CaConfig.CA_BASE_URL + "sign";
    public static final String CA_INFO_URL = CaConfig.CA_BASE_URL + "info";
    public static final String CSR_BEGIN = "-----BEGIN CERTIFICATE REQUEST-----\n";
    public static final String CSR_END = "\n-----END CERTIFICATE REQUEST-----\n";
    public static final String PRINCIPAL_STRING = "CN=Mike.org, L=Enschede, ST=Overijssel, C=Netherlands";
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
}
