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
import org.plovr.ModuleConfig;
import org.plovr.io.Streams;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;

public class BuildCommand extends AbstractCommandRunner<BuildCommandOptions> {

  @Override
  BuildCommandOptions createOptions() {
    return new BuildCommandOptions();
  }

  @Override
  int runCommandWithOptions(BuildCommandOptions options) throws IOException {
    // Even though logging would get printed to stderr and not stdout, it is
    // still distracting and feels wrong. May revisit this at some point.
    Logger.getLogger("org.plovr").setLevel(Level.OFF);

    List<String> arguments = options.getArguments();
    if (arguments.size() < 1) {
      printUsage();
      return 1;
    }

    // TODO(bolinfest): If mode == RAW, then simply concatenate the input files
    // in order.

    for (String configFile: arguments) {
      Config config = ConfigParser.parseFile(new File(configFile));
      Compilation compilation;
      try {
        compilation = CompileRequestHandler.compile(config);
      } catch (CompilationException e) {
        e.printStackTrace();
        compilation = null;
      }
      boolean isSuccess = processResult(compilation, config, options.getSourceMapPath(), config.getId());
      if (!isSuccess) {
        return 1;
      }
    }

    return 0;
  }

  private boolean processResult(Compilation compilation, Config config,
      String sourceMapPath, String sourceMapName) throws IOException {
    Preconditions.checkNotNull(compilation);
    Result result = compilation.getResult();
    boolean success = (result.success && result.errors.length == 0);
    if (success) {

      // Even if there were no errors, there may have been warnings, so print
      // them to standard error, but do not declare a build failure.
      for (JSError warning : result.warnings) {
        System.err.println(warning);
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
          Writer writer = Streams.createFileWriter(sourceMapPath, config);
          result.sourceMap.appendTo(writer, sourceMapName);
          Closeables.closeQuietly(writer);
        }
      } else {
        Function<String, String> moduleNameToUri = moduleConfig.
            createModuleNameToUriFunction();
        compilation.writeCompiledCodeToFiles(moduleNameToUri, sourceMapPath);
      }
    } else {
      for (JSError error : result.errors) {
        System.err.println(error);
      }
      for (JSError warning : result.warnings) {
        System.err.println(warning);
      }
    }

    printSummary(result, compilation);
    return success;
  }

  private void printSummary(Result result, Compilation compilation) {
    if (result.errors.length > 0) {
      System.err.print("BUILD FAILED: ");
    } else if (result.warnings.length > 0) {
      System.err.print("ATTENTION: ");
    }
    if (compilation.getTypedPercent() > 0.0) {
      System.err.printf("%d error(s), %d warning(s), %.2f%% typed\n", result.errors.length,
          result.warnings.length, compilation.getTypedPercent());
    } else {
      System.err.printf("%d error(s), %d warning(s)\n", result.errors.length,
          result.warnings.length);
    }
  }

  @Override
  String getUsageIntro() {
    return "Specify one or more configs to compile. " +
    		"If the \"output-file\" option is specified in the config, " +
    		"the file will be written there; otherwise, " +
    		"the result of the compilation will be printed to stdout.";
  }
}
