package org.plovr;

import java.io.File;

import com.google.common.base.Preconditions;

/**
 * {@link FileUtil} is a set of utilities for dealing with files.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class FileUtil {

  /** utility class; do not instantiate */
  private FileUtil() {}

  public static boolean contains(File parent, File child) {
    Preconditions.checkNotNull(parent);
    Preconditions.checkNotNull(child);

    while (child != null) {
      if (parent.equals(child)) {
        return true;
      }
      child = child.getParentFile();
    }

    return false;
  }


  /**
   * @see http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
   */
  public static String humanReadableByteCount(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;
    if (bytes < unit) {
      return bytes + " B";
    }
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }
}
