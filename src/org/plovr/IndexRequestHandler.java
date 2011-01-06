package org.plovr;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

final class IndexRequestHandler implements HttpHandler {

  private final CompilationServer server;

  public IndexRequestHandler(CompilationServer server) {
    this.server = server;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    if ("GET".equalsIgnoreCase(requestMethod)) {
      doGet(exchange);
    }
  }

  private void doGet(HttpExchange exchange) throws IOException {
    HttpUtil.writeNullResponse(exchange);
  }
}
