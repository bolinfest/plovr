package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

final class ExternsHandler extends AbstractGetHandler {

  public ExternsHandler(CompilationServer server) {
    super(server);
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    String externs = server.getExportsAsExternsFor(config);
    if (externs == null) {
      HttpUtil.writeErrorMessageResponse(exchange,
          "No externs found -- perhaps you have not compiled yet?");
    } else {
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/plain");
      exchange.sendResponseHeaders(200, externs.length());
      Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
      responseBody.write(externs);
      responseBody.close();
    }
  }

}
