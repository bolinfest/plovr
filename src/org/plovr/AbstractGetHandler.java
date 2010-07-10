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
        QueryData queryData = QueryData.createFromUri(exchange.getRequestURI());
        String id = queryData.getParam("id");
        if (id == null) {
          HttpUtil.writeNullResponse(exchange);
        } else {
          Config config = server.getConfigById(id);
          if (config == null) {
            HttpUtil.writeShortResponse(exchange,
                "Unknown configuration id: " + id);
          } else {
            doGet(exchange, queryData, config);
          }
        }
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Error during GET request to " + exchange.getRequestURI(), t);
        // TODO(bolinfest): Write/flush response.
      }
    }
  }

  /**
   * All parameters are guaranteed to be non-null.
   * @param exchange
   * @param data
   * @param config
   * @throws IOException
   */
  protected abstract void doGet(HttpExchange exchange, QueryData data,
      Config config) throws IOException;
}
