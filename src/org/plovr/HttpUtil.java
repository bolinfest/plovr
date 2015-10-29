package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import com.google.common.io.Resources;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public final class HttpUtil {

  private static final SoyTofu TOFU;

  static {
    SoyFileSet.Builder builder = SoyFileSet.builder();
    builder.add(Resources.getResource(InputFileHandler.class, "400.soy"));
    SoyFileSet fileSet = builder.build();
    TOFU = fileSet.compileToTofu();
  }

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
   * @return The Host header, parsed into a string so that we can
   * get the hostname and port.
   */
  public static URI getHost(HttpExchange exchange) {
    Headers headers = exchange.getRequestHeaders();
    String host = headers.getFirst("Host");
    if (host != null) {
      try {
        return new URI("http://" + host);
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

  public static void writeNotFound(HttpExchange exchange) throws IOException {
    writeHtmlErrorMessageResponse(exchange, "<h1>Not Found</h1>", 404);
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

  public static void return404(HttpExchange exchange) throws IOException {
    HttpUtil.writeHtmlErrorMessageResponse(
        exchange, "File Not Found", 404);
  }

  public static void writeHtmlErrorMessageResponse(HttpExchange exchange,
      String htmlMessage, int errorCode) throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");

    SoyMapData mapData = new SoyMapData("htmlMessage", htmlMessage);
    String message = TOFU.newRenderer("org.plovr.errorPage").
        setData(mapData).render();

    exchange.sendResponseHeaders(errorCode, message.length());
    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(message);
    responseBody.close();
  };

  /**
   * Returns a 400 with the specified HTML message.
   */
  public static void writeHtmlErrorMessageResponse(HttpExchange exchange,
      String htmlMessage) throws IOException {
    writeHtmlErrorMessageResponse(exchange, htmlMessage, 400);
  }
}
