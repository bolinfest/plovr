package org.plovr;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

/**
 * {@link ViewFileHandler} is used to display the source of a JavaScript file
 * with a particular line number highlighted. Its content-type is HTML rather
 * than plaintext, so InputFileHandler should be used to get the raw input file.
 * @author bolinfest@gmail.com (Michael Bolin)
 */
final class ViewFileHandler extends AbstractGetHandler {

  ViewFileHandler(CompilationServer server) {
    super(server);
  }

  @Override
  protected void doGet(HttpExchange exchange) throws IOException {
    // Extract the parameters from the query data.
    QueryData data = QueryData.createFromUri(exchange.getRequestURI());
    String id = data.getParam("id");
    String name = data.getParam("line");
    int lineNumer = Integer.parseInt(data.getParam("lineNumber"), 10);

    Config config = server.getConfigById(id);
    Manifest manifest = config.getManifest();
    JsInput input = manifest.getJsInputByName(name);

    // TODO(bolinfest): Write out each line in the input, giving each line an
    // id so the fragment can be used to navigate to it.
  }
}
