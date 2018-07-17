package org.plovr;

import java.io.PrintStream;

/**
 * Error when a Plovr config is malformed or has bad data.
 */
public class ConfigParseException extends Exception {
  public ConfigParseException(String msg) {
    super(msg);
  }

  public ConfigParseException(Throwable cause) {
    super(cause.getMessage());
  }

  public void print(PrintStream w) {
    w.println(getMessage());
  }
}
