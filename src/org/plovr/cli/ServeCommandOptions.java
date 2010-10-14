package org.plovr.cli;

import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import com.google.common.collect.Lists;

public class ServeCommandOptions extends AbstractCommandOptions {

  @Option(name = "--listen",
      aliases = {"-l"},
      usage = "The address on which to listen. " +
      		"Defaults to '0' but could also be 'localhost'.")
  private String listenAddress = "0";

  @Option(name = "--port",
      aliases = {"-p"},
      usage = "The port on which to run the server.")
  private int port = 9810;

  @Argument
  private List<String> arguments = Lists.newLinkedList();

  public String getListenAddress() {
    return listenAddress;
  }

  public int getPort() {
    return port;
  }

  public List<String> getArguments() {
    return arguments;
  }
}

