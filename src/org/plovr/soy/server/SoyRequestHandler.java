package org.plovr.soy.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.base.IntegerIdGenerator;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.msgs.SoyMsgBundle;
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

  private final Config config;

  public SoyRequestHandler(Config config) {
    this.config = config;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      doHandle(exchange);
    } catch (SoyRequestHandlerException e) {
      // No response should have been written yet if SoyRequestHandlerException
      // is thrown.
      String message = e.getMessage();

      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/plain");
      exchange.sendResponseHeaders(404, message.length());

      Writer writer = new OutputStreamWriter(exchange.getResponseBody());
      writer.write(message);
      writer.close();
    }
  }

  private void doHandle(HttpExchange exchange) throws IOException,
      SoyRequestHandlerException {
    URI uri = exchange.getRequestURI();
    String path = uri.getPath();

    File soyFile = new File(config.getContentDirectory(), path + ".soy");
    if (!soyFile.exists()) {
      throw new SoyRequestHandlerException(path + ".soy does not exist");
    }

    SoyFileParser parser = new SoyFileParser(
        Files.newReader(soyFile, Charsets.UTF_8),
        new IntegerIdGenerator());
    SoyFileNode node;
    try {
      node = parser.parseSoyFile();
    } catch (SoySyntaxException e) {
      throw new SoyRequestHandlerException(e);
    } catch (TokenMgrError e) {
      throw new SoyRequestHandlerException(e);
    } catch (ParseException e) {
      throw new SoyRequestHandlerException(e);
    } catch (Throwable t) {
      // For debugging purposes.
      throw new SoyRequestHandlerException(t);
    }

    String namespace = node.getNamespace();
    String templateName = namespace + ".base";

    SoyTofu tofu = getSoyTofu();
    final Map<String, ?> data = ImmutableMap.of();
    final SoyMsgBundle msgBundle = null;
    String html = tofu.render(templateName, data, msgBundle);

    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");
    exchange.sendResponseHeaders(200, html.length());

    Writer writer = new OutputStreamWriter(exchange.getResponseBody());
    writer.write(html);
    writer.close();
  }

  private SoyTofu getSoyTofu() throws SoyRequestHandlerException {
    if (config.useDynamicRecompilation()) {
      SoyFileSet.Builder builder = new SoyFileSet.Builder();
      // Add all of the .soy files under config.getContentDirectory().
      addToBuilder(config.getContentDirectory(), builder);
      SoyFileSet soyFileSet = builder.build();
      return soyFileSet.compileToJavaObj();
    } else {
      throw new SoyRequestHandlerException("dynamic recompilation is not supported yet");
    }
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

  private static final class SoyRequestHandlerException extends Exception {
    private SoyRequestHandlerException(String message) {
      super(message);
    }
    private SoyRequestHandlerException(Throwable t) {
      super(t);
    }
  }
}
