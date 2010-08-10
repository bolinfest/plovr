package org.plovr;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * {@link GsonUtil} provides utilities for working with {@link Gson}.
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class GsonUtil {

  private GsonUtil() {}

  /**
   * If element is a {@link JsonPrimitive} that corresponds to a string, then
   * return the value of that string; otherwise, return null.
   */
  public static String stringOrNull(JsonElement element) {
    if (element == null) {
      return null;
    }
    if (element.isJsonPrimitive()) {
      JsonPrimitive primitive = element.getAsJsonPrimitive();
      if (primitive.isString()) {
        return primitive.getAsString();
      }
    }
    return null;
  }
}
