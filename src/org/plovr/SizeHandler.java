package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.io.LineReader;
import com.google.common.io.Resources;
import com.google.javascript.jscomp.Result;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

final class SizeHandler extends AbstractGetHandler {

  private final Pattern inputDelimiterPattern = Pattern.compile(
      "// Input (\\d+): (\\S+).*");

  private final SoyTofu sizeTemplate;

  public SizeHandler(CompilationServer server) {
    super(server);

    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(SizeHandler.class, "size.soy"));
    SoyFileSet fileSet = builder.build();
    sizeTemplate = fileSet.compileToTofu();
  }

  @Override
  protected void doGet(
      HttpExchange exchange,
      QueryData data,
      Config config)
      throws IOException {
    Config.Builder builder = Config.builder(config);
    builder.setPrintInputDelimiter(true);
    config = builder.build();

    // Do not associate the Compilation with the Config because it is modified
    // to enable the printInputDelimiter option.
    final boolean recordCompilation = false;
    Compilation compilation = getCompilation(
        exchange, data, config, recordCompilation);
    if (compilation == null) {
      return;
    }

    Result result = compilation.getResult();
    if (result != null && result.success) {
      processCompiledCode(compilation.getCompiledCode(), config, exchange);
    } else {
      HttpUtil.writeNullResponse(exchange);
    }
  }

  private void processCompiledCode(String compiledJs, Config config,
      HttpExchange exchange) throws IOException {
    SoyListData compilationData = new SoyListData();
    JsInput currentInput = null;
    String currentLine = null;
    int currentCompiledFileSize = 0;
    Counter totalOriginalFileSize = new Counter();
    Counter totalCompiledFileSize = new Counter();
    Manifest manifest = config.getManifest();

    LineReader lineReader = new LineReader(new StringReader(compiledJs));
    while ((currentLine = lineReader.readLine()) != null) {
      Matcher matcher = inputDelimiterPattern.matcher(currentLine);
      if (matcher.matches()) {
        if (currentInput != null) {
          addEntry(compilationData, currentInput, currentCompiledFileSize,
              totalOriginalFileSize, totalCompiledFileSize);
        }

        String inputName = matcher.group(2);
        currentInput = manifest.getJsInputByName(inputName);
        currentCompiledFileSize = 0;
      } else {
        // Add an extra byte for the line terminator (assume one character).
        currentCompiledFileSize += currentLine.length() + 1;
      }
    }
    addEntry(compilationData, currentInput, currentCompiledFileSize,
        totalOriginalFileSize, totalCompiledFileSize);

    SoyMapData mapData = new SoyMapData(
        "configId", config.getId(),
        "compilationData", compilationData,
        "originalSize", totalOriginalFileSize.getCount(),
        "compiledSize", totalCompiledFileSize.getCount()
        );
    String html = sizeTemplate.newRenderer("org.plovr.size").setData(mapData).render();

    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");
    exchange.sendResponseHeaders(200, html.length());
    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(html);
    responseBody.close();
  }

  private void addEntry(SoyListData compilationData, JsInput jsInput,
      int compiledFileSize, Counter totalOriginalFileSize,
      Counter totalCompiledFileSize) {
    int originalFileSize = jsInput.getCode().length();
    totalOriginalFileSize.add(originalFileSize);
    totalCompiledFileSize.add(compiledFileSize);
    SoyMapData data = new SoyMapData(
        "name", jsInput.getName(),
        "originalSize", originalFileSize,
        "compiledSize", compiledFileSize
        );
    compilationData.add(data);
  }

  private static class Counter {
    private int count = 0;
    public void add(int increment) { count += increment; }
    public int getCount() { return count; }
  }
}
