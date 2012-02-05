package org.plovr.cli;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.plovr.CompilationException;
import org.plovr.CompileRequestHandler;
import org.plovr.CompilerPassFactory;
import org.plovr.Config;
import org.plovr.ConfigParser;
import org.plovr.docgen.DescriptorPass;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gson.JsonObject;
import com.google.javascript.jscomp.CustomPassExecutionTime;

public class JsDocCommand extends AbstractCommandRunner<JsDocCommandOptions> {

  @Override
  JsDocCommandOptions createOptions() {
    return new JsDocCommandOptions();
  }

  @Override
  String getUsageIntro() {
    return "Specify a single config to generate the documentation for " +
    		"all of its source files.";
  }

  @Override
  int runCommandWithOptions(JsDocCommandOptions options) throws IOException {
    // Even though logging would get printed to stderr and not stdout, it is
    // still distracting and feels wrong. May revisit this at some point.
    Logger.getLogger("org.plovr").setLevel(Level.WARNING);

    List<String> arguments = options.getArguments();
    if (arguments.size() != 1) {
      printUsage();
      return 1;
    }

    File configFile = new File(arguments.get(0));
    Config.Builder builder = ConfigParser.createBuilderFromFile(configFile);

    // PlovrCompilerOptions.ideMode must be set to true, or else the AST will
    // not contain all of the necessary JSDocInfo.
    JsonObject experimentalCompilerOptions = builder.getExperimentalCompilerOptions();
    if (experimentalCompilerOptions == null) {
      experimentalCompilerOptions = new JsonObject();
    }
    experimentalCompilerOptions.addProperty("ideMode", true);
    builder.setExperimentalCompilerOptions(experimentalCompilerOptions);

    // Add DescriptorPass
    ListMultimap<CustomPassExecutionTime, CompilerPassFactory> customPasses =
        ArrayListMultimap.create(builder.getCustomPasses());
    customPasses.put(CustomPassExecutionTime.BEFORE_CHECKS,
        new CompilerPassFactory(DescriptorPass.class));
    builder.setCustomPasses(customPasses);

    Config config = builder.build();
    try {
      CompileRequestHandler.compile(config);
    } catch (CompilationException e) {
      e.printStackTrace();
      return 1;
    }

    // Do not print anything to the console upon success.
    return 0;
  }

}
