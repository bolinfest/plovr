package org.plovr.soy.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import com.google.common.io.Files;
import com.google.inject.internal.ImmutableMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * {@link RequestHandlerSelector} selects the {@link HttpHandler} to use
 * to respond to a request based on the path.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class RequestHandlerSelector implements HttpHandler {

  private final Map<String, String> extensionToContentType;

  private final Config config;

  private final SoyRequestHandler soyRequestHandler;

  public RequestHandlerSelector(Config config) {
    this.config = config;
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    // TODO(bolinfest): Read this out of a config file.
    builder.put(".css", "text/css");
    builder.put(".html", "text/html");
    builder.put(".js", "text/javascript");
    builder.put(".png", "image/png");
    extensionToContentType = builder.build();

    soyRequestHandler = new SoyRequestHandler(config);
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    URI uri = exchange.getRequestURI();
    String path = uri.getPath();

    // Special case index.html.
    File contentDir = config.getContentDirectory();
    if (path.endsWith("/")) {
      if ((new File(contentDir, path + "index.html")).exists()) {
        path += "index.html";
      }
    }

    String extension = getFileExtension(path);

    // If this appears to be a file with static content, then serve the contents
    // of the file directly.
    if (extension != null) {
      String contentType = extensionToContentType.get(extension);
      File staticContent = new File(contentDir, path);
      if (contentType != null && staticContent.exists()) {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, staticContent.length());

        byte[] bytes = Files.toByteArray(staticContent);
        OutputStream output = exchange.getResponseBody();
        output.write(bytes);
        output.close();
      } else {
        // Send empty response.
        exchange.sendResponseHeaders(200, 0);
        exchange.getRequestBody().close();
      }
    } else {
      soyRequestHandler.handle(exchange);
    }
  }

  static String getFileExtension(String path) {
    if (path == null) {
      return null;
    }

    int slashIndex = path.lastIndexOf("/");
    path = path.substring(slashIndex + 1);

    int dotIndex = path.lastIndexOf(".");
    if (dotIndex < 0) {
      return null;
    } else {
      return path.substring(dotIndex);
    }
  }

}
