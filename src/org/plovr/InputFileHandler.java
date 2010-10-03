package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

/**
 * {@link InputFileHandler} serves the content of input files to a compilation.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
final class InputFileHandler extends AbstractGetHandler {

  private static final SoyTofu TOFU;

  static {
    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(InputFileHandler.class, "raw.soy"));
    SoyFileSet fileSet = builder.build();
    TOFU = fileSet.compileToJavaObj();
  }

  public InputFileHandler(CompilationServer server) {
    super(server);
  }

  /**
   * Returns the JavaScript code that bootstraps loading the application code.
   */
  static String getJsToLoadManifest(final Config config, Manifest manifest,
      final String prefix, String path) throws MissingProvideException {
    // Function that converts a JsInput to the URI where its raw content can be
    // loaded.
    Function<JsInput, JsonPrimitive> inputToUri =
        new Function<JsInput, JsonPrimitive>() {
      @Override
      public JsonPrimitive apply(JsInput input) {
        return new JsonPrimitive(prefix + "input" +
            "?id=" + QueryData.encode(config.getId()) +
            "&name=" + QueryData.encode(input.getName()));
      }
    };

    String moduleInfo;
    String moduleUris;
    ModuleConfig moduleConfig = config.getModuleConfig();
    List<JsInput> inputs;
    if (moduleConfig == null) {
      moduleInfo = null;
      moduleUris = null;
      inputs = manifest.getInputsInCompilationOrder();
    } else {
      moduleInfo = Compilation.createModuleInfo(moduleConfig).toString();

      // Get the list of JsInputs for each module and use that to construct
      // the JsonObject that will be used for the PLOVR_MODULE_URIS variable.
      List<JsInput> unpartitionedInputs = manifest.getInputsInCompilationOrder();
      Map<String, List<JsInput>> moduleToInputList = moduleConfig.
          partitionInputsIntoModules(unpartitionedInputs);
      JsonObject obj = new JsonObject();
      for (Map.Entry<String, List<JsInput>> entry : moduleToInputList.entrySet()) {
        String moduleName = entry.getKey();
        JsonArray uris = new JsonArray();
        for (JsInput input : entry.getValue()) {
          uris.add(inputToUri.apply(input));
        }
        obj.add(moduleName, uris);
      }
      moduleUris = obj.toString();

      // Have the initial JS load the list of files that correspond to the root
      // module. The root module is responsible for loading the other modules.
      inputs = moduleToInputList.get(moduleConfig.getRootModule());
    }

    JsonArray inputUrls = new JsonArray();
    for (JsInput input : inputs) {
      inputUrls.add(inputToUri.apply(input));
    }

    // TODO(bolinfest): Figure out how to reuse Compilation#appendRootModuleInfo
    // May require moving the method to ModuleConfig.
    SoyMapData mapData = new SoyMapData(
        "moduleInfo", moduleInfo,
        "moduleUris", moduleUris,
        "filesAsJsonArray", inputUrls.toString(),
        "path", path);

    final SoyMsgBundle messageBundle = null;
    return TOFU.render("org.plovr.raw", mapData, messageBundle);
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    Manifest manifest = config.getManifest();

    // Find the code for the requested input.
    String code = null;

    // Exactly one of module or name should be specified in the QueryData.
    String moduleName = data.getParam("module");
    String name = data.getParam("name");

    if (moduleName != null) {
      Compilation compilation = CompilationUtil.getLastCompilationOrFail(
          server, config, exchange);
      if (compilation == null) {
        return;
      }

      final boolean isDebugMode = false;
      Function<String, String> moduleNameToUri = createModuleNameToUriConverter(
          server, exchange, config.getId());
      code = compilation.getCodeForModule(moduleName, isDebugMode, moduleNameToUri);
    } else if (name != null) {
      JsInput requestedInput = manifest.getJsInputByName(name);
      if (requestedInput != null) {
        code = requestedInput.getCode();
      }
    }

    if (code == null) {
      HttpUtil.writeNullResponse(exchange);
      return;
    }

    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/javascript");
    exchange.sendResponseHeaders(200, code.length());

    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(code);
    responseBody.close();
  }

  static Function<String,String> createModuleNameToUriConverter(
      CompilationServer server, HttpExchange exchange, final String configId) {
    final String moduleUriBase = server.getServerForExchange(exchange);
    return new Function<String, String>() {
      @Override
      public String apply(String moduleName) {
        return moduleUriBase + "input" +
          "?id=" + QueryData.encode(configId) +
          "&module=" + QueryData.encode(moduleName);
      }
    };
  }
}
