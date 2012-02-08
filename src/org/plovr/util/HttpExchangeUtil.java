package org.plovr.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.sun.net.httpserver.HttpExchange;

/**
 * There is a bug in Google Chrome that affects plovr in serve mode:
 * http://code.google.com/p/chromium/issues/detail?id=105824
 * which was discussed at length on the Google Group:
 * https://groups.google.com/forum/?fromgroups#!topic/plovr/yWiGfVG-hq4
 * This class contains utility methods to do user agent detection to avoid the
 * bug.
 */
public final class HttpExchangeUtil {

  /** Utility class: do not instantiate. */
  private HttpExchangeUtil() {}

  private static final Pattern CHROME_VERSION_PATTERN =
      Pattern.compile("Chrome/(\\d+)\\.");

  public static boolean isGoogleChrome17OrEarlier(HttpExchange exchange) {
    String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
    return isGoogleChrome17OrEarlier(userAgent);
  }

  @VisibleForTesting
  static boolean isGoogleChrome17OrEarlier(String userAgent) {
    boolean isChrome = userAgent != null && userAgent.contains("Chrome");
    if (isChrome) {
      Matcher matcher = CHROME_VERSION_PATTERN.matcher(userAgent);
      if (matcher.find()) {
        int version = Integer.parseInt(matcher.group(1), 10);
        return version <= 17;
      }
    }
    return false;
  }

  public static boolean isGoogleChrome(HttpExchange exchange) {
    String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
    return userAgent != null && userAgent.contains("Chrome");
  }
}
