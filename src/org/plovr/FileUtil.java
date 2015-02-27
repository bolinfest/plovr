package org.plovr;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  public static String nomalizeName(String name) {
    try {
      name = new URI(name).normalize().getPath();
      if (name.startsWith("/")) {
        name = name.substring(1);
      }
      name = name.replaceAll(Pattern.quote("../"), Matcher.quoteReplacement("$$/"));
      return name;
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
