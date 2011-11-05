package org.plovr.cli;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.plovr.CompilationServer;
import org.plovr.Config;
import org.plovr.ConfigParser;

public class ServeCommand extends AbstractCommandRunner<ServeCommandOptions> {

  @Override
  ServeCommandOptions createOptions() {
    return new ServeCommandOptions();
  }

  @Override
  public int runCommandWithOptions(ServeCommandOptions options) throws IOException {
    List<String> arguments = options.getArguments();
    if (arguments.size() < 1) {
      printUsage();
      return 1;
    }

    CompilationServer server = new CompilationServer(options.getListenAddress(),
        options.getPort());
    // Register all of the configs.
    for (String arg : arguments) {
      File configFile = new File(arg);
      Config config = ConfigParser.parseFile(configFile);
      server.registerConfig(config);
    }
    server.run();
    return STATUS_NO_EXIT;
  }

  @Override
  String getUsageIntro() {
    return "Specify a list of config files to serve.";
  }

}
