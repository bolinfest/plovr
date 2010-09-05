package org.plovr.cli;

import java.io.File;
import java.io.IOException;

import org.plovr.soy.server.Config;
import org.plovr.soy.server.Server;

public class SoyWebCommand extends AbstractCommandRunner<SoyWebCommandOptions> {

  @Override
  SoyWebCommandOptions createOptions() {
    return new SoyWebCommandOptions();
  }

  @Override
  void runCommandWithOptions(SoyWebCommandOptions options) throws IOException {
    Config config = new Config(
        options.getPort(),
        new File(options.getDir()),
        options.isStatic());
    Server server = new Server(config);
    server.run();
  }

  @Override
  String getUsageIntro() {
    return "Specify a directory that contains Soy files";
  }
}
