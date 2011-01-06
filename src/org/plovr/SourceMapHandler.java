package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import com.google.javascript.jscomp.SourceMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

final class SourceMapHandler extends AbstractGetHandler {

  public SourceMapHandler(CompilationServer server) {
    super(server);
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config) throws IOException {
    SourceMap sourceMap = server.getSourceMapFor(config);
    if (sourceMap == null) {
      HttpUtil.writeErrorMessageResponse(exchange,
          "No source map found -- perhaps you have not compiled yet?");
    } else {
      StringBuilder builder = new StringBuilder();

      // name is supposed to be the name of the generated source file that this
      // source map represents.
      String name = config.getId();
      // TODO(bolinfest): Make sure that this is the appropriate name.
      // Presumably the Closure Inspector relies on this so it can match the
      // source map to the appropriate <script> tag.

      sourceMap.appendTo(builder, name);

      Headers responseHeaders = exchange.getResponseHeaders();
      responseHeaders.set("Content-Type", "text/plain");
      exchange.sendResponseHeaders(200, builder.length());
      Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
      responseBody.write(builder.toString());
      responseBody.close();
    }
  }

}
