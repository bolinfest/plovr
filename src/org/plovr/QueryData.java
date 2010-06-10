package org.plovr;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;

import com.google.common.collect.LinkedListMultimap;

class QueryData {

  LinkedListMultimap<String, String> params;

  private QueryData(LinkedListMultimap<String, String> params) {
    this.params = params;
  }

  String getParam(String key) {
    List<String> values = params.get(key);
    return values.size() > 0 ? values.get(0) : null;
  }

  static QueryData createFromUri(URI uri) {
    String rawQuery = uri.getRawQuery();
    LinkedListMultimap<String, String> params = LinkedListMultimap.create();
    String[] pairs = rawQuery.split("&");
    for (String pair : pairs) {
      String[] keyValuePair = pair.split("=");
      String key = keyValuePair[0];
      String value = keyValuePair.length == 2 ? keyValuePair[1] : "";
      params.put(decode(key), decode(value));
    }
    return new QueryData(params);
  }

  private static String decode(String str) {
    try {
      return URLDecoder.decode(str, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
