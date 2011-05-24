package org.plovr.cli;

import java.io.IOException;

public interface CommandRunner {

  /**
   * @param args the arguments passed to the command, not including the command
   *     name
   * @return the appropriate exit code for the command
   */
  public int runCommand(String[] args) throws IOException;

}
