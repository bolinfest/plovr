package org.plovr.soy.server;

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
}
