package org.plovr;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.plovr.io.Responses;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.css.JobDescription;
import com.google.common.css.JobDescription.OutputFormat;
import com.google.common.css.GssFunctionMapProvider;
import com.google.common.css.JobDescriptionBuilder;
import com.google.common.css.SourceCode;
import com.google.common.css.compiler.ast.BasicErrorManager;
import com.google.common.css.compiler.ast.CssTree;
import com.google.common.css.compiler.ast.GssError;
import com.google.common.css.compiler.ast.GssParser;
import com.google.common.css.compiler.ast.GssParserException;
import com.google.common.css.compiler.gssfunctions.DefaultGssFunctionMapProvider;
import com.google.common.css.compiler.passes.CompactPrinter;
import com.google.common.css.compiler.passes.PassRunner;
import com.google.common.css.compiler.passes.PrettyPrinter;
import com.google.common.io.Files;
import com.sun.net.httpserver.HttpExchange;

public class CssHandler extends AbstractGetHandler {

  public CssHandler(CompilationServer server) {
    super(server, true /* usesRestfulPath */);
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    JobDescriptionBuilder builder = new JobDescriptionBuilder();
    builder.setOutputFormat(JobDescription.OutputFormat.PRETTY_PRINTED);
    builder.setAllowedNonStandardFunctions(
        config.getAllowedNonStandardCssFunctions());
    // TODO: Read more of these options from a config.
//    builder.setAllowUnrecognizedFunctions(true);
//    builder.setAllowedUnrecognizedProperties(true);
//    builder.setAllowUnrecognizedProperties(true);
//    builder.setVendor(vendor);
    builder.setAllowWebkitKeyframes(true);
    builder.setProcessDependencies(true);
    builder.setSimplifyCss(true);
    builder.setEliminateDeadStyles(true);

    // Use the user-specified GssFunctionMapProvider if specified; otherwise,
    // fall back on the default.
    GssFunctionMapProvider functionMapProvider;
    String functionMapProviderClassName = config.
        getGssFunctionMapProviderClassName();
    if (functionMapProviderClassName == null) {
      functionMapProvider = new DefaultGssFunctionMapProvider();
    } else {
      functionMapProvider = getGssFunctionMapProviderForName(
          functionMapProviderClassName);
    }
    builder.setGssFunctionMapProvider(functionMapProvider);

    for (File input : config.getCssInputs()) {
      String fileContents;
      // TODO: Consider using the relative path, as specified in the config, as
      // the name.
      String fileName = input.getName();
      try {
        fileContents = Files.toString(input, Charsets.UTF_8);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      builder.addInput(new SourceCode(fileName, fileContents));
    }

    JobDescription job = builder.getJobDescription();
    ErrorManager errorManager = new ErrorManager();
    try {
      String output = execute(job, errorManager);
      StringBuilder css = new StringBuilder();

      // Prepend output with errors inside a CSS comment.
      // Hopefully such comments don't contain "*/"!
      if (errorManager.hasErrors()) {
        css.append("/*\n");
        for (GssError error : errorManager.getErrors()) {
          css.append(error.format() + "\n");
        }
        css.append("*/\n");
      }

      css.append(output);
      Responses.writeCss(css.toString(), exchange);
    } catch (GssParserException e) {
      String css = String.format("/* %s */", e.getMessage());
      Responses.writeCss(css, exchange);
    }
  }

  private String execute(JobDescription job, ErrorManager errorManager)
      throws GssParserException {
    GssParser parser = new GssParser(job.inputs);
    CssTree cssParseTree = parser.parse();

    PassRunner passRunner = new PassRunner(job, errorManager);
    passRunner.runPasses(cssParseTree);

    if (job.outputFormat == OutputFormat.COMPRESSED) {
      CompactPrinter compactPrinterPass = new CompactPrinter(cssParseTree);
      compactPrinterPass.runPass();
      return compactPrinterPass.getCompactPrintedString();
    } else {
      PrettyPrinter prettyPrinterPass = new PrettyPrinter(
          cssParseTree.getVisitController());
      prettyPrinterPass.runPass();
      return prettyPrinterPass.getPrettyPrintedString();
    }
  }

  /**
   * This method is taken from com.google.common.css.compiler.commandline.
   *     ClosureCommandLineCompiler.
   * @param gssFunctionMapProviderClassName such as
   *     "com.google.common.css.compiler.gssfunctions.DefaultGssFunctionMapProvider"
   * @return a new instance of the {@link GssFunctionMapProvider} that
   *     corresponds to the specified class name, or a new instance of
   *     {@link DefaultGssFunctionMapProvider} if the class name is
   *     {@code null}.
   */
  private static GssFunctionMapProvider getGssFunctionMapProviderForName(
      String gssFunctionMapProviderClassName) {
    // Verify that a class with the given name exists.
    Class<?> clazz;
    try {
      clazz = Class.forName(gssFunctionMapProviderClassName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(String.format(
          "Class does not exist: %s", gssFunctionMapProviderClassName), e);
    }

    // The class must implement GssFunctionMapProvider.
    if (!GssFunctionMapProvider.class.isAssignableFrom(clazz)) {
      throw new RuntimeException(String.format(
          "%s does not implement GssFunctionMapProvider",
          gssFunctionMapProviderClassName));
    }

    // Create the GssFunctionMapProvider using reflection.
    try {
      return (GssFunctionMapProvider) clazz.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static class ErrorManager extends BasicErrorManager {
    @Override
    public void print(String msg) {
      // printing to /dev/null!
    }

    public List<GssError> getErrors() {
      return ImmutableList.copyOf(errors);
    }
  }
}
