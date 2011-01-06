package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.plovr.util.HtmlUtil;

import com.google.common.base.Function;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public final class ListHandler extends AbstractGetHandler {

  public ListHandler(CompilationServer server) {
    super(server);
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    Compilation compilation = getCompilation(exchange, data, config);
    if (compilation == null) {
      return;
    }

    String configId = config.getId();
    List<JsInput> inputs;
    if (compilation.usesModules()) {
      ModuleConfig moduleConfig = config.getModuleConfig();
      Map<String, List<JsInput>> moduleToInputs;
      try {
        moduleToInputs = moduleConfig
            .partitionInputsIntoModules(config.getManifest());
      } catch (CompilationException e) {
        throw new RuntimeException(e);
      }

      String module = data.getParam("module");
      inputs = moduleToInputs.get(module);
    } else {
      try {
        inputs = config.getManifest().getInputsInCompilationOrder();
      } catch (CompilationException e) {
        HttpUtil.writeErrorMessageResponse(exchange, e.getMessage());
        return;
      }
    }

    // Build up the list of hyperlinks.
    StringBuilder builder = new StringBuilder();
    Function<JsInput, String> converter = InputFileHandler
        .createInputNameToUriConverter(server, exchange, configId);
    for (JsInput input : inputs) {
      builder.append(String.format("<a href='%s'>%s</a><br>",
          HtmlUtil.htmlEscape(converter.apply(input)),
          HtmlUtil.htmlEscape(input.getName())));
    }

    // Write the response.
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");
    String html = builder.toString();
    exchange.sendResponseHeaders(200, html.length());
    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(html);
    responseBody.close();
  }

}
