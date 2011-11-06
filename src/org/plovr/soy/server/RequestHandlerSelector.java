package org.plovr.soy.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Nullable;

import org.plovr.HttpUtil;

import com.google.common.base.Ascii;
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

  /**
   * This is the MIME type returned by
   * {@link MimetypesFileTypeMap#getContentType(String)} if no entry is found
   * in the map.
   */
  private static final String UNKNOWN_MIME_TYPE = "application/octet-stream";

  private final Map<String, String> extensionToContentType;

  private final Config config;

  private final SoyRequestHandler soyRequestHandler;

  private final DirectoryHandler directoryHandler;

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
    directoryHandler = new DirectoryHandler(config);
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    File staticContent = getCorrespondingFileForRequest(config, exchange);
    if (staticContent == null) {
      // A 403 response was already written.
      return;
    }

    if (staticContent.exists() && staticContent.isDirectory()) {
      // Special case index.html.
      File contentDir = config.getContentDirectory();
      URI uri = exchange.getRequestURI();
      File indexHtmlFile = new File(contentDir, uri.getPath() + "index.html");
      File indexSoyFile = new File(contentDir, uri.getPath() + "index.soy");
      if (indexHtmlFile.exists() || indexSoyFile.exists()) {
        staticContent = indexHtmlFile;
      } else {
        // If there is no index.html or index.soy and directory listing is
        // enabled, display a list of the files under the requested directory as
        // HTML.
        directoryHandler.handle(exchange);
        return;
      }
    }

    String extension = getFileExtension(staticContent.toString());

    // If the request is for an HTML file but no HTML file exists at that path,
    // try to fall back on a Soy file with the same name. This feature makes it
    // easier to convert an HTML file to a template without having to create a
    // redirect.
    boolean trySoyInstead = !staticContent.exists() && ".html".equals(
        extension);

    if (trySoyInstead) {
      soyRequestHandler.handle(exchange);
    } else if (!staticContent.exists()) {
      HttpUtil.return404(exchange);
    } else {
      byte[] bytes = Files.toByteArray(staticContent);

      // This appears to be a file with static content: serve the contents of
      // the file directly.
      String contentType = extensionToContentType.get(extension);
      if (contentType == null) {
        contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().
            getContentType(staticContent.toString());
        if (UNKNOWN_MIME_TYPE.equals(contentType) && !containsNul(bytes)) {
          // As a coarse heuristic, when the MIME type is unknown, if the file
          // does not contain a \0, use 'text/plain' instead of
          // 'application/octet-stream'. It is really annoying to visit a URL
          // for a file of source code and ask to download it rather than view
          // it as plaintext in the browser when it is .sh, .py, .java, .etc.
          contentType = "text/plain";
        }
      }

      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", contentType);
      exchange.sendResponseHeaders(200, staticContent.length());

      OutputStream output = exchange.getResponseBody();
      output.write(bytes);
      output.close();
    }
  }

  /**
   * Returns the file that corresponds to the request. If the file is not under
   * the directory that is being served by SoyWeb, then a 403 response is
   * written and this method will return {@code null}. The returned
   * {@link File} object may not represent a file that actually exists.
   */
  static @Nullable File getCorrespondingFileForRequest(Config config,
      HttpExchange exchange) throws IOException {
    URI uri = exchange.getRequestURI();
    File contentDir = config.getContentDirectory();
    File staticContent = new File(contentDir, uri.getPath());

    if (!FileUtil.contains(contentDir, staticContent)) {
      // Someone is trying to pull a fast one! The request URI might be
      // something like: "/../../../etc/passwd", so do not allow requests to
      // files above the content directory.
      HttpUtil.writeHtmlErrorMessageResponse(exchange,
          "You do not have permission to access the requested file.",
          403);
      return null;
    } else {
      return staticContent;
    }
  }

  private static boolean containsNul(byte[] bytes) {
    for (byte b : bytes) {
      if (b == Ascii.NUL) {
        return true;
      }
    }
    return false;
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
