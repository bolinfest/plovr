package org.plovr.cli;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.WebDriver;
import org.plovr.Config;
import org.plovr.ConfigParser;
import org.plovr.TestHandler;
import org.plovr.webdriver.TestRunner;
import org.plovr.webdriver.WebDriverFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class TestCommand extends AbstractCommandRunner<TestCommandOptions> {

  @Override
  TestCommandOptions createOptions() {
    return new TestCommandOptions();
  }

  @Override
  @SuppressWarnings("deprecation")
  int runCommandWithOptions(TestCommandOptions options) throws IOException {
    List<String> arguments = options.getArguments();
    if (arguments.size() < 1) {
      printUsage();
      return 1;
    }

    Thread serverThread = new Thread(new ServeCommandRunner(options));
    serverThread.start();

    int exitCode = 0;
    try {
      for (String configFile: arguments) {
        Config config = ConfigParser.parseFile(new File(configFile));

        int timeout = options.getTimeout() * 1000;
        List<WebDriver> drivers = Lists.transform(config.getWebDriverFactories(),
            new Function<WebDriverFactory, WebDriver>() {
              @Override
              public WebDriver apply(WebDriverFactory factory) {
                return factory.newInstance();
              }
        });

        Set<String> relativeTestPaths = TestHandler.getRelativeTestFilePaths(config);
        for (String relativeTestPath : relativeTestPaths) {
          URL url = new URL(String.format("http://localhost:%d/test/%s/%s",
              options.getPort(), config.getId(), relativeTestPath));
          TestRunner testRunner = new TestRunner(url, drivers, timeout);
          if (!testRunner.run()) {
            exitCode = 1;
          }
        }
      }
    } finally {
      // TODO: Create a cleaner API to shut down the server.
      serverThread.stop();
    }

    return exitCode;
  }

  @Override
  String getUsageIntro() {
    return "Specify one or more configs whose tests should be run.";
  }

  private static class ServeCommandRunner implements Runnable {

    private final ServeCommandOptions options;

    private ServeCommandRunner(ServeCommandOptions options) {
      this.options = options;
    }

    public void run() {
      ServeCommand command = new ServeCommand();
      try {
        command.runCommandWithOptions(options);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
