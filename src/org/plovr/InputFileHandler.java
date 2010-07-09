package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * {@link InputFileHandler} serves the content of input files to a compilation.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
final class InputFileHandler implements HttpHandler {

  private static final SoyTofu TOFU;

  static {
    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(InputFileHandler.class, "raw.soy"));
    SoyFileSet fileSet = builder.build();
    TOFU = fileSet.compileToJavaObj();
  }

  private final CompilationServer server;

  public InputFileHandler(CompilationServer server) {
    this.server = server;
  }

  static String getJsToLoadManifest(String configId, Manifest manifest,
      String prefix, String path) throws MissingProvideException {
    JsonArray inputs = new JsonArray();
    for (JsInput input : manifest.getInputsInCompilationOrder()) {
      inputs.add(new JsonPrimitive(prefix + "input?config_id=" + configId +
          "&name=" + input.getName()));
    }

    SoyMapData mapData = new SoyMapData(
        "filesAsJsonArray", inputs.toString(),
        "path", path);

    final SoyMsgBundle messageBundle = null;
    return TOFU.render("org.plovr.raw", mapData, messageBundle);
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    if (requestMethod.equalsIgnoreCase("GET")) {
      doGet(exchange);
    }
  }

  private void doGet(HttpExchange exchange) throws IOException {
    QueryData data = QueryData.createFromUri(exchange.getRequestURI());
    String id = data.getParam("config_id");
    if (id == null) {
      HttpUtil.writeNullResponse(exchange);
      return;
    }

    Config config = server.getConfigById(id);
    if (config == null) {
      HttpUtil.writeNullResponse(exchange);
      return;
    }

    Manifest manifest = config.getManifest();
    String name = data.getParam("name");
    JsInput requestedInput = manifest.getJsInputByName(name);

    if (requestedInput == null) {
      HttpUtil.writeNullResponse(exchange);
      return;
    }

    String code = requestedInput.getCode();
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/javascript");
    exchange.sendResponseHeaders(200, code.length());

    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(code);
    responseBody.close();
  }
}
