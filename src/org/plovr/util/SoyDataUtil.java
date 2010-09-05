package org.plovr.util;

import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.data.restricted.BooleanData;
import com.google.template.soy.data.restricted.FloatData;
import com.google.template.soy.data.restricted.IntegerData;
import com.google.template.soy.data.restricted.NullData;
import com.google.template.soy.data.restricted.StringData;

public final class SoyDataUtil {

  /** utility class; do not instantiate */
  private SoyDataUtil() {}

  // TODO(bolinfest): Create unit test.

  public static SoyData jsonToSoyData(JsonElement el) {
    if (el == null || el.isJsonNull()) {
      return NullData.INSTANCE;
    } else if (el.isJsonObject()) {
      SoyMapData map = new SoyMapData();
      for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
        JsonElement value = entry.getValue();
        map.put(entry.getKey(), SoyDataUtil.jsonToSoyData(value));
      }
      return map;
    } else if (el.isJsonArray()) {
      SoyListData list = new SoyListData();
      for (JsonElement item : el.getAsJsonArray()) {
        list.add(jsonToSoyData(item));
      }
      return list;
    } else if (el.isJsonPrimitive()) {
      JsonPrimitive primitive = el.getAsJsonPrimitive();
      if (primitive.isString()) {
        return new StringData(primitive.getAsString());
      } else if (primitive.isBoolean()) {
        return new BooleanData(primitive.getAsBoolean());
      } else if (primitive.isNumber()) {
        if (primitive.getAsDouble() == primitive.getAsInt()) {
          return new IntegerData(primitive.getAsInt());
        } else {
          return new FloatData(primitive.getAsDouble());
        }
      }
    }
    throw new RuntimeException("Not able to convert: " + el);
  }
}
