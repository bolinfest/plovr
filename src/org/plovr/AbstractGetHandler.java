package org.plovr;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


abstract class AbstractGetHandler implements HttpHandler {

  private static final Logger logger = Logger.getLogger(
      "org.plovr.AbstractGetHandler");

  protected final CompilationServer server;

  AbstractGetHandler(CompilationServer server) {
    this.server = server;
  }

  public final void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    if (requestMethod.equalsIgnoreCase("GET")) {
      try {
        doGet(exchange);
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Error during GET request", t);
      }
    }
  }

  protected abstract void doGet(HttpExchange exchange) throws IOException;

}
