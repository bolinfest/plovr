package org.plovr.io;

import java.io.File;
import java.io.IOException;

public final class Files {

  /** Utility class: do not instantiate. */
  private Files() {}

  public static String toString(File file) throws IOException {
    return com.google.common.io.Files.toString(file, Settings.CHARSET);
  }

  public static void write(String content, File outputFile) throws IOException {
    com.google.common.io.Files.write(content, outputFile, Settings.CHARSET);
  }
}
