package org.plovr;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

  /**
   * @param element must be one of:
   * <ul>
   *   <li>{@code null}, in which case this returns {@code null}
   *   <li>a single string literal
   *   <li>a list of non-null string literals
   * </ul>
   * @return null or a list of non-null strings
   * @throws IllegalArgumentException if {@code element} does not meet any of
   *     the above conditions
   */
  public static List<String> toListOfStrings(JsonElement element)
  throws IllegalArgumentException {
    if (element == null || element.isJsonNull()) {
      return null;
    }

    String str = stringOrNull(element);
    if (str != null) {
      return ImmutableList.of(str);
    }

    if (!element.isJsonArray()) {
      throw new IllegalArgumentException(
          "Must be either null, a single string, or an array of strings, but was: " + element);
    }

    JsonArray array = element.getAsJsonArray();
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (JsonElement el : array) {
      str = stringOrNull(el);
      if (str == null) {
        throw new IllegalArgumentException(
            "List contained an element that was not a string literal: " + el);
      }
      builder.add(str);
    }
    return builder.build();
  }

  public static final Function<String, JsonPrimitive>
      STRING_TO_JSON_PRIMITIVE = new Function<String, JsonPrimitive>() {
        @Override
        public JsonPrimitive apply(String str) {
          return new JsonPrimitive(str);
        }
  };

  public static JsonObject clone(JsonObject value) {
    Preconditions.checkNotNull(value);
    // TODO(bolinfest): See if there is a more efficient way to do this.
    JsonParser parser = new JsonParser();
    return parser.parse(value.toString()).getAsJsonObject();
  }
}
