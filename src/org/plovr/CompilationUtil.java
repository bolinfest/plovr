package org.plovr;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

public final class CompilationUtil {

  /** Private class; do not instantiate. */
  private CompilationUtil() {}

  public static Compilation getLastCompilationOrFail(CompilationServer server,
      Config config, HttpExchange exchange) throws IOException {
    Compilation compilation = server.getLastCompilation(config);
    if (compilation == null) {
      String compileUrl = server.getServerForExchange(exchange) +
          "compile?id=" + QueryData.encode(config.getId());
      // TODO(bolinfest): HTML escape inputs (using Soy?)
      HttpUtil.writeHtmlErrorMessageResponse(exchange,
          "No compilation found for config: " + config.getId() + "<br>" +
          "Try visiting: <a href='" + compileUrl + "'>" + compileUrl + "</a>");
      return null;
    } else {
      return compilation;
    }
  }
}
