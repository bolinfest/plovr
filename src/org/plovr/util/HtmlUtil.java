package org.plovr.util;


public final class HtmlUtil {

  /** Utility class; do not instantiate. */
  private HtmlUtil() {}

  public static String htmlEscape(String text) {
    return text
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&squot;");
  }
}
