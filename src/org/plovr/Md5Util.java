package org.plovr;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.base.Charsets;

/**
 * {@link Md5Util} provides a nice API for getting the MD5 hash of a string.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class Md5Util {

  /** Utility class; do not instantiate. */
  private Md5Util() {}

  public static String hash(String str, Charset charset) {
    // From http://www.geekpedia.com/code114_MD5-Encryption-Using-Java.html
    MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    messageDigest.update(str.getBytes(charset), 0, str.length());
    return new BigInteger(1, messageDigest.digest()).toString(16);
  }

  public static String hashJs(String str) {
    return hash(str, Charsets.UTF_8);
  }
}
