package org.plovr.cli;

import org.kohsuke.args4j.Option;

/**
 * {@code TestCommandOptions} subclasses {@link ServeCommandOptions} because it
 * has to run a plovr server as a side-effect of running the tests, so this
 * ensures that all of the server options are available during testing.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class TestCommandOptions extends ServeCommandOptions {

  @Option(name = "--timeout",
      usage = "Time, in seconds, after which a test suite should timeout.")
  private int timeout = 60;

  public int getTimeout() {
    return timeout;
  }
}
