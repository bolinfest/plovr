package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.PlovrCompilerOptions;

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
    PlovrCompilerOptions options = new PlovrCompilerOptions();
    assertFalse(options.getProcessObjectPropertyString());
    assertFalse(options.isExternExportsEnabled());
    assertNull(options.checkMissingGetCssNameBlacklist);
    assertEquals(CheckLevel.OFF, options.getReportUnknownTypes());
    assertFalse(options.getAcceptConstKeyword());
    assertNull(options.getOutputCharset());
    assertEquals(LanguageMode.ECMASCRIPT3, options.getLanguageIn());

    JsonParser parser = new JsonParser();
    JsonObject experimentalOptions = parser.parse("{" +
    		"\"processObjectPropertyString\": true, " +
    		"\"externExports\": true, " +
    		"\"checkMissingGetCssNameBlacklist\": \"hello world\", " +
    		"\"reportUnknownTypes\": \"ERROR\", " +
    		"\"acceptConstKeyword\": true, " +
    		"\"outputCharset\": \"UTF-8\", " +
    		"\"languageIn\": \"ECMASCRIPT5\"" +
    		"}").getAsJsonObject();
    Config.applyExperimentalCompilerOptions(experimentalOptions, options);

    assertTrue(options.getProcessObjectPropertyString());
    assertTrue(options.isExternExportsEnabled());
    assertEquals("hello world", options.checkMissingGetCssNameBlacklist);
    assertEquals(CheckLevel.ERROR, options.getReportUnknownTypes());
    assertTrue(options.getAcceptConstKeyword());
    assertEquals("UTF-8", options.getOutputCharset());
    assertEquals(LanguageMode.ECMASCRIPT5, options.getLanguageIn());
  }
}
