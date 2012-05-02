package org.plovr.cli;

import java.io.IOException;
import java.util.logging.Level;

import com.google.javascript.jscomp.Compiler;

/**
 * {@link Main} kicks off the plovr buildr.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class Main {

  private Main() {}

  private static int usage() {
    // TODO(bolinfest): Make this a data-driven list from the Command enum.
    System.err.println("plovr build tool\n");
    System.err.println("basic commands:\n");
    System.err.println(" build    compile the input specified in a config file");
    System.err.println(" info     show the versions of the Closure Tools packaged with plovr");
    System.err.println(" extract  extract messages from the Soy files");
    System.err.println(" serve    start the plovr web server");
    System.err.println(" soyweb   serve static content as well as Soy");
    return 1;
  }

  public static void main(String[] args) throws IOException {
    Integer exitCode = mainWithExitCode(args);
    if (exitCode != null) {
      System.exit(exitCode);
    }
  }

  /**
   * @param args command line arguments
   * @return an exit code or {@code null} if this is a process that should not
   *     exit
   * @throws IOException
   */
  public static Integer mainWithExitCode(String[] args) throws IOException {
    // The Compiler logging statements produce too much output.
    Compiler.setLoggingLevel(Level.OFF);

    if (args.length == 0) {
      return usage();
    }

    Command command = Command.getCommandForName(args[0]);
    if (command == null) {
      return usage();
    } else {
      String[] remainingArgs = new String[args.length - 1];
      System.arraycopy(args, 1, remainingArgs, 0, remainingArgs.length);
      int status = command.execute(remainingArgs);
      if (status == AbstractCommandRunner.STATUS_NO_EXIT) {
        return null;
      } else {
        return status;
      }
    }
  }
}
