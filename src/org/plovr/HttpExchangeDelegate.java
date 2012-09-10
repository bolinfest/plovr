package org.plovr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

/**
 * {@link HttpExchangeDelegate} takes an {@link HttpExchange} to delegate to,
 * but records when certain methods are invoked, such as
 * {@link #sendResponseHeaders(int, long)}, so it can be determined whether
 * it is still possible to write headers or if only the response body can be
 * written.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
class HttpExchangeDelegate extends HttpExchange {

  private final HttpExchange exchange;
  private boolean haveResponseHeadersBeenSent = false;
  
  HttpExchangeDelegate(HttpExchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public void close() {
    exchange.close();
  }

  @Override
  public boolean equals(Object obj) {
    return exchange.equals(obj);
  }

  @Override
  public Object getAttribute(String arg0) {
    return exchange.getAttribute(arg0);
  }

  @Override
  public HttpContext getHttpContext() {
    return exchange.getHttpContext();
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    return exchange.getLocalAddress();
  }

  @Override
  public HttpPrincipal getPrincipal() {
    return exchange.getPrincipal();
  }

  @Override
  public String getProtocol() {
    return exchange.getProtocol();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return exchange.getRemoteAddress();
  }

  @Override
  public InputStream getRequestBody() {
    return exchange.getRequestBody();
  }

  @Override
  public Headers getRequestHeaders() {
    return exchange.getRequestHeaders();
  }

  @Override
  public String getRequestMethod() {
    return exchange.getRequestMethod();
  }

  @Override
  public URI getRequestURI() {
    return exchange.getRequestURI();
  }

  @Override
  public OutputStream getResponseBody() {
    return exchange.getResponseBody();
  }

  @Override
  public int getResponseCode() {
    return exchange.getResponseCode();
  }

  @Override
  public Headers getResponseHeaders() {
    return exchange.getResponseHeaders();
  }

  @Override
  public int hashCode() {
    return exchange.hashCode();
  }

  @Override
  public void sendResponseHeaders(int arg0, long arg1) throws IOException {
    exchange.sendResponseHeaders(arg0, arg1);
    haveResponseHeadersBeenSent = true;
  }
  
  public boolean haveResponseHeadersBeenSent() {
    return haveResponseHeadersBeenSent;
  }

  @Override
  public void setAttribute(String arg0, Object arg1) {
    exchange.setAttribute(arg0, arg1);
  }

  @Override
  public void setStreams(InputStream arg0, OutputStream arg1) {
    exchange.setStreams(arg0, arg1);
  }

  @Override
  public String toString() {
    return exchange.toString();
  }
}
