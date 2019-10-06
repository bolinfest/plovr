package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    assertFalse(options.isExternExportsEnabled());
    assertNull(options.checkMissingGetCssNameBlacklist);
    assertNull(options.getOutputCharset());
    assertEquals(LanguageMode.ECMASCRIPT_2017, options.getLanguageIn());

    JsonParser parser = new JsonParser();
    JsonObject experimentalOptions = parser.parse("{" +
        "\"externExports\": true, " +
        "\"checkMissingGetCssNameBlacklist\": \"hello world\", " +
        "\"outputCharset\": \"UTF-8\", " +
        "\"languageIn\": \"ECMASCRIPT5\"" +
        "}").getAsJsonObject();
    Config.applyExperimentalCompilerOptions(experimentalOptions, options);

    assertTrue(options.isExternExportsEnabled());
    assertEquals("hello world", options.checkMissingGetCssNameBlacklist);
    assertEquals(Charsets.UTF_8, options.getOutputCharset());
    assertEquals(LanguageMode.ECMASCRIPT5, options.getLanguageIn());
  }

  @Test
  public void testOutputAndGlobalScopeWrapper() {
    Config.Builder builder = Config.builderForTesting();
    builder.addInput(new File("fake-input.js"), "fake-input.js");
    builder.setOutputWrapper("(function () { %output% })()");
    builder.setCompilationMode(CompilationMode.ADVANCED);

    assertEquals("(function () { %output% })()",
                 builder.build().getOutputAndGlobalScopeWrapper(false, "", ""));
    assertEquals("(function () { %output% })()\n//# sourceURL=foo.js",
                 builder.build().getOutputAndGlobalScopeWrapper(false, "", "foo.js"));

    builder.setOutputWrapper("");
    builder.setGlobalScopeName("_mdm");

    assertEquals("(function(z){\n%output%}).call(this, _mdm);",
                 builder.build().getOutputAndGlobalScopeWrapper(false, "", ""));
    assertEquals("var _mdm={};(function(z){\n%output%}).call(this, _mdm);",
                 builder.build().getOutputAndGlobalScopeWrapper(true, "", ""));

    builder.setOutputWrapper("(function () { %output% })()");
    builder.setGlobalScopeName("_mdm");

    assertEquals("(function () { (function(z){\n%output%}).call(this, _mdm); })()",
                 builder.build().getOutputAndGlobalScopeWrapper(false, "", ""));
  }

  @Test
  public void testSourceMapUrl() {
    Config.Builder builder = Config.builderForTesting();
    builder.setId("id");
    builder.addInput(new File("fake-input.js"), "fake-input.js");

    builder.setSourceMapBaseUrl("http://plovr.org/");
    assertEquals("%output%\n//# sourceMappingURL=http://plovr.org/id.map",
                 builder.build().getOutputAndGlobalScopeWrapper(false, "", ""));

    builder.setSourceMapBaseUrl("http://plovr.org/path/");
    assertEquals("%output%\n//# sourceMappingURL=http://plovr.org/path/id.map",
                 builder.build().getOutputAndGlobalScopeWrapper(false, "", ""));
  }
}
