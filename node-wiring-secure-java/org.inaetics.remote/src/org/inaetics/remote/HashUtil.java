/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author <a href="mailto:amdatu-developers@amdatu.org">Amdatu Project Team</a>
 */
public final class HashUtil {

    /** The message digest algorithm used to fingerprint files. */
    private static final String DIGEST_ALG = "MD5";

    /** The hexadecimal alphabet. */
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    /**
     * Creates a new message digest instance.
     * 
     * @return a new {@link MessageDigest} instance, never <code>null</code>.
     * @throws RuntimeException in case the desired message digest algorithm is not
     *         supported by the platform.
     */
    public static MessageDigest createDigester() {
        try {
            return MessageDigest.getInstance(DIGEST_ALG);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("No such algorithm: " + DIGEST_ALG);
        }
    }

    /**
     * Creates a hexadecimal representation of all given bytes an concatenates these
     * hex-values to a single string.
     * 
     * @param bytes the byte values to convert, cannot be <code>null</code>.
     * @return a hex-string of the given bytes, never <code>null</code>.
     */
    public static String toHexString(byte[] bytes) {
        // based on <http://stackoverflow.com/a/9855338/229140>
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_DIGITS[v >>> 4];
            hexChars[j * 2 + 1] = HEX_DIGITS[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String hash(String msg) {
        try {
            return toHexString(createDigester().digest(msg.getBytes("UTF-8")));
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported?!");
        }
    }

    public static String hash(Method m) {
        return hash(m.toGenericString());
    }
}
