package org.plovr;

import java.io.File;
import java.io.IOException;

/**
 * {@link Main} kicks off the plovr buildr.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class Main {

  /**
   * Runs on port 9810 by default. (Eventually there will be a real flags
   * architecture behind this.)
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Must supply exactly one argument: the config file");
      System.exit(1);
      return;
    }

    File configFile = new File(args[0]);
    Config config = ConfigParser.parseFile(configFile);
    CompilationServer server = new CompilationServer(9810);
    server.registerConfig(config);
    
    server.run();
  }
}
