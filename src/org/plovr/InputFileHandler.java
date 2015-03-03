package org.plovr;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.plovr.JsInput.CodeWithEtag;
import org.plovr.io.Responses;
import org.plovr.util.HttpExchangeUtil;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link InputFileHandler} serves the content of input files to a compilation.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class InputFileHandler extends AbstractGetHandler {

  private static final SoyTofu TOFU;

  static {
    SoyFileSet.Builder builder = SoyFileSet.builder();
    builder.add(Resources.getResource(InputFileHandler.class, "raw.soy"));
    SoyFileSet fileSet = builder.build();
    TOFU = fileSet.compileToTofu();
  }

  private final ClientErrorReporter reporter;

  public InputFileHandler(CompilationServer server) {
    super(server, true /* usesRestfulPath */);
    this.reporter = new ClientErrorReporter();
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

    return TOFU.newRenderer("org.plovr.raw").setData(mapData).render();
  }

  /**
   * Pattern that matches the path to the REST URI for this handler.
   * The \\w+ will match the config id and the (/.*) will match the
   * input name.
   */
  private static final Pattern URI_INPUT_PATTERN = Pattern.compile(
      "/input/" + AbstractGetHandler.CONFIG_ID_PATTERN + "/(.*)");

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    Manifest manifest = config.getManifest();

    URI uri = exchange.getRequestURI();
    Matcher matcher = URI_INPUT_PATTERN.matcher(uri.getPath());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Input could not be extracted from URI");
    }
    String name = matcher.group(1);

    // If the user requests the deps.js alongside base.js, then return the
    // generated dependency info for this config rather than the default deps.js
    // that comes with the Closure Library.
    String depsJsName = Manifest.DEPS_JS_INPUT_NAME;
    if (name.equals(depsJsName)) {
      super.setCacheHeaders(exchange.getResponseHeaders());
      StringBuilder builder = new StringBuilder();
      try {
        builder.append(getCodeForDepsJs(manifest));
      } catch (UncheckedCompilationException e) {
        reporter.newReport(config)
            .withErrors(ImmutableList.of(e.createCompilationError()))
            .appendTo(builder);
      }
      Responses.writeJs(builder.toString(), config, exchange);
      return;
    }

    // Find the JsInput that matches the specified name.
    JsInput requestedInput = manifest.getJsInputByName(name);

    // Find the code for the requested input.
    String code = null;;
    try {
      if (requestedInput != null && requestedInput.supportsEtags()) {
        // Set/check an ETag, if appropriate.
        CodeWithEtag codeWithEtag = requestedInput.getCodeWithEtag();
        String eTag = codeWithEtag.eTag;
        String ifNoneMatch = exchange.getRequestHeaders().getFirst(
            "If-None-Match");

        // Don't send etags on old version of Chrome, because of a weird Java
        // HttpServer bug that it reacted badly to. See:
        // https://code.google.com/p/chromium/issues/detail?id=105824
        if (eTag.equals(ifNoneMatch) &&
            !HttpExchangeUtil.isGoogleChrome35OrEarlier(exchange)) {
          Responses.notModified(exchange);
          return;
        } else {
          Headers headers = exchange.getResponseHeaders();
          headers.set("ETag", eTag);
          code = codeWithEtag.code;
        }
      } else {
        // Do not set cache headers if the logic for ETags has not been defined
        // for this JsInput. Setting an "Expires" header based on the last
        // modified time for a file has been observed to cause resources to be
        // cached incorrectly by IE6.
        super.setCacheHeaders(exchange.getResponseHeaders());
        code = requestedInput.getCode();
      }
    } catch (UncheckedCompilationException e) {
      StringBuilder builder = new StringBuilder();
      reporter.newReport(config)
          .withErrors(ImmutableList.of(e.createCompilationError()))
          .appendTo(builder);
      code = builder.toString();
    }

    if (code == null) {
      HttpUtil.writeNullResponse(exchange);
      return;
    }

    Responses.writeJs(code, config, exchange);
  }

  private String getCodeForDepsJs(Manifest manifest) {
    String baseJsPath = Manifest.BASE_JS_INPUT_NAME;

    // Although baseJsPath is a constant, so this logic will always produce the
    // same result, we keep it here in case Manifest.BASE_JS_INPUT_NAME is
    // redefined in the future.
    int numDirectories = baseJsPath.split("\\/").length - 1;
    final String relativePath = Strings.repeat("../", numDirectories);

    Function<JsInput, String> converter = new Function<JsInput, String>() {
      @Override
      public String apply(JsInput input) {
        return relativePath + input.getName();
      }
    };

    return manifest.buildDepsJs(converter);
  }

  /**
   * By default, do nothing. {@link AbstractGetHandler#setCacheHeaders(Headers)}
   * will be called as appropriate from
   * {@link #doGet(HttpExchange, QueryData, Config)}.
   */
  @Override
  protected void setCacheHeaders(Headers headers) {}

  static Function<JsInput, String> createInputNameToUriConverter(
      CompilationServer server, HttpExchange exchange, final String configId) {
    String moduleUriBase = server.getServerForExchange(exchange);
    return createInputNameToUriConverter(moduleUriBase, configId);
  }

  @VisibleForTesting
  static Function<JsInput, String> createInputNameToUriConverter(
      final String moduleUriBase, final String configId) {
    return new Function<JsInput, String>() {
      @Override
      public String apply(JsInput input) {
        // TODO(bolinfest): Should input.getName() be URI-escaped? Maybe all
        // characters other than slashes?

        return String.format("%sinput/%s/%s",
            moduleUriBase,
            QueryData.encode(configId),
            input.getName());
      }
    };
  }
}
