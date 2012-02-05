package org.plovr;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;

import com.google.common.io.Resources;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

/**
 * {@link ViewFileHandler} is used to display the source of a JavaScript file
 * with a particular line number highlighted. Its content-type is HTML rather
 * than plaintext, so InputFileHandler should be used to get the raw input file.
 * @author bolinfest@gmail.com (Michael Bolin)
 */
final class ViewFileHandler extends AbstractGetHandler {

  private final SoyTofu viewTemplate;

  public ViewFileHandler(CompilationServer server) {
    super(server);

    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(ViewFileHandler.class, "view.soy"));
    SoyFileSet fileSet = builder.build();
    viewTemplate = fileSet.compileToTofu();
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config) throws IOException {
    // Extract the parameters from the query data.
    String name = data.getParam("name");

    Manifest manifest = config.getManifest();

    // Write out each line in the input, giving each line an id so the fragment
    // can be used to navigate to it.
    JsInput input;
    String codeToDisplay;
    try {
      input = manifest.getJsInputByName(name);
      codeToDisplay = input.getCode();
    } catch (RuntimeException e) {
      SoyFile soyFile = findSoyFile(name, manifest, e);
      input = soyFile;
      codeToDisplay = soyFile.getTemplateCode();
    }
    String[] lines = codeToDisplay.split("\\n");
    SoyMapData mapData = new SoyMapData(
        "name", input.getName(),
        "lines", new SoyListData((Object[])lines)
        );
    String html = viewTemplate.newRenderer("org.plovr.view").setData(mapData).render();

    // TODO(bolinfest): Add syntax highlighting in the HTML.
    // TODO(bolinfest): Support ctrl+L to prompt for a line number to navigate to.

    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/html");
    exchange.sendResponseHeaders(200, html.length());

    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(html);
    responseBody.close();
  }

  /**
   * Find the Soy file by name in the manifest or throw the specified exception.
   */
  private SoyFile findSoyFile(String name, Manifest manifest, RuntimeException e) {
    Set<JsInput> allDeps = manifest.getAllDependencies(
        false /* cacheDependencies */);
    for (JsInput input : allDeps) {
      if (input.getName().equals(name) && input instanceof SoyFile) {
        return (SoyFile)input;
      }
    }
    throw e;
  }
}
