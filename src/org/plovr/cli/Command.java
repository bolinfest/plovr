package org.plovr.cli;

import java.io.IOException;

/**
 * {@link Command} is the list of commands that the plovr executable takes.
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public enum Command {

  SERVE("serve", new ServeCommand()),

  BUILD("build", new BuildCommand()),

  ;

  private final String name;

  private final CommandRunner commandRunner;

  Command(String name, CommandRunner commandRunner) {
    this.name = name;
    this.commandRunner = commandRunner;
  }

  public static Command getCommandForName(String name) {
    for (Command command : Command.values()) {
      if (command.name.equals(name)) {
        return command;
      }
    }
    return null;
  }

  public void execute(String[] args) throws IOException {
    commandRunner.runCommand(args);
  }

  @Override
  public String toString() {
    return name;
  }
}
