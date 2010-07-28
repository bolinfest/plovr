package org.plovr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * {@link GsonTest} tests the behavior of various objects in the Gson API.
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class GsonTest {

  @Test
  public void gettingStringAsString() {
    JsonElement stringPrimitive = new JsonPrimitive("hello");
    assertEquals("\"hello\"", stringPrimitive.toString());
    assertEquals("hello", stringPrimitive.getAsString());
  }

  @Test
  public void gettingNumberAsString() {
    JsonElement numberPrimitive = new JsonPrimitive(42);
    assertEquals("42", numberPrimitive.toString());
    assertEquals("42", numberPrimitive.getAsString());
  }

  @Test
  public void gettingBooleanAsString() {
    JsonElement booleanPrimitive = new JsonPrimitive(true);
    assertEquals("true", booleanPrimitive.toString());
    assertEquals("true", booleanPrimitive.getAsString());
  }

  @Test(expected = NullPointerException.class)
  public void gettingNullAsString() {
    JsonElement nullPrimitive = new JsonPrimitive((String)null);
    assertEquals("null", nullPrimitive.getAsString());
  }

  @Test(expected = NullPointerException.class)
  public void gettingNullToString() {
    JsonElement nullPrimitive = new JsonPrimitive((String)null);
    assertEquals("null", nullPrimitive.toString());
  }

}
