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
import com.google.template.soy.data.SanitizedContent;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.data.UnsafeSanitizedContentOrdainer;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import org.plovr.io.Responses;
import org.plovr.ClientErrorReporter.Report;

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
  private final CompilationCache compilationCache;

  public CompileRequestHandler(CompilationServer server) {
    super(server);
    this.reporter = new ClientErrorReporter();
    this.compilationCache = new CompilationCache();
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config) throws IOException {
    // Update these fields as they are responsible for the response that will be
    // written.
    StringBuilder builder = new StringBuilder();
    String viewSourceUrl = getViewSourceUrlForExchange(exchange);
    Report report = reporter.newReport(config)
        .withViewSourceUrl(viewSourceUrl);

    try {
      Manifest manifest = config.getManifest();
      if (config.getCompilationMode() == CompilationMode.RAW) {
        String js = InputFileHandler.getJsToLoadManifest(
          server, config, manifest, exchange);
        builder.append(js);
      } else {
        String cachedJs = compilationCache.getIfUpToDate(config);
        if (cachedJs != null) {
          builder.append(cachedJs);
        } else {
          long startTime = System.currentTimeMillis();
          Function<String, String> moduleNameToUri = ModuleHandler.
              createModuleNameToUriConverter(server, exchange, config.getId());
          compile(config, moduleNameToUri, report, builder);
          compilationCache.put(config, builder.toString(), startTime);
        }
      }
    } catch (CompilationException e) {
      Preconditions.checkState(builder.length() == 0,
          "Should not write errors to builder if output has already been written");
      report.withErrors(e.createCompilationErrors())
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
  private void compile(
      Config config, Function<String, String> moduleNameToUri, Report report, Appendable appendable)
      throws IOException, CompilationException {
    Compilation compilation = Compilation.create(config);
    compilation.compile();
    server.recordCompilation(config, compilation);
    Result result = compilation.getResult();

    if (result.success) {
      if (config.getCompilationMode() == CompilationMode.WHITESPACE) {
        appendable.append("CLOSURE_NO_DEPS = true;\n");
      }

      if (compilation.usesModules()) {
        final boolean isDebugMode = true;
        ModuleConfig moduleConfig = config.getModuleConfig();
        if (moduleConfig.excludeModuleInfoFromRootModule()) {
          // If the module info is excluded from the root module, then the
          // module info should be written out now, followed by JS that will
          // dynamically load the root module.
          compilation.appendRootModuleInfo(appendable, isDebugMode,
              moduleNameToUri);

          String src = moduleNameToUri.apply(moduleConfig.getRootModule());
          SanitizedContent.ContentKind scHtml = SanitizedContent.ContentKind.HTML;
          SoyMapData mapData = new SoyMapData("src",
                  src == null ? null : UnsafeSanitizedContentOrdainer
                                         .ordainAsSafe(src, scHtml));
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
    }

    // TODO(bolinfest): Check whether writing out the plovr library confuses the
    // source map. Hopefully adding it after the compiled code will prevent it
    // from messing with the line numbers.

    // Write out the plovr library, even if there are no warnings.
    // It is small, and it exports some symbols that may be of use to
    // developers.
    report.withErrors(compilation.getCompilationErrors())
        .withWarnings(compilation.getCompilationWarnings())
        .appendTo(appendable);
  }

  private String getViewSourceUrlForExchange(HttpExchange exchange) {
    return server.getServerForExchange(exchange) + "view";
  }
}
