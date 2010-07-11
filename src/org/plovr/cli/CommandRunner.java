package org.plovr.cli;

import java.io.IOException;

public interface CommandRunner {

  public void runCommand(String[] args) throws IOException;

}
