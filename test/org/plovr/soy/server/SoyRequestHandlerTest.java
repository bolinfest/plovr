package org.plovr.soy.server;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.restricted.BooleanData;
import com.google.template.soy.data.restricted.FloatData;
import com.google.template.soy.data.restricted.IntegerData;
import com.google.template.soy.data.restricted.NullData;
import com.google.template.soy.data.restricted.StringData;

/**
 * {@link SoyRequestHandlerTest} is a unit test for {@link SoyRequestHandler}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class SoyRequestHandlerTest {

  @Test
  public void testGetValueForQueryParam() {
    // Nominal cases.
    assertValueForQueryParam(StringData.forValue("soy"), "soy");
    assertValueForQueryParam(BooleanData.TRUE, "true");
    assertValueForQueryParam(BooleanData.FALSE, "false");
    assertValueForQueryParam(IntegerData.forValue(3), "3");
    assertValueForQueryParam(FloatData.forValue(3.14), "3.14");
    assertValueForQueryParam(NullData.INSTANCE, "null");

    // Trickier cases.
    assertValueForQueryParam("Malformed JSON should be treated as a string",
        StringData.forValue("[1, "), "[1, ");
    assertValueForQueryParam("Boolean data is case-sensitive",
        StringData.forValue("TRUE"), "TRUE");
    assertValueForQueryParam("Boolean data is case-sensitive",
        StringData.forValue("FALSE"), "FALSE");
    assertValueForQueryParam("Null data is case-sensitive",
        StringData.forValue("NULL"), "NULL");
    assertValueForQueryParam(
        "Should be able to tolerate a string with double-quotes",
        StringData.forValue("\"Hello,\" I said."), "\"Hello,\" I said.");
  }

  @Test
  public void testCreateSoyDataFromUri() throws URISyntaxException {
    URI uri = new URI("http://localhost:9811/settings.html" +
    		"?option=trueoption=10&option=null&option=false");
    Map<String, SoyData> soyData = SoyRequestHandler.createSoyDataFromUri(uri);
    assertEquals("Should use rightmost value for 'option'",
        ImmutableMap.of("option", BooleanData.FALSE),
        soyData);
  }

  private void assertValueForQueryParam(SoyData expected, String queryParam) {
    assertEquals(null /* message */, expected,
        SoyRequestHandler.getValueForQueryParam(queryParam));
  }

  private void assertValueForQueryParam(String message, SoyData expected,
      String queryParam) {
    assertEquals(message, expected,
        SoyRequestHandler.getValueForQueryParam(queryParam));
  }
}
