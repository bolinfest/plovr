package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.plovr.cli.Main;

import com.google.common.base.Preconditions;

/**
 * {@link PlovrRunner} is used to simulate running plovr from the command line
 * for testing purposes.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class PlovrRunner {

  private PlovrRunner() {}

  /**
   * Runs plovr with the specified command-line arguments, failing if calling
   * plovr does not terminate or returns a non-zero exit code.
   * <p>
   * Note that this does not run a jar'd version of plovr, so resources are
   * <em>not</em> guaranteed to be in the correct location as if it were run
   * from a packaged jarfile. This means that any plovr config targeted by this
   * command will likely need to use the "closure-library" config option, among
   * other plovr config options.
   */
  public static void run(List<String> args) {
    // Ensure the list is non-null and copy it to an array of Strings.
    Preconditions.checkNotNull(args);
    String[] commandLineArgs = new String[args.size()];
    int index = 0;
    for (String arg : args) {
      commandLineArgs[index++] = arg;
    }

    // Run plovr with the specified arguments.
    Integer exitCode = null;
    try {
      exitCode = Main.mainWithExitCode(commandLineArgs);
    } catch (IOException e) {
      fail("Running plovr should not throw an IOException: " + e);
    }
    assertNotNull("Running plovr from a test must not start a process " +
        "that cannot be shut down", exitCode);
    assertEquals("calling plovr terminate with an exit code of 0", 0,
        exitCode.intValue());
  }
}
