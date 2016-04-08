package org.inaetics.certificateservice;
/**
 * Created by Mike on 14-12-2015.
 */
public class CaConfig {
    private static final String CA_BASE_URL = "http://127.0.0.1:8888/api/v1/cfssl/";
    public static final String CA_SIGN_URL = CaConfig.CA_BASE_URL + "sign";
    public static final String CA_INFO_URL = CaConfig.CA_BASE_URL + "info";
    public static final String CSR_BEGIN = "-----BEGIN CERTIFICATE REQUEST-----\n";
    public static final String CSR_END = "\n-----END CERTIFICATE REQUEST-----\n";
    public static final String PRINCIPAL_STRING = "CN={CN}, L=Hengelo, ST=Overijssel, C=Netherlands";
    public static final String PRINCIPAL_STRING_CN_SELECTER = "{CN}";
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
}
