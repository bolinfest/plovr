package org.plovr;

import java.io.File;

import com.google.common.base.Preconditions;

/**
 * A {@ConfigPath} represents an argument associated with the {@code paths}
 * option in a plovr config. It contains both the {@link File} specified by the
 * argument in the config file as well as the original argument text.
 * <p>
 * These objects are used together to:
 * <ol>
 *   <li>Find any input files that are descendants of the {@code paths} argument
 *       when the argument specifies a directory.
 *   <li>Name the resulting {@link LocalFileJsInput} such that the name contains
 *       the argument name and the path relative to the directory, but not the
 *       full path to the {@link LocalFileJsInput}. Full paths may be harder to
 *       read when displayed in URLs, and could leak information.
 * </ol>
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class ConfigPath {

  private final File file;
  private final String name;

  public ConfigPath(File file, String name) {
    Preconditions.checkNotNull(file);
    Preconditions.checkNotNull(name);
    this.file = file;
    this.name = normalize(name, file.isDirectory());
  }

  static String normalize(String name, boolean isDirectory) {
    if (isDirectory) {
      // Strip any trailing path separators.
      return name.replaceAll("[/\\\\]$", "") + "/";
    } else {
      return name;
    }
  }

  public File getFile() {
    return file;
  }

  /**
   * If and only if {@link #getFile()} returns a directory, then this method
   * will return a string with a trailing slash.
   */
  public String getName() {
    return name;
  }
}
