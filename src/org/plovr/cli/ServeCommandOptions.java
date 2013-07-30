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

  @Option(name = "--jks",
      usage = "Keystore file (.jks) containing SSL certificates to serve via https://")
  private String jksFile = "";

  @Option(name = "--passphrase",
      usage = "Passphrase for the keystore (--jks)")
  private String passphrase = "";

  @Argument
  private List<String> arguments = Lists.newLinkedList();

  public String getListenAddress() {
    return listenAddress;
  }

  public int getPort() {
    return port;
  }

  public String getJksFile() {
    return jksFile;
  }

  public String getPassphrase() {
    return passphrase;
  }

  public List<String> getArguments() {
    return arguments;
  }
}

