package org.plovr.util;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

/**
 * {@link VersionUtil} is a utility for reporting the version numbers for
 * various components of plovr.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class VersionUtil {

  private static final Map<String, String> versions =
      ImmutableMap.<String, String>builder().
      put("closure-library", readRevisionFromJar("closure-library")).
      put("closure-compiler", readRevisionFromJar("closure-compiler")).
      put("closure-templates", readRevisionFromJar("closure-templates")).
      put("plovr", readRevisionFromJar("plovr")).
      build();

  private static final String readRevisionFromJar(String projectName) {
    String resourceName = "/revisions/rev-" + projectName + ".txt";
    URL url = Resources.getResource(VersionUtil.class, resourceName);
    try {
      return Resources.toString(url, Charsets.US_ASCII).trim();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Utility class; do not instantiate. */
  private VersionUtil() {}

  /**
   * @param projectName should be one of "closure-library", "closure-compiler",
   *        "closure-templates", or "plovr"
   */
  public static @Nullable String getRevision(String projectName) {
    return versions.get(projectName);
  }
}
