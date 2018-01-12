package org.plovr.cli;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.plovr.Compilation;
import org.plovr.CompilationException;
import org.plovr.CompileRequestHandler;
import org.plovr.Config;
import org.plovr.ConfigParser;
import org.plovr.CssHandler;
import org.plovr.CssHandler.ErrorManager;
import org.plovr.ModuleConfig;
import org.plovr.io.Streams;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.css.JobDescription;
import com.google.common.css.compiler.ast.GssError;
import com.google.common.css.compiler.ast.GssParserException;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;

public class BuildCommand extends AbstractCommandRunner<BuildCommandOptions> {

  @Override
  BuildCommandOptions createOptions() {
    return new BuildCommandOptions();
  }

  /**
   * TODO(bolinfest): This needs an integration test to ensure that it works in
   * all compilation modes and with and without modules.
   */
  @Override
  int runCommandWithOptions(BuildCommandOptions options) throws IOException {
    Logger.getLogger("org.plovr").setLevel(Level.WARNING);

    List<String> arguments = options.getArguments();
    if (arguments.size() < 1) {
      printUsage();
      return 1;
    }

    for (String configFile: arguments) {
      Config.Builder builder = ConfigParser.createBuilderFromFile(new File(configFile));
      if (options.getLanguage() != null) {
        builder.setLanguage(options.getLanguage());
      }
      builder.setPrintConfig(options.getPrintConfig());
      Config config = builder.build();
      Compilation compilation;
      try {
        compilation = Compilation.create(config);
        compilation.compile();
      } catch (CompilationException e) {
        e.print(System.err);
        return 1;
      }
      boolean isSuccess = processResult(compilation, config, options.getSourceMapPath());
      if (!isSuccess) {
        return 1;
      }

      isSuccess = processCssIfPresent(config);
      if (!isSuccess) {
        return 1;
      }
    }

    return 0;
  }

  private boolean processResult(Compilation compilation, Config config,
      String sourceMapPath) throws IOException {
    Preconditions.checkNotNull(compilation);
    Result result = compilation.getResult();
    boolean success = (result.success && result.errors.length == 0);
    if (success) {
      // write mapping files if requested
      if (config.getVariableMapOutputFile() != null) {
        File variableMapOutputFile = config.getVariableMapOutputFile();
        variableMapOutputFile.getParentFile().mkdirs();
        Files.write(result.variableMap.toBytes(), variableMapOutputFile);
      }

      if (config.getPropertyMapOutputFile() != null) {
        File propertyMapOutputFile = config.getPropertyMapOutputFile();
        propertyMapOutputFile.getParentFile().mkdirs();
        Files.write(result.propertyMap.toBytes(), propertyMapOutputFile);
      }

      ModuleConfig moduleConfig = config.getModuleConfig();
      if (moduleConfig == null) {
        String compiledJs = compilation.getCompiledCode();
        if (config.getOutputFile() != null) {
          File outputFile = config.getOutputFile();
          outputFile.getParentFile().mkdirs();
          Files.write(compiledJs, outputFile, config.getOutputCharset());
        } else {
          System.out.println(compiledJs);
        }

        // It turns out that the SourceMap will not be populated until after the
        // Compiler's internal representation has been output as source code, so
        // it should only be written out to a file after the compiled code has
        // been generated.
        if (sourceMapPath != null) {
          new File(sourceMapPath).mkdirs();
          String sourceMapName = config.getSourceMapOutputName();
          Writer writer = Streams.createFileWriter(
              new File(sourceMapPath, sourceMapName), config);
          result.sourceMap.appendTo(writer, sourceMapName);
          Closeables.close(writer, false);
        }
      } else {
        Function<String, String> moduleNameToUri = moduleConfig.
            createModuleNameToUriFunction();
        compilation.writeCompiledCodeToFiles(moduleNameToUri, sourceMapPath);
      }
    }

    return success;
  }

  private boolean processCssIfPresent(Config config) throws IOException {
    List<File> cssInputs = config.getCssInputs();
    File cssOutputFile = config.getCssOutputFile();
    if (cssInputs.isEmpty() || cssOutputFile == null) {
      return true;
    }

    JobDescription job = CssHandler.createJobFromConfig(config);
    ErrorManager errorManager = new ErrorManager();
    String compiledCss;
    try {
      compiledCss = CssHandler.execute(job, errorManager);
    } catch (GssParserException e) {
      e.printStackTrace();
      return false;
    }

    if (errorManager.hasErrors()) {
      for (GssError error : errorManager.getErrors()) {
        System.err.println(error.format() + "\n");
      }
      return false;
    } else {
      Files.write(compiledCss, cssOutputFile, Charsets.UTF_8);
    }
    return true;
  }

  @Override
  String getUsageIntro() {
    return "Specify one or more configs to compile. " +
        "If the \"output-file\" option is specified in the config, " +
        "the file will be written there; otherwise, " +
        "the result of the compilation will be printed to stdout.";
  }
}
