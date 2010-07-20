package org.plovr.cli;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.plovr.CheckedSoySyntaxException;
import org.plovr.CompileRequestHandler;
import org.plovr.Config;
import org.plovr.ConfigParser;
import org.plovr.MissingProvideException;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.Compiler;
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
    // still distracting and feels wrong.
    Logger.getLogger("org.plovr").setLevel(Level.OFF);

    List<String> arguments = options.getArguments();
    if (arguments.size() != 1) {
      printUsage();
      return;
    }

    String configFile = arguments.get(0);
    Config config = ConfigParser.parseFile(new File(configFile));
    Compiler compiler = new Compiler();
    Result result;
    try {
      result = CompileRequestHandler.compile(compiler, config);
    } catch (MissingProvideException e) {
      e.printStackTrace();
      result = null;
    } catch (CheckedSoySyntaxException e) {
      e.printStackTrace();
      result = null;
    }

    processResult(compiler, result);
  }

  private void processResult(Compiler compiler, Result result) {
    Preconditions.checkNotNull(compiler);
    Preconditions.checkNotNull(result);
    if (result.success) {
      System.out.println(compiler.toSource());
    } else {
      for (JSError error : result.errors) {
        System.err.println(error);
      }
      for (JSError warning : result.warnings) {
        System.err.println(warning);
      }
      System.exit(1);
    }
  }

  @Override
  String getUsageIntro() {
    return "Specify a single config and the result of compilation will be " +
        "printed to stdout.";
  }
}
