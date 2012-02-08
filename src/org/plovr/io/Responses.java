package org.plovr.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.plovr.Config;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public final class Responses {

  /** Utility class: do not instantiate. */
  private Responses() {}

  /**
   * Writes a 200 response with the specified JavaScript content using the
   * appropriate headers and character encoding.
   * Once this method is called, nothing else may be written as it closes the
   * response.
   * @param js The JavaScript code to write to the response.
   * @param exchange to which the response will be written -- no content may
   *     have been written to its response yet as this method sets headers
   */
  public static void writeJs(
      String js, Config config, HttpExchange exchange)
  throws IOException {
    // Write the Content-Type and Content-Length headers.
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", config.getJsContentType());
    byte[] bytes = js.getBytes(config.getOutputCharset());
    exchange.sendResponseHeaders(200, bytes.length);

    // Write the JavaScript code to the response and close it.
    OutputStream output = new BufferedOutputStream(exchange.getResponseBody());
    output.write(bytes);
    output.close();
  }

  public static void writePlainText(String text, HttpExchange exchange)
  throws IOException {
    writeText(text, "text/plain", exchange);
  }

  public static void writeHtml(String html, HttpExchange exchange)
  throws IOException {
    writeText(html, "text/html", exchange);
  }

  public static void writeCss(String css, HttpExchange exchange)
  throws IOException {
    writeText(css, "text/css", exchange);
  }

  private static void writeText(String text, String contentType,
      HttpExchange exchange) throws IOException {
    // Write the Content-Type and Content-Length headers.
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", contentType);
    exchange.sendResponseHeaders(200, text.length());

    // Write the plain text to the response and close it.
    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(text);
    responseBody.close();
  }

  public static void redirect(HttpExchange exchange, String uri)
      throws IOException {
    exchange.getResponseHeaders().add("Location", uri);
    exchange.sendResponseHeaders(302, -1);
  }

  public static void notModified(HttpExchange exchange) throws IOException {
    exchange.sendResponseHeaders(304, -1);
  }
}
