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
public class CompileRequestHandlerTest {
  @Test
  public void testCompileModulesAdvanced() throws Exception {
    CompileRequestHandler handler = createHandler();
    Config config = ConfigParser.parseFile(new File("testdata/modules/plovr-config.js"));
    HttpExchange ex = createExchange();
    QueryData qdata = QueryData.createFromUri(new URI("/compile?id=modules"));
    assertEquals(CompilationMode.ADVANCED, config.getCompilationMode());

    handler.doGet(ex, qdata, config);
    Mockito.verify(ex).sendResponseHeaders(Mockito.eq(200), Mockito.anyInt());
  }

  @Test
  public void testCompileModulesRaw() throws Exception {
    CompileRequestHandler handler = createHandler();
    Config config = ConfigParser.parseFile(new File("testdata/modules/plovr-config.js"));
    HttpExchange ex = createExchange();
    QueryData qdata = QueryData.createFromUri(new URI("/compile?id=modules&mode=raw"));
    config = ConfigParser.update(config, qdata);
    assertEquals(CompilationMode.RAW, config.getCompilationMode());

    handler.doGet(ex, qdata, config);
    Mockito.verify(ex).sendResponseHeaders(Mockito.eq(200), Mockito.anyInt());
  }

  private CompileRequestHandler createHandler() {
    CompilationServer server = new CompilationServer("plovr.org", 1234, false);
    return new CompileRequestHandler(server);
  }

  private HttpExchange createExchange() throws Exception {
    HttpExchange exchange = Mockito.mock(HttpExchange.class);

    Mockito.when(exchange.getRequestURI()).thenReturn(new URI("/"));

    Headers headers = Mockito.mock(Headers.class);
    Mockito.when(exchange.getRequestHeaders()).thenReturn(headers);
    Mockito.when(headers.getFirst("Referer")).thenReturn("");
    Mockito.when(headers.getFirst("Host")).thenReturn("localhost");

    Headers resHeaders = Mockito.mock(Headers.class);
    Mockito.when(exchange.getResponseHeaders()).thenReturn(resHeaders);

    OutputStream output = Mockito.mock(OutputStream.class);
    Mockito.when(exchange.getResponseBody()).thenReturn(output);
    return exchange;
  }
}
