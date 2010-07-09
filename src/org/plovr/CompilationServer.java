package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class CompilationServer implements Runnable {

  private static final Logger logger =
      Logger.getLogger("org.plovr.CompilationServer");

  private final Map<String, Config> configMap;

  private final int port;

  CompilationServer(int port) {
    this.configMap = Maps.newHashMap();
    this.port = port;
  }

  public void registerConfig(Config config) {
    String id = config.getId();
    if (configMap.containsKey(id)) {
      throw new IllegalArgumentException(
          "A config with this id has already been registered: " + id);
    }
    configMap.put(id, config);
  }

  @Override
  public void run() {
    InetSocketAddress addr = new InetSocketAddress(port);
    HttpServer server;
    try {
      server = HttpServer.create(addr, 0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    server.createContext("/config", new ConfigRequestHandler());
    server.createContext("/input", new InputFileHandler(this));
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
  }

  Config getConfigById(String id) {
    return configMap.get(id);
  }

  private class ConfigRequestHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String requestMethod = exchange.getRequestMethod();
      if (requestMethod.equalsIgnoreCase("GET")) {
        doGet(exchange);
      }
    }

    private void doGet(HttpExchange exchange) throws IOException {
      QueryData data = QueryData.createFromUri(exchange.getRequestURI());
      String id = data.getParam("id");

      StringBuilder builder = new StringBuilder();
      String contentType;
      int responseCode;
      if (id != null && configMap.containsKey(id)) {
        Config config = new Config(configMap.get(id));

        // First, use query parameters from the script tag to update the config.
        update(config, data);

        // If supported, modify query parameters based on the referrer. This is
        // more convenient for the developer, so it should be used to override.
        URI referrer = null;
        if (!config.isUseExplicitQueryParameters()) {
          referrer = getReferrer(exchange);
          if (referrer != null) {
            QueryData referrerData = QueryData.createFromUri(referrer);
            update(config, referrerData);
          }
        }

        if (config.getCompilationMode() == CompilationMode.RAW) {
          String prefix;
          URI requestUri = exchange.getRequestURI();
          if (referrer == null) {
            prefix = "http://localhost:9810/";
          } else {
            prefix = referrer.getScheme() + "://" + referrer.getHost() + ":9810/";
          }
          Manifest manifest = config.getManifest();
          String js = InputFileHandler.getJsToLoadManifest(config.getId(), manifest, prefix,
              requestUri.getPath());
          builder.append(js);
        } else {
          compile(config, data, builder);
        }
        contentType = "text/javascript";
        responseCode = 200;
      } else {
        builder.append("Failed to specify a valid config id.");
        contentType = "text/plain";
        responseCode = 400;
      }

      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", contentType);
      exchange.sendResponseHeaders(responseCode, builder.length());

      Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
      responseBody.write(builder.toString());
      responseBody.close();
    }

    private void compile(Config config, QueryData data, Appendable builder)
        throws IOException {
      CompilerArguments compilerArguments =
          config.getManifest().getCompilerArguments();
      Compiler compiler = new Compiler();

      CompilerOptions options = config.getCompilerOptions();
      Result result;
      try {
        result = compiler.compile(compilerArguments.getExterns(),
            compilerArguments.getInputs(), options);
      } catch (Throwable t) {
        logger.severe(t.getMessage());
        result = null;
      }

      if (result.success) {
        if (config.getCompilationMode() == CompilationMode.WHITESPACE) {
          builder.append("CLOSURE_NO_DEPS = true;\n");
        }
        builder.append(compiler.toSource());
      } else {
        JsonArray array = new JsonArray();
        for (JSError warning : result.warnings) {
          array.add(new JsonPrimitive(warning.toString()));
        }
        for (JSError error : result.errors) {
          array.add(new JsonPrimitive(error.toString()));
        }
        builder.append(array.toString());
      }
    }
  }

  private static void update(Config config, QueryData queryData) {
    // TODO(bolinfest): Allow user to specify more detailed CompilerOptions
    // via the Config.

    String mode = queryData.getParam("mode");
    if (mode != null) {
      try {
        CompilationMode compilationMode = CompilationMode.valueOf(mode.
            toUpperCase());
        config.setCompilationMode(compilationMode);
      } catch (IllegalArgumentException e) {
        // OK
      }
    }
  }

  private static URI getReferrer(HttpExchange exchange) {
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
}
