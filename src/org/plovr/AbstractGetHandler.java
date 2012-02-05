package org.plovr;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


abstract class AbstractGetHandler implements HttpHandler {

  private static final Logger logger = Logger.getLogger(
      "org.plovr.AbstractGetHandler");

  protected final CompilationServer server;

  private final boolean usesRestfulPath;

  AbstractGetHandler(CompilationServer server) {
    this(server, false /* usesRestfulPath */);
  }

  /**
   * @param server
   * @param usesRestfulPath If true, then the config id will be specified by a
   *     query parameter named "id". If false, then the config id will be
   *     embedded in the URL as follows:
   *     http://localhost:9810/command_name/config_id/other/args?like=this
   */
  AbstractGetHandler(CompilationServer server, boolean usesRestfulPath) {
    this.server = server;
    // TODO(bolinfest): Consider switching all handlers to use RESTful URLs.
    this.usesRestfulPath = usesRestfulPath;
  }

  @Override
  public final void handle(HttpExchange ex) throws IOException {
    HttpExchangeDelegate exchange = new HttpExchangeDelegate(ex);
    String requestMethod = exchange.getRequestMethod();
    if (requestMethod.equalsIgnoreCase("GET")) {
      try {
        QueryData queryData = QueryData.createFromUri(exchange.getRequestURI());
        String id;
        if (usesRestfulPath) {
          id = parseConfigIdFromRestUri(exchange.getRequestURI());
        } else {
          id = queryData.getParam("id");
        }
        if (id == null) {
          HttpUtil.writeNullResponse(exchange);
        } else {
          Config config = server.getConfigById(id);
          if (config == null) {
            HttpUtil.writeErrorMessageResponse(exchange,
                "Unknown configuration id: " + id);
            return;
          }

          // First, use query parameters from the script tag to update the config.
          config = ConfigParser.update(config, queryData);

          // Modify query parameters based on the referrer. This is more
          // convenient for the developer, so it should be used to override
          // the default settings.
          URI referrer = HttpUtil.getReferrer(exchange);
          if (referrer != null) {
            QueryData referrerData = QueryData.createFromUri(referrer);
            config = ConfigParser.update(config, referrerData);
          }

          // Set the cache headers
          setCacheHeaders(exchange.getResponseHeaders());

          doGet(exchange, queryData, config);
        }
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Error during GET request to " + exchange.getRequestURI(), t);

        // Even though there has been an error, it is important to write a
        // response or else the client will hang.
        if (exchange.haveResponseHeadersBeenSent()) {
          // If the response headers have already been sent, then just close
          // whatever has been written to the response.
          Closeables.closeQuietly(exchange.getResponseBody());
        } else {
          HttpUtil.writeErrorMessageResponse(exchange, t.getMessage());
        }
      }
    }
  }

  /**
   * Sets the cache headers to disable caching of resources.
   * See http://code.google.com/p/doctype/wiki/ArticleHttpCaching
   */
  protected void setCacheHeaders(Headers headers) {
    DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    format.setTimeZone(TimeZone.getTimeZone("GMT"));

    headers.set("Date", format.format(new Date()));
    headers.set("Expires", "Fri, 01 Jan 1990 00:00:00 GMT");
    headers.set("Pragma", "no-cache");
    headers.set("Cache-control", "no-cache, must-revalidate");
  }

  /**
   * Regex (as text) to match a config id. Stored as a string so it can be
   * included in other regexes.
   */
  static final String CONFIG_ID_PATTERN = "[\\w-]+";

  /**
   * Pattern used to select the handler name and config id from a plovr URI path.
   * The first group is the handler name and the second group is the config id.
   */
  private static final Pattern URI_ID_PATTERN = Pattern.compile(
      "/(\\w+)/(" + CONFIG_ID_PATTERN + ")/.*");

  @VisibleForTesting
  static String parseConfigIdFromRestUri(URI uri) {
    String path = uri.getPath();
    Matcher matcher = URI_ID_PATTERN.matcher(path);
    if (matcher.matches()) {
      return matcher.group(2);
    } else {
      return null;
    }
  }

  /**
   * Successfully returns a {@link Compilation} (and records it as the latest
   * {@link Compilation} for the config), or returns null, indicating that no
   * {@link Compilation} could be found and that an error message was already
   * written.
   */
  protected final @Nullable Compilation getCompilation(
      HttpExchange exchange, QueryData data, Config config) throws IOException{
    final boolean recordCompilation = true;
    return getCompilation(exchange, data, config, recordCompilation);
  }

  protected final @Nullable Compilation getCompilation(
      HttpExchange exchange,
      QueryData data,
      Config config,
      boolean recordCompilation) throws IOException {
    // Make sure that RAW mode was not used.
    CompilationMode mode = config.getCompilationMode();
    if (mode == CompilationMode.RAW) {
      HttpUtil.writeErrorMessageResponse(exchange,
          "Not applicable for RAW mode");
      return null;
    }

    // Make sure that this code has been compiled.
    try {
      return CompilationUtil.getCompilationOrFail(server, config, recordCompilation);
    } catch (CompilationException e) {
      logger.log(Level.SEVERE, "Error during compilation", e);
      HttpUtil.writeNullResponse(exchange);
      return null;
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
