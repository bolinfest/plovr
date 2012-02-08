package org.plovr.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HttpExchangeUtilTest {

  @Test
  public void testIsGoogleChrome16OrEarlier() {
    String chrome16UserAgent =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.77 Safari/535.7";
    assertTrue(HttpExchangeUtil.isGoogleChrome16OrEarlier(chrome16UserAgent));

    String chrome17UserAgent =
      "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.46 Safari/535.11";
    assertFalse(HttpExchangeUtil.isGoogleChrome16OrEarlier(chrome17UserAgent));
  }
}
