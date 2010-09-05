package org.plovr.cli;

import org.kohsuke.args4j.Option;

public class SoyWebCommandOptions extends AbstractCommandOptions {

  @Option(name = "--port",
      aliases = {"-p"},
      usage = "The port on which to run the server.")
  private int port = 9811;

  @Option(name = "--dir",
      aliases = {"-d"},
      usage = "Directory that contains the Soy files")
  private String dir = null;

  @Option(name = "--static",
      aliases = {"-s"},
      usage = "Parse the Soy files once on startup")
  private boolean isStatic = false;

  public SoyWebCommandOptions() {}

  public int getPort() {
    return port;
  }

  public String getDir() {
    return dir;
  }

  public boolean isStatic() {
    return isStatic;
  }
}
