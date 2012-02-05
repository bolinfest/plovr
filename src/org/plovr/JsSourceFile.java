package org.plovr;

import java.io.File;
import java.io.IOException;

import org.plovr.io.Files;

/**
 * {@link JsSourceFile} represents a JavaScript file.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class JsSourceFile extends LocalFileJsInput {

  JsSourceFile(String name, File source) {
    super(name, source);
  }

  @Override
  public String getCode() {
    try {
      return Files.toString(getSource());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean supportsEtags() {
    return true;
  }
}
