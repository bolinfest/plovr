package org.plovr;

import static org.junit.Assert.assertEquals;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * {@link CompilationServerTest} is a unit test for {@CompilationServer}.
 *
 * @author nicholas.j.santos@gmail.com
 */
public class CompilationServerTest {

  @Test
  public void testServerForExchange() {
    CompilationServer server = new CompilationServer("plovr.org", 1234, false);
    assertEquals("http://localhost:1234/", server.getServerForExchange(createExchange(null, null)));
    assertEquals("http://standard.dev:2345/",
                 server.getServerForExchange(createExchange("standard.dev:2345", null)));
    assertEquals("https://standard.dev:2345/",
                 server.getServerForExchange(createExchange("standard.dev:2345", "https://standard.local:4567")));
    assertEquals("http://standard.local:1234/",
                 server.getServerForExchange(createExchange(null, "http://standard.local:4567")));
    assertEquals("http://standard.dev/", server.getServerForExchange(createExchange("standard.dev", null)));
  }

  private HttpExchange createExchange(String host, String referer) {
    HttpExchange exchange = Mockito.mock(HttpExchange.class);
    Headers headers = Mockito.mock(Headers.class);
    Mockito.when(exchange.getRequestHeaders()).thenReturn(headers);
    Mockito.when(headers.getFirst("Referer")).thenReturn(referer);
    Mockito.when(headers.getFirst("Host")).thenReturn(host);
    return exchange;
  }
}
