package org.plovr;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.javascript.jscomp.Result;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import org.plovr.io.Responses;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CompileRequestHandler extends AbstractGetHandler {

  private static final Logger logger = Logger.getLogger(
      CompileRequestHandler.class.getName());

  private static final SoyTofu TOFU;

  static {
    SoyFileSet.Builder builder = SoyFileSet.builder();
    builder.add(Resources.getResource(InputFileHandler.class, "raw.soy"));
    SoyFileSet fileSet = builder.build();
    TOFU = fileSet.compileToTofu();
  }

  private final ClientErrorReporter reporter;

  public CompileRequestHandler(CompilationServer server) {
    super(server);
    this.reporter = new ClientErrorReporter();
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config) throws IOException {
    // Update these fields as they are responsible for the response that will be
    // written.
    StringBuilder builder = new StringBuilder();

    try {
      Manifest manifest = config.getManifest();
      if (config.getCompilationMode() == CompilationMode.RAW) {
        String js = InputFileHandler.getJsToLoadManifest(
          server, config, manifest, exchange);
        builder.append(js);
      } else {
        if (needsRecompile(config)) {
          compile(config, exchange, builder);
        } else {
          File tmp = config.getCacheOutputFile();
          String cachedJs = Files.toString(tmp, config.getOutputCharset());
          builder.append(cachedJs);
          logger.info("JS Recompile skipped. Served "
                      + FileUtil.humanReadableByteCount(tmp.length(), true)
                      + " from cache <" + tmp + '>');
        }
      }
    } catch (CompilationException e) {
      Preconditions.checkState(builder.length() == 0,
          "Should not write errors to builder if output has already been written");
      String viewSourceUrl = getViewSourceUrlForExchange(exchange);
      reporter.newReport(config)
          .withErrors(e.createCompilationErrors())
          .withViewSourceUrl(viewSourceUrl)
          .appendTo(builder);
    }

    // Set header identifying source map unless in RAW mode.
    if (config.getCompilationMode() != CompilationMode.RAW) {
      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("X-SourceMap", "/sourcemap?id=" + config.getId());
    }

    Responses.writeJs(builder.toString(), config, exchange);
  }

  /**
   * For modes other than RAW, compile the code and write the result to builder.
   * When modules are used, only the code for the initial module will be written,
   * along with the requisite bootstrapping code for the remaining modules.
   */
  private void compile(Config config,
      HttpExchange exchange,
      Appendable appendable) throws IOException, CompilationException {
    Compilation compilation;
    String viewSourceUrl = getViewSourceUrlForExchange(exchange);
    try {
      compilation = Compilation.createAndCompile(config);
    } catch (CompilationException e) {
      reporter.newReport(config)
          .withErrors(e.createCompilationErrors())
          .withViewSourceUrl(viewSourceUrl)
          .appendTo(appendable);
      return;
    }

    server.recordCompilation(config, compilation);
    Result result = compilation.getResult();

    if (result.success) {
      if (config.getCompilationMode() == CompilationMode.WHITESPACE) {
        appendable.append("CLOSURE_NO_DEPS = true;\n");
      }

      if (compilation.usesModules()) {
        final boolean isDebugMode = true;
        Function<String, String> moduleNameToUri = ModuleHandler.
            createModuleNameToUriConverter(server, exchange, config.getId());
        ModuleConfig moduleConfig = config.getModuleConfig();
        if (moduleConfig.excludeModuleInfoFromRootModule()) {
          // If the module info is excluded from the root module, then the
          // module info should be written out now, followed by JS that will
          // dynamically load the root module.
          compilation.appendRootModuleInfo(appendable, isDebugMode,
              moduleNameToUri);

          String src = moduleNameToUri.apply(moduleConfig.getRootModule());
          SoyMapData mapData = new SoyMapData("src", src);
          String js = TOFU.newRenderer("org.plovr.loadRootModule").setData(
              mapData).render();
          appendable.append(js);
        } else {
          appendable.append(compilation.getCodeForRootModule(isDebugMode,
              moduleNameToUri));
        }
      } else {
        appendable.append(compilation.getCompiledCode());
      }

      // Here at the end of a successful compilation.  If the
      // cache-output-file has been defined, save compiled js out to it.
      File cacheOutputFile = config.getCacheOutputFile();
      if (cacheOutputFile != null) {
          cacheOutputFile.getParentFile().mkdirs();
          Files.write(appendable.toString(), cacheOutputFile, config.getOutputCharset());
      }

    }

    // TODO(bolinfest): Check whether writing out the plovr library confuses the
    // source map. Hopefully adding it after the compiled code will prevent it
    // from messing with the line numbers.

    // Write out the plovr library, even if there are no warnings.
    // It is small, and it exports some symbols that may be of use to
    // developers.
    reporter.newReport(config)
        .withErrors(compilation.getCompilationErrors())
        .withWarnings(compilation.getCompilationWarnings())
        .withViewSourceUrl(viewSourceUrl)
        .appendTo(appendable);
  }

  private String getViewSourceUrlForExchange(HttpExchange exchange) {
    return server.getServerForExchange(exchange) + "view";
  }

  /**
   * Return true if (1) the tmp file does not exist, (2) the config
   * file or any local file dependencies are newer than the tmp file.
   *
   * @author Paul Johnston (pcj@pubref.org)
   */
  protected boolean needsRecompile(Config config) throws CompilationException {
    File cacheOutputFile = config.getCacheOutputFile();

    if (cacheOutputFile == null) {
      return true;
    }

    if (!cacheOutputFile.exists()) {
      logger.info("JS Recompile required (cache-output-file not found): " + cacheOutputFile);
      return true;
    }

    long lastModified = cacheOutputFile.lastModified();

    if (config.isOutOfDate() || FileUtil.isNewer(config.getConfigFile(), lastModified)) {
      logger.info("JS Recompile required (config-file newer): " + config.getConfigFile());
      return true;
    }

    Manifest manifest = config.getManifest();
    List<JsInput> inputs = manifest.getInputsInCompilationOrder();
    for (JsInput input : inputs) {
      if (input instanceof LocalFileJsInput) {
        File source = ((LocalFileJsInput)input).getSource();
        if (FileUtil.isNewer(source, lastModified)) {
          logger.info("JS Recompile required (found newer file): " + source);
          return true;
        }
      } else if (input instanceof ResourceJsInput) {
        // assume these are up to date.
        continue;
      } else if (input.toString().startsWith("/closure/goog")) {
        // assume these are up to date.
        continue;
      } else {
        logger.info("JS Recompile required (found non-localfile-input): " + input + " of type " + input.getClass().getName());
        return true;
      }
    }

    return false;
  }

}
