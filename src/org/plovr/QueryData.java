package org.plovr;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;

/**
 * {@link QueryData} represents the query parameters in a URI.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class QueryData {

  LinkedListMultimap<String, String> params;

  private QueryData(LinkedListMultimap<String, String> params) {
    this.params = params;
  }

  /**
   * @param name of a query parameter
   * @return The value associated with the query parameter, or null if there is
   *     no value for {@code name}. If there are multiple value for the query
   *     parameters, the first value is returned, where "first" is the first one
   *     that appears when the query string is read left to right.
   */
  public String getParam(String name) {
    List<String> values = params.get(name);
    return values.size() > 0 ? values.get(0) : null;
  }

  public Set<String> getParams() {
    return ImmutableSet.copyOf(params.keySet());
  }

  public static QueryData createFromUri(URI uri) {
    String rawQuery = uri.getRawQuery();
    LinkedListMultimap<String, String> params = LinkedListMultimap.create();
    if (rawQuery != null) {
      String[] pairs = rawQuery.split("&");
      for (String pair : pairs) {
        String[] keyValuePair = pair.split("=");
        String key = keyValuePair[0];
        String value = keyValuePair.length == 2 ? keyValuePair[1] : "";
        params.put(decode(key), decode(value));
      }
    }
    return new QueryData(params);
  }

  static String encode(String str) {
    try {
      return URLEncoder.encode(str, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private static String decode(String str) {
    try {
      return URLDecoder.decode(str, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
