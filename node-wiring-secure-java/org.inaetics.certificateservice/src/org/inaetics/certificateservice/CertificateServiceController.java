package org.inaetics.certificateservice;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.inaetics.truststorage.TrustStorageService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CertificateServiceController {

	private static CertificateServiceController _instance;
	
	private volatile TrustStorageService trustStorage;
	
	private static final String IPADDRESS_PATTERN = 
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	private CertificateServiceController(TrustStorageService trustStorage) {
		this.trustStorage = trustStorage;
	}

	protected static CertificateServiceController getInstance(TrustStorageService trustStorage) {
		if (_instance == null) {
			_instance = new CertificateServiceController(trustStorage);
		}
		return _instance;
	}
	
	protected static CertificateServiceController getInstance() throws IllegalAccessException {
		if (_instance == null) {
			throw new IllegalAccessException();
		}
		return _instance;
	}
	
	/**
	 * Generates the principal string for the certificate.
	 * @return The certificates principal string based on the primary route.
	 */
	private String aggregatePodPrincipalString() {
		String hostString = CertificateServiceController.cmdExec("ip route get 1");
		String ip = "127.0.0.1";
		String[] input = hostString.split("\n");
		if (input.length >= 1) {
			String[] ipArr = input[0].split(" ");
			hostString = ipArr[ipArr.length-1];
			Pattern p = Pattern.compile(IPADDRESS_PATTERN);
			Matcher m = p.matcher(hostString);
			if (m.matches()) {
				ip = m.group(0);
			}
		}
	    return CaConfig.PRINCIPAL_STRING.replace(
	            CaConfig.PRINCIPAL_STRING_CN_SELECTER, ip);
	}
	
	public static String cmdExec(String cmdLine) {
	    String line;
	    String output = "";
	    try {
	        Process p = Runtime.getRuntime().exec(cmdLine);
	        BufferedReader input = new BufferedReader
	            (new InputStreamReader(p.getInputStream()));
	        while ((line = input.readLine()) != null) {
	            output += (line + '\n');
	        }
	        input.close();
	        }
	    catch (Exception ex) {
	        ex.printStackTrace();
	    }
	    return output;
	}

	protected void checkAndUpdateCertificates() {
		System.out.println("checkAndUpdateCertificates");
		X509Certificate caCert = trustStorage.getRootCaCert();
		System.out.println(caCert == null);
		if (caCert == null) {
			System.out.println("Ca cert not loaded yet");
			try {
				getRootCaCertificate();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				caCert.checkValidity();
			} catch (CertificateExpiredException | CertificateNotYetValidException e) {
				// TODO Auto-generated catch block

				try {
					getRootCaCertificate();
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					System.out.println("Failed to parse server response");
				}
			}
			System.out.println("No need to change CA Certs");

		}
		try {
			X509Certificate nodeCertificate = (X509Certificate) trustStorage.getKeyStore().getCertificate("keypair");
			if (nodeCertificate == null) {
				generateKeyAndSign();
			} else {
				nodeCertificate.checkValidity();
				if (caCert != null) {
					nodeCertificate.verify(caCert.getPublicKey());
				}
				System.out.println("node cert still valid");
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			// TODO Auto-generated catch block
				generateKeyAndSign();

		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected void generateKeyAndSign() {
		System.out.println("getNewCertificate");
		byte[] byteCSR = null;
		KeyPair pair = null;
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048, new SecureRandom());
			pair = keyGen.generateKeyPair();
			PKCS10CertificationRequest csr = createCSR(pair);
			byteCSR = csr.getEncoded();
		} catch (NoSuchAlgorithmException | OperatorCreationException | IOException e) {
			// TODO Add LOGGING THROW CUSTOM EXCEPTION
		}
		if (byteCSR != null) {
			OutputStream out;
			URL url = null;
			try {
				url = new URL(CaConfig.getSignUrl());
				JSONObject csrPostBody = new JSONObject();
				csrPostBody.put("certificate_request",
						CaConfig.CSR_BEGIN + Base64.toBase64String(byteCSR) + CaConfig.CSR_END);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "Application/json");
				out = connection.getOutputStream();
				if (out != null) {
					out.write(csrPostBody.toString().getBytes("UTF-8"));
				}
				int statusCode = connection.getResponseCode();
				switch (statusCode) {
				case HttpURLConnection.HTTP_OK:
					X509Certificate cert = parseCAResponse(connection.getInputStream());
					if (cert != null && trustStorage != null) {
						System.out.println("cert fetched");
						trustStorage.storeSignedKeyPair(cert, pair.getPrivate());

					}

					break;
				default:
					System.out.print("Error" + statusCode);
					break;
				}
			} catch (MalformedURLException | ProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	protected void getRootCaCertificate() throws ParseException {
		System.out.println("getRootCaCertificate");
		JSONObject body = new JSONObject();

		HttpURLConnection connection;

		try {
			URL url = new URL(CaConfig.getRootCertUrl());
			// Proxy proxy = new Proxy(Proxy.Type.HTTP, new
			// InetSocketAddress("10.160.64.104", 8080));
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "Application/json");
			connection.setRequestProperty("Cookie", "notified-NotifyUser_catagory_NON=1");
			OutputStream out = connection.getOutputStream();
			out.write(body.toString().getBytes());
			int statusCode = connection.getResponseCode();

			switch (statusCode) {
			case HttpURLConnection.HTTP_OK:
				X509Certificate caCert = parseCAResponse(connection.getInputStream());
				if (caCert != null && trustStorage !=null) {
					trustStorage.storeRootCaCert(caCert);
					System.out.println("server cert fetched");
				}

				break;
			default:
				System.out.print("Error" + statusCode);
			}
			connection.setConnectTimeout(4000);
			connection.setReadTimeout(4000);
			connection.connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private PKCS10CertificationRequest createCSR(KeyPair pair) throws OperatorCreationException {
		PublicKey publicKey = pair.getPublic();
		PrivateKey privateKey = pair.getPrivate();
		String principalString = aggregatePodPrincipalString();
		X500Principal principal = new X500Principal(principalString);

		PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(principal, publicKey);
		JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(CaConfig.SIGNATURE_ALGORITHM);
		ContentSigner signer = csBuilder.build(privateKey);
		return p10Builder.build(signer);
	}

	private X509Certificate parseCAResponse(InputStream in) throws IOException, ParseException, CertificateException {
		JSONParser parser = new JSONParser();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder sBuffer = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sBuffer.append(line);
		}
		reader.close();

		JSONObject response = (JSONObject) parser.parse(sBuffer.toString());
		boolean succes = (boolean) response.get("success");
		if (succes) {
			JSONObject resultObject = (JSONObject) response.get("result");
			String certificateString = (String) resultObject.get("certificate");
			// Remove all stupid \n stuff the CA returns
			certificateString.replaceAll("\n", "");
			PemReader pemReader = new PemReader(new StringReader(certificateString));
			PemObject pemObject = pemReader.readPemObject();
			if (!pemObject.getType().equalsIgnoreCase("CERTIFICATE")) {
				pemReader.close();
				throw new IllegalArgumentException(("An invalid PemObject type found: " + pemObject.getType()));
			}
			byte[] x509Data = pemObject.getContent();
			pemReader.close();
			CertificateFactory factory = CertificateFactory.getInstance("X509");
			X509Certificate cert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(x509Data));
			return cert;
		} else {
			// TODO LOG error stuff
		}

		return null;
	}

}
