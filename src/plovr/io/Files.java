package plovr.io;

import java.io.File;
import java.io.IOException;

public final class Files {

  /** Utility class: do not instantiate. */
  private Files() {}

  public static String toString(File file) throws IOException {
    return com.google.common.io.Files.toString(file, Settings.CHARSET);
  }
}
