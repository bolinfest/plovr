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
import org.plovr.SoyFile;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Injector;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.base.IncrementingIdGenerator;
import com.google.template.soy.base.SoySyntaxException;
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

  private static final Injector injector = SoyFile.createInjector(
      ImmutableList.of("org.plovr.soy.function.PlovrModule"));

  private final Config config;

  private final SoyTofu tofu;

  public SoyRequestHandler(Config config) {
    this.config = config;
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
    String templateName = namespace + ".base";

    SoyTofu tofu = getSoyTofu();
    final Map<String, ?> data = ImmutableMap.of();
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
    SoyFileSet.Builder builder = injector.getInstance(SoyFileSet.Builder.class);
    builder.setCompileTimeGlobals(config.getCompileTimeGlobals());
    // Add all of the .soy files under config.getContentDirectory().
    addToBuilder(config.getContentDirectory(), builder);
    SoyFileSet soyFileSet = builder.build();
    return soyFileSet.compileToJavaObj();
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
}
