package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.TestCompilerOptions;

public class ConfigTest {

  @Test(expected = NullPointerException.class)
  public void testSetIdNullArgument() {
    Config.Builder builder = Config.builderForTesting();
    builder.setId(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetIdInvalidArgument() {
    Config.Builder builder = Config.builderForTesting();
    builder.setId("foo/bar");
  }

  @Test
  public void testApplyExperimentalCompilerOptions() {
    TestCompilerOptions options = new TestCompilerOptions();
    assertFalse(options.allowLegacyJsMessages);
    assertNull(options.checkMissingGetCssNameBlacklist);
    assertEquals(CheckLevel.OFF, options.checkShadowVars);
    assertFalse(options.getAcceptConstKeyword());
    assertNull(options.getOutputCharset());

    JsonParser parser = new JsonParser();
    JsonObject experimentalOptions = parser.parse("{" +
    		"\"allowLegacyJsMessages\": true, " +
    		"\"checkMissingGetCssNameBlacklist\": \"hello world\", " +
    		"\"checkShadowVars\": \"ERROR\", " +
    		"\"acceptConstKeyword\": true, " +
    		"\"outputCharset\": \"UTF-8\" " +
    		"}").getAsJsonObject();
    Config.applyExperimentalCompilerOptions(experimentalOptions, options);

    assertTrue(options.allowLegacyJsMessages);
    assertEquals("hello world", options.checkMissingGetCssNameBlacklist);
    assertEquals(CheckLevel.ERROR, options.checkShadowVars);
    assertTrue(options.getAcceptConstKeyword());
    assertEquals("UTF-8", options.getOutputCharset());
  }
}
