package org.plovr;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.plovr.io.Responses;

import com.google.common.base.Function;
import com.google.common.base.Functions;
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
public class InputFileHandler extends AbstractGetHandler {

  private static final SoyTofu TOFU;

  static {
    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(InputFileHandler.class, "raw.soy"));
    SoyFileSet fileSet = builder.build();
    TOFU = fileSet.compileToJavaObj();
  }

  public InputFileHandler(CompilationServer server) {
    super(server, true /* usesRestfulPath */);
  }

  /**
   * Returns the JavaScript code that bootstraps loading the application code.
   */
  static String getJsToLoadManifest(CompilationServer server,
      final Config config,
      Manifest manifest,
      HttpExchange exchange) throws CompilationException {
    // Function that converts a JsInput to the URI where its raw content can be
    // loaded.
   Function<JsInput,JsonPrimitive> inputToUri = Functions.compose(
       GsonUtil.STRING_TO_JSON_PRIMITIVE,
       createInputNameToUriConverter(server, exchange, config.getId()));

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
      Map<String, List<JsInput>> moduleToInputList = moduleConfig.
          partitionInputsIntoModules(manifest);
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
        "path", exchange.getRequestURI().getPath());

    final SoyMsgBundle messageBundle = null;
    return TOFU.render("org.plovr.raw", mapData, messageBundle);
  }

  /**
   * Pattern that matches the path to the REST URI for this handler.
   * The \\w+ will match the config id and the (/.*) will match the
   * input name.
   */
  private static final Pattern URI_INPUT_PATTERN = Pattern.compile(
      "/input/" + AbstractGetHandler.CONFIG_ID_PATTERN + "(/.*)");

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    Manifest manifest = config.getManifest();

    // Find the code for the requested input.
    String code = null;

    URI uri = exchange.getRequestURI();
    Matcher matcher = URI_INPUT_PATTERN.matcher(uri.getPath());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Input could not be extracted from URI");
    }
    String name = matcher.group(1);

    JsInput requestedInput = manifest.getJsInputByName(name);

    // TODO: eliminate this hack with the slash -- just make it an invariant of
    // the system.
    if (requestedInput == null) {
      // Remove the leading slash and try again.
      name = name.substring(1);
      requestedInput = manifest.getJsInputByName(name);
    }

    if (requestedInput != null) {
      code = requestedInput.getCode();
    }

    if (code == null) {
      HttpUtil.writeNullResponse(exchange);
      return;
    }

    Responses.writeJs(code, config, exchange);
  }

  @Override
  protected void setCacheHeaders(Headers headers) {
    // TODO: allow caching of JS files
    super.setCacheHeaders(headers);
  }

  static Function<JsInput,String> createInputNameToUriConverter(
      CompilationServer server, HttpExchange exchange, final String configId) {
    final String moduleUriBase = server.getServerForExchange(exchange);
    return new Function<JsInput, String>() {
      @Override
      public String apply(JsInput input) {
        // TODO(bolinfest): Should input.getName() be URI-escaped? Maybe all
        // characters other than slashes?

        // Hack: some input names do not have a leading slash, so add one when
        // that is not the case and special case this in doGet().
        String name = input.getName();
        if (!name.startsWith("/")) {
          name = "/" + name;
        }

        return String.format("%sinput/%s%s",
            moduleUriBase,
            QueryData.encode(configId),
            name);
      }
    };
  }
}
