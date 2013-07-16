package org.plovr.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.junit.Test;
import org.plovr.GsonUtil;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

/**
 * {@link GsonUtilTest} is a unit test for {@link GsonUtil}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class GsonUtilTest {

  @Test
  public void testStringOrNull() {
    assertEquals(null, GsonUtil.stringOrNull(null));
    assertEquals("foo", GsonUtil.stringOrNull(new JsonPrimitive("foo")));
    assertEquals(null, GsonUtil.stringOrNull(new JsonPrimitive(400)));
    assertEquals(null, GsonUtil.stringOrNull(new JsonPrimitive(false)));
    assertEquals(null, GsonUtil.stringOrNull(new JsonArray()));
    assertEquals(null, GsonUtil.stringOrNull(new JsonObject()));
  }

  @Test
  public void testStringToJsonPrimitive() {
    assertEquals(new JsonPrimitive("foo"),
        GsonUtil.STRING_TO_JSON_PRIMITIVE.apply("foo"));
  }

  @Test(expected = NullPointerException.class)
  public void testStringToJsonPrimitiveThrowsNullPointerException() {
    GsonUtil.STRING_TO_JSON_PRIMITIVE.apply(null);
  }

  @Test
  public void testToListOfStrings() {
    assertEquals(null, GsonUtil.toListOfStrings(null));

    assertEquals(null, GsonUtil.toListOfStrings(JsonNull.INSTANCE));

    assertEquals(ImmutableList.of("foo"),
        GsonUtil.toListOfStrings(new JsonPrimitive("foo")));

    JsonArray array = new JsonArray();
    array.add(new JsonPrimitive("foo"));
    array.add(new JsonPrimitive("bar"));
    array.add(new JsonPrimitive("baz"));
    assertEquals(ImmutableList.of("foo", "bar", "baz"),
        GsonUtil.toListOfStrings(array));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testToListOfStringsThrowsIllegalArgumentException() {
    JsonArray array = new JsonArray();
    array.add(new JsonPrimitive("foo"));
    array.add(new JsonPrimitive(400));
    GsonUtil.toListOfStrings(array);
  }

  /**
   * Tests what behavior is allowed by {@link JsonReader#setLenient(boolean)}.
   */
  @Test
  public void testLenientJsonReader() {
    StringReader reader = new StringReader("{foo: ['bar', 'baz',]}");
    JsonReader jsonReader = new JsonReader(reader);
    jsonReader.setLenient(true);
    JsonParser parser = new JsonParser();
    JsonElement root = parser.parse(jsonReader);

    assertTrue(root.isJsonObject());
    JsonObject jsonObject = root.getAsJsonObject();

    assertTrue(jsonObject.has("foo"));
    JsonElement fooValue = jsonObject.get("foo");
    assertTrue(fooValue.isJsonArray());

    // This is atrocious: this should parse as a two element array.
    JsonArray fooArray = fooValue.getAsJsonArray();
    assertEquals(3, fooArray.size());
    assertEquals("bar", fooArray.get(0).getAsString());
    assertEquals("baz", fooArray.get(1).getAsString());
    assertTrue(fooArray.get(2).isJsonNull());
  }
}
