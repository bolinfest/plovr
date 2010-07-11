package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

final class HttpUtil {

  private HttpUtil() {}

  public static URI getReferrer(HttpExchange exchange) {
    Headers headers = exchange.getRequestHeaders();
    String referrer = headers.getFirst("Referer");
    if (referrer != null) {
      try {
        return new URI(referrer);
      } catch (URISyntaxException e) {
        // OK
      }
    }
    return null;
  }

  /**
   * Returns a 400 with no message.
   */
  public static void writeNullResponse(HttpExchange exchange) throws IOException {
    writeErrorMessageResponse(exchange, "");
  }

  /**
   * Returns a 400 with the specified message.
   */
  public static void writeErrorMessageResponse(HttpExchange exchange,
      String message) throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    exchange.sendResponseHeaders(400, message.length());

    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(message);
    responseBody.close();
  }

}
