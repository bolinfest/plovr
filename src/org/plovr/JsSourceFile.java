package org.plovr;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

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
      return Files.toString(getSource(), Charsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
