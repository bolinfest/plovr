package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceExcerptProvider;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

class CompileRequestHandler extends AbstractGetHandler {

  private static final Logger logger = Logger.getLogger("org.plovr.CompileRequestHandler");

  private final Gson gson;

  private final String plovrJsLib;

  public CompileRequestHandler(CompilationServer server) {
    super(server);

    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(CompilationError.class,
        new CompilationErrorSerializer());
    gson = gsonBuilder.create();

    URL plovrJsLibUrl = Resources.getResource("org/plovr/plovr.js");
    String plovrJsLib;
    try {
      plovrJsLib = Resources.toString(plovrJsLibUrl, Charsets.US_ASCII);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error loading errors.js", e);
      plovrJsLib = null;
    }
    this.plovrJsLib = plovrJsLib;
  }

  @Override
  protected void doGet(HttpExchange exchange) throws IOException {
    QueryData data = QueryData.createFromUri(exchange.getRequestURI());
    String id = data.getParam("id");

    StringBuilder builder = new StringBuilder();
    String contentType;
    int responseCode;
    if (server.containsConfigWithId(id)) {
      Config config = new Config(server.getConfigById(id));

      // First, use query parameters from the script tag to update the config.
      ConfigParser.update(config, data);

      // If supported, modify query parameters based on the referrer. This is
      // more convenient for the developer, so it should be used to override.
      URI referrer = null;
      if (!config.isUseExplicitQueryParameters()) {
        referrer = HttpUtil.getReferrer(exchange);
        if (referrer != null) {
          QueryData referrerData = QueryData.createFromUri(referrer);
          ConfigParser.update(config, referrerData);
        }
      }

      try {
        if (config.getCompilationMode() == CompilationMode.RAW) {
          String prefix;
          URI requestUri = exchange.getRequestURI();
          if (referrer == null) {
            prefix = "http://localhost:9810/";
          } else {
            prefix =
                referrer.getScheme() + "://" + referrer.getHost() + ":9810/";
          }
          Manifest manifest = config.getManifest();
          String js = InputFileHandler.getJsToLoadManifest(config.getId(),
              manifest, prefix, requestUri.getPath());
          builder.append(js);
        } else {
          compile(config, data, builder);
        }
      } catch (MissingProvideException e) {
        Preconditions.checkState(builder.length() == 0,
            "Should not write errors to builder if output has already been written");
        writeErrors(config, ImmutableList.of(e.createCompilationError()),
            builder);
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
      throws IOException, MissingProvideException {
    CompilerArguments compilerArguments;
    compilerArguments = config.getManifest().getCompilerArguments();

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

    // Write out the plovr library, even if there are no warnings.
    // It is small, and it exports some symbols that may be of use to
    // developers.
    writeErrorsAndWarnings(config, normalizeErrors(result.errors, compiler),
        normalizeErrors(result.warnings, compiler), builder);
    logger.info("content: " + builder.toString());

    if (result.success) {
      if (config.getCompilationMode() == CompilationMode.WHITESPACE) {
        builder.append("CLOSURE_NO_DEPS = true;\n");
      }
      builder.append(compiler.toSource());
    }
  }

  private static List<CompilationError> normalizeErrors(JSError[] errors,
      SourceExcerptProvider sourceExcerptProvider) {
    List<CompilationError> compilationErrors = Lists.newLinkedList();
    for (JSError error : errors) {
      compilationErrors.add(new CompilationError(error, sourceExcerptProvider));
    }
    return compilationErrors;
  }

  private void writeErrors(Config config, List<CompilationError> errors,
      Appendable builder) throws IOException {
    writeErrorsAndWarnings(config, errors,
        ImmutableList.<CompilationError>of(),
        builder);
  }

  private void writeErrorsAndWarnings(Config config,
      List<CompilationError> errors, List<CompilationError> warnings,
      Appendable builder) throws IOException {
    Preconditions.checkNotNull(errors);
    Preconditions.checkNotNull(builder);

    String configIdJsString = gson.toJson(config.getId());
    builder.append(plovrJsLib)
        .append("plovr.addErrors(").append(gson.toJson(errors)).append(");\n")
        .append("plovr.addWarnings(").append(gson.toJson(warnings)).append(");\n")
        .append("plovr.setConfigId(").append(configIdJsString).append(");\n")
        .append("plovr.writeErrorsOnLoad();\n");
  }
}

