package org.plovr;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.mockito.Mockito;

import java.io.OutputStream;
import java.io.File;
import java.net.URI;

/**
 * Superclass for Http Handler tests.
 */
public class HandlerTest {
  CompilationServer createServer() {
    return new CompilationServer("plovr.org", 1234, false);
  }

  HttpExchange createExchange(URI uri) throws Exception {
    HttpExchange exchange = Mockito.mock(HttpExchange.class);

    Mockito.when(exchange.getRequestMethod()).thenReturn("GET");
    Mockito.when(exchange.getRequestURI()).thenReturn(uri);

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
