package plovr.io;

import java.io.IOException;
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
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", config.getJsContentType());
    int responseLength = js.getBytes(config.getOutputCharset()).length;
    exchange.sendResponseHeaders(200, responseLength);

    Writer responseBody = Streams.createOutputStreamWriter(
        exchange.getResponseBody(), config);
    responseBody.write(js);
    responseBody.close();
  }
}
