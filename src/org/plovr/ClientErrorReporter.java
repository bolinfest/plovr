package org.plovr;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.javascript.jscomp.Result;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.tofu.SoyTofu;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import org.plovr.io.Responses;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes Javascript that reports errors to the client.
 * @author nicholas.j.santos@gmail.com (Nick Santos)
 */
class ClientErrorReporter {
  private static final Logger logger = Logger.getLogger(
      ClientErrorReporter.class.getName());

  private final Gson gson;
  private final String plovrJsLib;

  ClientErrorReporter() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(CompilationError.class,
        new CompilationErrorSerializer());
    gson = gsonBuilder.create();

    URL plovrJsLibUrl = Resources.getResource("org/plovr/plovr.js");
    String plovrJsLib;
    try {
      plovrJsLib = Resources.toString(plovrJsLibUrl, Charsets.US_ASCII);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error loading errors.js", e);
      plovrJsLib = null;
    }
    this.plovrJsLib = plovrJsLib;
  }

  Report newReport(Config config) {
    return new Report(config);
  }

  class Report {
    private final Config config;

    private List<CompilationError> errors = ImmutableList.<CompilationError>of();
    private List<CompilationError> warnings = ImmutableList.<CompilationError>of();
    private String viewSourceUrl;

    Report(Config config) {
      this.config = config;
    }

    Report withErrors(List<CompilationError> errors) {
      this.errors = errors;
      return this;
    }

    Report withWarnings(List<CompilationError> warnings) {
      this.warnings = warnings;
      return this;
    }

    Report withViewSourceUrl(String viewSourceUrl) {
      this.viewSourceUrl = viewSourceUrl;
      return this;
    }

    void appendTo(Appendable builder) throws IOException {
      Preconditions.checkNotNull(errors);
      Preconditions.checkNotNull(builder);

      String configIdJsString = gson.toJson(config.getId());
      builder.append(plovrJsLib)
          .append("plovr.addErrors(").append(gson.toJson(errors)).append(");\n")
          .append("plovr.addWarnings(").append(gson.toJson(warnings)).append(");\n");
      if (viewSourceUrl != null) {
        builder.append("plovr.setViewSourceUrl(\"").append(viewSourceUrl).append("\");\n");
      }
      builder
          .append("plovr.setConfigId(").append(configIdJsString).append(");\n")
          .append("plovr.writeErrorsOnLoad();\n");
    }
  }
}
