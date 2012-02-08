package org.plovr.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HttpExchangeUtilTest {

  @Test
  public void testIsGoogleChrome17OrEarlier() {
    String chrome16UserAgent =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.77 Safari/535.7";
    assertTrue(HttpExchangeUtil.isGoogleChrome17OrEarlier(chrome16UserAgent));

    String chrome17UserAgent =
      "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.46 Safari/535.11";
    assertTrue(HttpExchangeUtil.isGoogleChrome17OrEarlier(chrome17UserAgent));

    String chrome18UserAgent =
      "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.18 (KHTML, like Gecko) Chrome/18.0.1010.1 Safari/535.18";
    assertFalse(HttpExchangeUtil.isGoogleChrome17OrEarlier(chrome18UserAgent));
  }
}
