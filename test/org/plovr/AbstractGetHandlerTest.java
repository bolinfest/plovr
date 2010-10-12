package org.plovr;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class AbstractGetHandlerTest {

  @Test
  public void testParseConfigIdFromRestUri() throws URISyntaxException {
    URI uri = new URI("http://localhost:9810/input/myConfig/goog/debug/error.js");
    String configId = AbstractGetHandler.parseConfigIdFromRestUri(uri);
    assertEquals("myConfig", configId);
  }
}
