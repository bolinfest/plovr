package org.plovr.cli;

import org.kohsuke.args4j.Option;

public abstract class AbstractCommandOptions {

  @Option(name = "--help",
      usage = "Prints the available options and exits.")
  private boolean help = false;

  public boolean showHelp() {
    return help;
  }

}
