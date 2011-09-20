package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.io.Resources;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public final class ListHandler extends AbstractGetHandler {

  private static final SoyTofu TOFU;

  static {
    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(ListHandler.class, "list.soy"));
    SoyFileSet fileSet = builder.build();
    TOFU = fileSet.compileToJavaObj();
  }

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
    Function<JsInput, String> converter = InputFileHandler
        .createInputNameToUriConverter(server, exchange, configId);
    SoyListData inputData = new SoyListData();
    for (JsInput input : inputs) {
      SoyMapData inputDatum = new SoyMapData();
      inputDatum.put("href", converter.apply(input));
      inputDatum.put("name", input.getName());
      inputData.add(inputDatum);
    }
    SoyMapData soyData = new SoyMapData();
    soyData.put("inputs", inputData);
    soyData.put("configId", configId);

    // Write the response.
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");
    String html = TOFU.newRenderer("org.plovr.list").setData(soyData).render();
    exchange.sendResponseHeaders(200, html.length());
    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(html);
    responseBody.close();
  }

}
