package org.plovr.soy.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
    assertValueForQueryParam(
        "Should be able to tolerate a string with double-quotes",
        StringData.forValue("\"Hello,\" I said."), "\"Hello,\" I said.");
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
