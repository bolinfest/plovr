package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

final class IndexRequestHandler implements HttpHandler {

  private final CompilationServer server;

  private final SoyTofu indexTemplate;

  public IndexRequestHandler(CompilationServer server) {
    this.server = server;

    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(IndexRequestHandler.class, "index.soy"));
    SoyFileSet fileSet = builder.build();
    indexTemplate = fileSet.compileToTofu();
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    if ("GET".equalsIgnoreCase(requestMethod)) {
      doGet(exchange);
    }
  }

  private void doGet(HttpExchange exchange) throws IOException {
    Set<Config> configs = Sets.newTreeSet(server.getAllConfigs());

    SoyMapData mapData = new SoyMapData(
        "configs", Lists.transform(Lists.newArrayList(configs),
            configToSoyData)
        );
    String html = indexTemplate.newRenderer("org.plovr.index").setData(mapData).render();

    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");
    exchange.sendResponseHeaders(200, html.length());

    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(html);
    responseBody.close();
  }

  private static Function<Config, SoyMapData> configToSoyData =
      new Function<Config, SoyMapData>() {
    @Override
    public SoyMapData apply(Config config) {
      return new SoyMapData(
          "id", config.getId(),
          "hasModules", config.hasModules(),
          "rootModule", config.hasModules() ? config.getModuleConfig().getRootModule() : null
      );
    }
  };
}
