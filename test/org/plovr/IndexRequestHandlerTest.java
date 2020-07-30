package org.plovr;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;

import java.io.OutputStream;
import java.io.File;
import java.net.URI;

public class IndexRequestHandlerTest extends HandlerTest {
  @Test
  public void testIndexRequestHandler() throws Exception {
    IndexRequestHandler handler = createHandler();
    Config config = ConfigParser.parseFile(new File("testdata/modules/plovr-config.js"));
    URI uri = new URI("/");
    HttpExchange ex = createExchange(uri);
    handler.handle(ex);
    Mockito.verify(ex).sendResponseHeaders(Mockito.eq(200), Mockito.anyInt());
  }

  private IndexRequestHandler createHandler() {
    return new IndexRequestHandler(createServer());
  }
}
