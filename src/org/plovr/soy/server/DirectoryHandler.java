package org.plovr.soy.server;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.plovr.io.Responses;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * {@link DirectoryHandler} serves a page that lists the files in a directory.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
final class DirectoryHandler implements HttpHandler {

  private final Config config;

  private final SoyTofu soyWebTemplate;

  public DirectoryHandler(Config config) {
    this.config = config;

    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(DirectoryHandler.class, "soyweb.soy"));
    SoyFileSet fileSet = builder.build();
    soyWebTemplate = fileSet.compileToJavaObj();
  }

  /**
   * @param exchange
   */
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    File staticContentDirectory = RequestHandlerSelector
        .getCorrespondingFileForRequest(config, exchange);
    if (staticContentDirectory == null) {
      // A 403 response was already written.
      return;
    }

    Preconditions.checkArgument(staticContentDirectory.isDirectory(),
        "This handler only processes directory listings.");

    // If the URI does not end in "/" (and is not "/"), then redirect. This will
    // ensure that all of the hyperlinks in the page work correctly.
    final String path = exchange.getRequestURI().getPath();
    if (!path.endsWith("/") && !path.equals("/")) {
      Responses.redirect(exchange, path + "/");
      return;
    }

    // Split the files into directories and files.
    List<File> directories = Lists.newArrayList();
    List<File> files = Lists.newArrayList();
    for (File f : staticContentDirectory.listFiles()) {
      if (f.isDirectory()) {
        directories.add(f);
      } else {
        files.add(f);
        String name = f.getName();
        if (name.endsWith(".soy")) {
          name = name.replaceAll("\\.soy$", ".html");
          File htmlFile = new File(f.getParentFile(), name);
          if (!htmlFile.exists()) {
            // If the HTML already exists, then it will already be added by this
            // loop.
            files.add(htmlFile);
          }
        }
      }
    }

    // Sort each list alphabetically.
    Collections.sort(directories);
    Collections.sort(files);

    List<String> directoryNames = Lists.transform(directories, fileToName);
    List<String> fileNames = Lists.transform(files, fileToName);

    List<Map<String, String>> directoryParts = Lists.newArrayList();
    String href = "/";
    directoryParts.add(ImmutableMap.of("href", href, "name", "Root"));
    for (String name : path.split("/")) {
      if (!name.isEmpty()) {
        href += name + "/";
        directoryParts.add(ImmutableMap.of("href", href, "name", name));
      }
    }

    // Write out the HTML using a template.
    Map<String, ?> data = ImmutableMap.<String, Object>of(
        "directory", path,
        "directoryParts", directoryParts,
        "directories", directoryNames,
        "files", fileNames);
    String html = soyWebTemplate.newRenderer("org.plovr.soyweb.directory")
        .setData(data).render();
    Responses.writeHtml(html, exchange);
  }

  private static Function<File, String> fileToName = new Function<File, String>() {
    @Override
    public String apply(File file) {
      return file.getName();
    }
  };
}
