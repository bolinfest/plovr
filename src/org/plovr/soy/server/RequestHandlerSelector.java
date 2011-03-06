package org.plovr.soy.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import org.plovr.HttpUtil;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
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
    builder.put(".gif", "image/gif");
    builder.put(".html", "text/html");
    builder.put(".jpeg", "image/jpeg");
    builder.put(".jpg", "image/jpeg");
    builder.put(".js", "text/javascript");
    builder.put(".png", "image/png");
    builder.put(".sh", "text/plain");
    builder.put(".txt", "text/plain");
    builder.put(".ttf", "font/ttf");
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
      // TODO(bolinfest): If there is no index.html or index.soy and
      // directory listing is enabled, display a list of the files
      // under the requested directory as HTML.
    }

    String extension = getFileExtension(path);

    // If the request is for an HTML file but no HTML file exists at that path,
    // try to fall back on a Soy file with the same name. This feature makes it
    // easier to convert an HTML file to a template without having to create a
    // redirect.
    File staticContent = new File(contentDir, path);

    if (!FileUtil.contains(contentDir, staticContent)) {
      // Someone is trying to pull a fast one! The request URI might be
      // something like: "/../../../etc/passwd", so do not allow requests to
      // files above the content directory.
      HttpUtil.writeHtmlErrorMessageResponse(exchange,
          "You do not have permission to access the requested file.",
          403);
      return;
    }

    boolean trySoyInstead = !staticContent.exists() && ".html".equals(extension);

    if (!trySoyInstead && extension != null) {
      // If this appears to be a file with static content, then serve the
      // contents of the file directly.
      String contentType = extensionToContentType.get(extension);
      if (staticContent.exists()) {
        if (contentType != null) {
          Headers responseHeaders = exchange.getResponseHeaders();
          responseHeaders.set("Content-Type", contentType);
          exchange.sendResponseHeaders(200, staticContent.length());
        }

        byte[] bytes = Files.toByteArray(staticContent);
        OutputStream output = exchange.getResponseBody();
        output.write(bytes);
        output.close();
      } else {
        HttpUtil.return404(exchange);
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
