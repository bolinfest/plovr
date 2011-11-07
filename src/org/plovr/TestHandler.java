package org.plovr;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.plovr.io.Responses;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.HttpExchange;

public class TestHandler extends AbstractGetHandler {

  /**
   * Pattern that matches the path to the REST URI for this handler.
   * The \\w+ will match the config id and the (.*) will match the
   * input name.
   */
  private static final Pattern URI_TEST_PATTERN = Pattern.compile(
      "/test/" + AbstractGetHandler.CONFIG_ID_PATTERN + "/(.*)");

  private static final SoyTofu TOFU;

  static {
    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(TestHandler.class, "test.soy"));
    SoyFileSet fileSet = builder.build();
    TOFU = fileSet.compileToJavaObj();
  }

  public TestHandler(CompilationServer server) {
    super(server, true /* usesRestfulPath */);
  }

  /**
   * @return the relative URL to load base.js for the specified {@link Config}
   */
  private static String getBaseJsUrl(Config config) {
    return "/input/" + config.getId() +
        config.getManifest().getBaseJs().getName();
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    URI uri = exchange.getRequestURI();
    Matcher matcher = URI_TEST_PATTERN.matcher(uri.getPath());
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "Test file path could not be extracted from the URL");
    }
    String name = matcher.group(1);

    if ("all".equals(name)) {
      writeTestPage("org.plovr.all", config, exchange);
      return;
    } else if ("list".equals(name)) {
      writeTestPage("org.plovr.listTests", config, exchange);
      return;
    } else if (name.endsWith("_test.js")) {
      // Serve the test code, if it exists.
      File testJsFile = config.getTestFile(name);
      if (testJsFile != null) {
        String js = Files.toString(testJsFile, Charsets.UTF_8);
        Responses.writeJs(js, config, exchange);
        return;
      }
    } else if (name.endsWith("_test.html")) {
      // Serve the test host page, if it exists.
      File testHtmlFile = config.getTestFile(name);
      if (testHtmlFile != null) {
        String html = Files.toString(testHtmlFile, Charsets.UTF_8);
        Responses.writeHtml(html, exchange);
        return;
      }

      // If there is no HTML file, then create one.
      if (writeSyntheticTestFile(name, config, exchange)) {
        return;
      }
    }

    // If this point is reached, then no file was found.
    HttpUtil.writeNotFound(exchange);
  }

  /**
   * @param name the relative path to the test file: "foo/bar/baz_test.html"
   * @return true if the HTML test host page was written; otherwise, return
   *     false
   */
  private boolean writeSyntheticTestFile(String name, Config config,
      HttpExchange exchange) throws IOException {
    // Unlike replace(), replaceFirst() takes a regex.
    String jsFileName = name.replaceFirst("_test\\.html$", "_test.js");
    if (config.getTestFile(jsFileName) == null) {
      return false;
    }

    // Instead of loading base.js and the uncompiled test file, consider
    // creating a plovr config for each test file so that it is easier to run
    // the test in either raw or compiled mode.
    String baseJsUrl = getBaseJsUrl(config);
    String testJsUrl = "/test/" + config.getId() + "/" + jsFileName;
    SoyMapData mapData = new SoyMapData(
        "title", jsFileName,
        "baseJsUrl", baseJsUrl,
        "testJsUrl", testJsUrl);

    SoyTofu testTemplateTofu = getTestTemplateTofu(config);
    String html = testTemplateTofu.newRenderer("org.plovr.test").setData(
        mapData).render();
    Responses.writeHtml(html, exchange);
    return true;
  }

  private static SoyTofu getTestTemplateTofu(Config config) {
    if (config.getTestTemplate() != null) {
      SoyFileSet.Builder builder = new SoyFileSet.Builder();
      builder.add(config.getTestTemplate());
      SoyFileSet fileSet = builder.build();
      return fileSet.compileToJavaObj();
    } else {
      return TOFU;
    }
  }

  private static void writeTestPage(String templateName, Config config,
      HttpExchange exchange) throws IOException {
    Set<String> testFilePaths = getRelativeTestFilePaths(config);

    SoyListData tests = new SoyListData(testFilePaths);
    SoyMapData soyData = new SoyMapData(
        "configId", config.getId(),
        "baseJsUrl", getBaseJsUrl(config),
        "tests", tests);
    String html = TOFU.newRenderer(templateName).setData(
        soyData).render();
    Responses.writeHtml(html, exchange);
  }

  /**
   * @return all paths to _test.html files, none of which start with a leading
   *     slash
   */
  private static Set<String> getRelativeTestFilePaths(Config config) {
    Manifest manifest = config.getManifest();
    TreeSet<String> testFilePaths = Sets.newTreeSet();

    for (File dependency : manifest.getDependencies()) {
      if (!dependency.isDirectory()) {
        continue;
      }
      addAllTestFiles(dependency, dependency, testFilePaths);
    }

    return testFilePaths;
  }

  /**
   * Each value in to testFilePaths must not have a leading slash.
   */
  private static void addAllTestFiles(File base, File directory,
      Set<String> testFilePaths) {
    for (File entry : directory.listFiles()) {
      if (entry.isDirectory()) {
        addAllTestFiles(base, entry, testFilePaths);
      } else if (entry.isFile()) {
        if (entry.getName().endsWith("_test.js")) {
          String basePath = base.getAbsolutePath();
          String testPath = entry.getAbsolutePath();
          String relativePath = testPath.substring(basePath.length());
          if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
          }
          // Unlike replace(), replaceFirst() takes a regex.
          relativePath = relativePath.replaceFirst("_test\\.js$", "_test.html");
          testFilePaths.add(relativePath);
        }
      }
    }
  }
}
