package org.plovr.cli;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Joiner;

/**
 * {@link Main} kicks off the plovr buildr.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class Main {

  private Main() {}

  private static void usage() {
    System.err.println("Must specify at least one of: " +
        Joiner.on(',').join(Command.values()));
    System.exit(1);
  }

  /**
   * Runs on port 9810 by default. (Eventually there will be a real flags
   * architecture behind this.)
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    // The Compiler logging statements produce too much output.
    Logger.getLogger("com.google.javascript.jscomp").setLevel(Level.OFF);

    if (args.length == 0) {
      usage();
    }

    Command command = Command.getCommandForName(args[0]);
    if (command == null) {
      usage();
    } else {
      String[] remainingArgs = new String[args.length - 1];
      System.arraycopy(args, 1, remainingArgs, 0, remainingArgs.length);
      command.execute(remainingArgs);
    }
  }
}
