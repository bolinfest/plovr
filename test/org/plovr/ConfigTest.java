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
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;

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
    assertFalse(options.instrumentForCoverage);
    assertNull(options.checkMissingGetCssNameBlacklist);
    assertEquals(CheckLevel.OFF, options.checkShadowVars);
    assertFalse(options.getAcceptConstKeyword());
    assertNull(options.getOutputCharset());
    assertEquals(LanguageMode.ECMASCRIPT3, options.getLanguageIn());

    JsonParser parser = new JsonParser();
    JsonObject experimentalOptions = parser.parse("{" +
    		"\"allowLegacyJsMessages\": true, " +
    		"\"instrumentForCoverage\": true, " +
    		"\"checkMissingGetCssNameBlacklist\": \"hello world\", " +
    		"\"checkShadowVars\": \"ERROR\", " +
    		"\"acceptConstKeyword\": true, " +
    		"\"outputCharset\": \"UTF-8\", " +
    		"\"languageIn\": \"ECMASCRIPT5\"" +
    		"}").getAsJsonObject();
    Config.applyExperimentalCompilerOptions(experimentalOptions, options);

    assertTrue(options.allowLegacyJsMessages);
    assertTrue(options.instrumentForCoverage);
    assertEquals("hello world", options.checkMissingGetCssNameBlacklist);
    assertEquals(CheckLevel.ERROR, options.checkShadowVars);
    assertTrue(options.getAcceptConstKeyword());
    assertEquals("UTF-8", options.getOutputCharset());
    assertEquals(LanguageMode.ECMASCRIPT5, options.getLanguageIn());
  }
}
