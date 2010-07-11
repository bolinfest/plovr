package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.Result;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

final class SizeHandler extends AbstractGetHandler {

  private static final Logger logger = Logger.getLogger(
      AbstractGetHandler.class.getName());

  SizeHandler(CompilationServer server) {
    super(server);
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    Config.Builder builder = Config.builder(config);
    builder.setPrintInputDelimiter(true);
    config = builder.build();

    CompilationMode mode = config.getCompilationMode();
    if (mode == CompilationMode.RAW) {
      HttpUtil.writeErrorMessageResponse(exchange,
          "Not applicable for RAW mode");
      return;
    }

    Compiler compiler = new Compiler();
    Result result;
    try {
      result = CompileRequestHandler.compile(compiler, config);
    } catch (MissingProvideException e) {
      logger.log(Level.SEVERE, "Error during compilation", e);
      result = null;
    }

    if (result != null && result.success) {
      processCompiledCode(compiler.toSource(), config.getManifest(), exchange);
    } else {
      HttpUtil.writeNullResponse(exchange);
    }
  }

  private void processCompiledCode(String compiledJs, Manifest manifest,
      HttpExchange exchange) throws IOException {
    // TODO(bolinfest): Parse input delimiter and display original file size
    // compared to compiled file size.
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    exchange.sendResponseHeaders(200, compiledJs.length());
    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(compiledJs);
    responseBody.close();

  }
}
