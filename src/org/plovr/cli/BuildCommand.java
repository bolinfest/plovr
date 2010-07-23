package org.plovr.cli;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.plovr.CheckedSoySyntaxException;
import org.plovr.CompileRequestHandler;
import org.plovr.Compilation;
import org.plovr.Config;
import org.plovr.ConfigParser;
import org.plovr.MissingProvideException;
import org.plovr.ModuleConfig;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;

public class BuildCommand extends AbstractCommandRunner<BuildCommandOptions> {

  @Override
  BuildCommandOptions createOptions() {
    return new BuildCommandOptions();
  }

  @Override
  void runCommandWithOptions(BuildCommandOptions options) throws IOException {
    // Even though logging would get printed to stderr and not stdout, it is
    // still distracting and feels wrong. May revisit this at some point.
    Logger.getLogger("org.plovr").setLevel(Level.OFF);

    List<String> arguments = options.getArguments();
    if (arguments.size() != 1) {
      printUsage();
      return;
    }

    String configFile = arguments.get(0);
    Config config = ConfigParser.parseFile(new File(configFile));
    Compilation compilation;
    try {
      compilation = CompileRequestHandler.compile(config);
    } catch (MissingProvideException e) {
      e.printStackTrace();
      compilation = null;
    } catch (CheckedSoySyntaxException e) {
      e.printStackTrace();
      compilation = null;
    }

    processResult(compilation, config, options.getSourceMapPath(), config.getId());
  }

  private void processResult(Compilation compilation, Config config,
      String sourceMapPath, String sourceMapName) throws IOException {
    Preconditions.checkNotNull(compilation);
    Result result = compilation.getResult();
    if (result.success && result.errors.length == 0 && result.warnings.length == 0) {
      ModuleConfig moduleConfig = config.getModuleConfig();
      if (moduleConfig == null) {
        System.out.println(compilation.getCompiledCode());
      } else {
        compilation.writeCompiledCodeToFiles();
      }

      // It turns out that the SourceMap will not be populated until after the
      // Compiler's internal representation has been output as source code, so
      // it should only be written out to a file after the compiled code has
      // been generated.
      if (sourceMapPath != null) {
        result.sourceMap.appendTo(new FileWriter(sourceMapPath), sourceMapName);
      }
    } else {
      for (JSError error : result.errors) {
        System.err.println(error);
      }
      for (JSError warning : result.warnings) {
        System.err.println(warning);
      }
      System.err.printf("BUILD FAILED: %d Errors, %d Warnings\n",
          result.errors.length,
          result.warnings.length);
      System.exit(1);
    }
  }

  @Override
  String getUsageIntro() {
    return "Specify a single config and the result of compilation will be " +
        "printed to stdout.";
  }
}
