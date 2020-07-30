package org.plovr;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;

import java.io.OutputStream;
import java.io.File;
import java.net.URI;

/**
 * {@link CompileRequestHandlerTest} is a unit test for {@link CompileRequestHandler}.
 */
public class CompileRequestHandlerTest extends HandlerTest {
  @Test
  public void testCompileModulesAdvanced() throws Exception {
    CompileRequestHandler handler = createHandler();
    Config config = ConfigParser.parseFile(new File("testdata/modules/plovr-config.js"));
    URI uri = new URI("/compile?id=modules");
    HttpExchange ex = createExchange(uri);
    QueryData qdata = QueryData.createFromUri(uri);
    assertEquals(CompilationMode.ADVANCED, config.getCompilationMode());

    handler.doGet(ex, qdata, config);
    Mockito.verify(ex).sendResponseHeaders(Mockito.eq(200), Mockito.anyInt());
  }

  @Test
  public void testCompileModulesRaw() throws Exception {
    CompileRequestHandler handler = createHandler();
    Config config = ConfigParser.parseFile(new File("testdata/modules/plovr-config.js"));
    URI uri = new URI("/compile?id=modules&mode=raw");
    HttpExchange ex = createExchange(uri);
    QueryData qdata = QueryData.createFromUri(uri);
    config = ConfigParser.update(config, qdata);
    assertEquals(CompilationMode.RAW, config.getCompilationMode());

    handler.doGet(ex, qdata, config);
    Mockito.verify(ex).sendResponseHeaders(Mockito.eq(200), Mockito.anyInt());
  }

  private CompileRequestHandler createHandler() {
    return new CompileRequestHandler(createServer());
  }
}
