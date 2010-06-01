package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class CompilationServer implements Runnable {

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
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
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
      int responseCode;
      if (id != null && configMap.containsKey(id)) {
        responseCode = 200;
        Config config = configMap.get(id);
        compile(config, data, builder);
      } else {
        responseCode = 400;
        builder.append("Failed to specify a valid config id.");
      }
      
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/plain");
      exchange.sendResponseHeaders(responseCode, builder.length());
      
      Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
      responseBody.write(builder.toString());
      responseBody.close();
    }
    
    private void compile(Config config, QueryData data, Appendable builder) throws IOException {
      CompilerArguments compilerArguments = config.getManifest().getCompilerArguments();
      Compiler compiler = new Compiler();
      
      // TODO(bolinfest): Allow user to specify more detailed CompilerOptions
      // via the Config.
      CompilationLevel level = CompilationLevel.SIMPLE_OPTIMIZATIONS;
      String levelValue = data.getParam("level");
      if (levelValue != null) {
        try {
          level = CompilationLevel.valueOf(levelValue);
        } catch (IllegalArgumentException e) {
          // Ignore, use default value.
        }
      }
      CompilerOptions options = new CompilerOptions();
      level.setOptionsForCompilationLevel(options);
      
      Result result = compiler.compile(compilerArguments.getExterns(),
          compilerArguments.getInputs(), options);

      if (result.success) {
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

  private static class QueryData {
    
    LinkedListMultimap<String, String> params;

    private QueryData(LinkedListMultimap<String, String> params) {
      this.params = params;
    }
    
    String getParam(String key) {
      List<String> values = params.get(key);
      return values.size() > 0 ? values.get(0) : null;
    }

    static QueryData createFromUri(URI uri) {
      String rawQuery = uri.getRawQuery();
      LinkedListMultimap<String, String> params = LinkedListMultimap.create();
      String[] pairs = rawQuery.split("&");
      for (String pair : pairs) {
        String[] keyValuePair = pair.split("=");
        String key = keyValuePair[0];
        String value = keyValuePair.length == 2 ? keyValuePair[1] : "";
        params.put(decode(key), decode(value));
      }
      return new QueryData(params);
    }

    private static String decode(String str) {
      try {
        return URLDecoder.decode(str, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
