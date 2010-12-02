package org.plovr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.plovr.io.Settings;

import com.google.common.io.LineReader;

/**
 * {@link ResourceJsInput} represents a JavaScript file loaded from a JAR, so
 * it will never change, so its dependencies must only be read once.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class ResourceJsInput extends AbstractJsInput {

  private static final Logger logger = Logger.getLogger("org.plovr.ResourceJsInput");

  private final String pathToResource;

  ResourceJsInput(String pathToResource) {
    super(pathToResource);
    this.pathToResource = pathToResource;
  }

  @Override
  public String getCode() {
    try {
      InputStream input = ResourceReader.class.getResourceAsStream(
          pathToResource);
      Readable readable = new InputStreamReader(input, Settings.CHARSET);
      LineReader lineReader = new LineReader(readable);
      StringBuilder builder = new StringBuilder();
      String line;
      while ((line = lineReader.readLine()) != null) {
        builder.append(line + "\n");
      }
      return builder.toString();
    } catch (IOException e) {
      logger.severe(e.getMessage());
      throw new RuntimeException(e);
    }
  }

}
