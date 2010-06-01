package org.plovr;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Main} kicks off the plovr buildr.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class Main {

  private Main() {}
  
  /**
   * Runs on port 9810 by default. (Eventually there will be a real flags
   * architecture behind this.)
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println("Must specify at least one config file");
      System.exit(1);
      return;
    }
    
    // The Compiler logging statements produce too much output.
    Logger.getLogger("com.google.javascript.jscomp").setLevel(Level.OFF);

    // Register all of the configs.
    CompilationServer server = new CompilationServer(9810);
    for (String arg : args) {
      File configFile = new File(arg);
      Config config = ConfigParser.parseFile(configFile);
      server.registerConfig(config);
    }
    
    server.run();
  }
}
