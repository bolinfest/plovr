package org.plovr.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.plovr.GsonUtil;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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

    assertEquals(null, GsonUtil.toListOfStrings(new JsonNull()));

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
}
