package org.plovr.cli;

import java.io.IOException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.google.common.base.Pair;

abstract class AbstractCommandRunner<T extends AbstractCommandOptions> implements CommandRunner {

  abstract T createOptions();

  private final Pair<T, CmdLineParser> createParser() {
    T options = createOptions();
    return Pair.of(options, new CmdLineParser(options));
  }

  @Override
  public final void runCommand(String[] args) throws IOException {
    Pair<T, CmdLineParser> parserAndOptions = createParser();
    T options = parserAndOptions.getFirst();
    CmdLineParser parser = parserAndOptions.getSecond();

    boolean isConfigValid = true;
    try {
      parser.parseArgument(args);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      isConfigValid = false;
    }

    if (isConfigValid && !options.showHelp()) {
      runCommandWithOptions(options);
    } else {
      printUsage(parser);
    }
  }

  public final void printUsage() {
    CmdLineParser parser = createParser().getSecond();
    printUsage(parser);
  }

  public final void printUsage(CmdLineParser parser) {
    String intro = getUsageIntro();
    if (intro != null) {
      System.err.println(intro);
    }
    parser.printUsage(System.err);
  }

  abstract void runCommandWithOptions(T options) throws IOException;

  /**
   * @return may be null
   */
  abstract String getUsageIntro();
}
