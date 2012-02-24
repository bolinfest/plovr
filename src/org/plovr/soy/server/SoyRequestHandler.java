package org.plovr.soy.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.plovr.HttpUtil;
import org.plovr.QueryData;
import org.plovr.util.SoyDataUtil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Injector;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.base.IncrementingIdGenerator;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.restricted.StringData;
import com.google.template.soy.soyparse.ParseException;
import com.google.template.soy.soyparse.SoyFileParser;
import com.google.template.soy.soyparse.TokenMgrError;
import com.google.template.soy.soytree.SoyFileNode;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * {@link SoyRequestHandler} handles a request for a Soy file and prints the
 * contents of its base template, if available.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class SoyRequestHandler implements HttpHandler {

  private static final Logger logger = Logger.getLogger(SoyRequestHandler.class.getName());

  private static final JsonParser parser = new JsonParser();

  private final Config config;

  /**
   * The name of the template to render within a Soy namespace/file.
   */
  private final String templateToRender;

  private final SoyTofu tofu;

  public SoyRequestHandler(Config config) {
    this.config = config;
    this.templateToRender = config.getTemplateToRender();
    this.tofu = config.isStatic() ? getSoyTofu(config) : null;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      doHandle(exchange);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "IO Error: response may not have been written", e);
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Error generating HTML from Soy", t);

      // No response should have been written yet if a Throwable is thrown.
      String message = t.getMessage();

      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/plain");
      exchange.sendResponseHeaders(404, message.length());

      Writer writer = new OutputStreamWriter(exchange.getResponseBody());
      writer.write(message);
      writer.close();
    }
  }

  private void doHandle(HttpExchange exchange) throws IOException,
      SoySyntaxException, TokenMgrError, ParseException {
    URI uri = exchange.getRequestURI();
    String path = uri.getPath();

    // If the request is to a Soy file whose name starts with a double
    // underscore, then do not serve it as it should be considered private.
    String name = path.substring(path.lastIndexOf("/") + 1);
    if (name.startsWith("__")) {
      HttpUtil.writeNotFound(exchange);
      return;
    }

    // For a request to the root directory, use index.soy, if available.
    if (path.endsWith("/")) {
      path += "/index";
    }

    // This was likely a redirect from RequestHandlerSelector where an existing
    // HTML file is now backed by a Soy template.
    if (path.endsWith(".html")) {
      // Remove the HTML suffix here so ".soy" can be added to the path.
      path = path.replaceFirst("\\.html$", "");
    }

    String relativePath = path + ".soy";
    File soyFile = new File(config.getContentDirectory(), relativePath);
    if (!soyFile.exists()) {
      HttpUtil.return404(exchange);
      return;
    }

    SoyFileParser parser = new SoyFileParser(
        Files.newReader(soyFile, Charsets.UTF_8),
        new IncrementingIdGenerator(),
        relativePath);
    SoyFileNode node = parser.parseSoyFile();

    String namespace = node.getNamespace();
    String templateName = namespace + "." + templateToRender;

    SoyTofu tofu = getSoyTofu();
    final Map<String, ?> data = createSoyData(exchange);
    String html = tofu.newRenderer(templateName).setData(data).render();

    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");
    exchange.sendResponseHeaders(200, html.length());

    Writer writer = new OutputStreamWriter(exchange.getResponseBody());
    writer.write(html);
    writer.close();
  }

  private SoyTofu getSoyTofu() {
    // tofu is non-null when config.useDynamicRecompilation() is false.
    if (tofu != null) {
      return tofu;
    } else {
      return getSoyTofu(config);
    }
  }

  private static SoyTofu getSoyTofu(Config config) {
    Injector injector = config.getInjector();
    SoyFileSet.Builder builder = injector.getInstance(SoyFileSet.Builder.class);
    builder.setCompileTimeGlobals(config.getCompileTimeGlobals());
    // Add all of the .soy files under config.getContentDirectory().
    addToBuilder(config.getContentDirectory(), builder);
    SoyFileSet soyFileSet = builder.build();
    return soyFileSet.compileToTofu();
  }

  private static void addToBuilder(File file, SoyFileSet.Builder builder) {
    if (file.isDirectory()) {
      for (File entry : file.listFiles()) {
        addToBuilder(entry, builder);
      }
    } else {
      if (file.isFile() && file.getName().endsWith(".soy")) {
        builder.add(file);
      }
    }
  }

  private Map<String, ?> createSoyData(HttpExchange exchange) {
    if (config.isSafeMode()) {
      return ImmutableMap.of();
    } else {
      return createSoyDataFromUri(exchange.getRequestURI());
    }
  }

  @VisibleForTesting
  static Map<String, SoyData> createSoyDataFromUri(URI uri) {
    QueryData queryData = QueryData.createFromUri(uri);
    Map<String, SoyData> soyData = Maps.newHashMap();
    for (String param : queryData.getParams()) {
      String queryParamValue = queryData.getLastValueForParam(param);
      soyData.put(param, getValueForQueryParam(queryParamValue));
    }
    return ImmutableMap.copyOf(soyData);
  }

  @VisibleForTesting
  static SoyData getValueForQueryParam(String param) {
    JsonElement jsonElement;
    try {
      jsonElement = parser.parse(param);
    } catch (JsonSyntaxException e) {
      return StringData.forValue(param);
    }

    // Gson is a little more liberal than we would like when it comes to parsing
    // booleans. Only allow "true" and "false" to be converted to BooleanData.
    if (jsonElement.isJsonPrimitive() &&
        jsonElement.getAsJsonPrimitive().isBoolean()) {
      if (!("true".equals(param) || "false".equals(param))) {
        return StringData.forValue(param);
      }
    }

    // Sometimes, Gson will return a Json null if it cannot parse something
    // rather than throw a JsonSyntaxException. Make sure "null" was actually
    // specified.
    if (jsonElement.isJsonNull() && !param.equals("null")) {
      return StringData.forValue(param);
    }

    return SoyDataUtil.jsonToSoyData(jsonElement);
  }
}
