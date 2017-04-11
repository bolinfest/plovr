package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.CustomPassExecutionTime;

/**
 * {@link ConfigParserTest} is a unit test for {@link ConfigParser}.
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class ConfigParserTest {

  /**
   * Test to ensure that config inheritance works correctly.
   * This is particularly tricky when resolving relative filenames: a filename
   * for an input, path, or extern should always be resolved relative to the
   * config file in which it is referenced.
   */
  @Test
  public void testInherits() throws IOException {
    File configFile = new File("testdata/inherits/child/childconfig.js");
    assertTrue("Could not find test config file", configFile.exists());
    Config config = ConfigParser.parseFile(configFile);

    // id
    final String configId = "child";
    assertEquals("Resulting config should have id: " + configId, configId,
        config.getId());

    // inputs
    Manifest manifest = config.getManifest();
    List<JsInput> inputs = manifest.getRequiredInputs();
    assertEquals(
        "Resulting config should only contain inputs specified in child config",
        2, inputs.size());
    assertEquals("a.js", inputs.get(0).getName());
    assertEquals("b.js", inputs.get(1).getName());

    // paths
    Set<File> paths = manifest.getDependencies();
    assertEquals("Should have only one path", 1, paths.size());
    File path = Iterables.getFirst(paths, null);
    assertTrue(
        "path should be a subdirectory of inherits, not child\n" +
        "Got: " + path.getAbsolutePath(),
        path.getAbsolutePath().endsWith(
            Joiner.on(File.separator).join(
                "inherits", "child", "..", "fakedir")));

    // id-generators
    Set<String> idGenerators = config.getIdGenerators();
    assertEquals(idGenerators, Sets.newHashSet("goog.events.getUniqueId"));

    // custom-passes
    ListMultimap<CustomPassExecutionTime, CompilerPassFactory> customPasses =
        config.getCustomPasses();
    assertNull(customPasses);

    // soy-function-plugins
    List<String> soyFunctionPlugins = config.getSoyFunctionPlugins();
    assertEquals(Lists.newArrayList(
        "com.google.template.soy.xliffmsgplugin.XliffMsgPluginModule",
        "org.plovr.soy.function.PlovrModule"),
        soyFunctionPlugins);

    // modules
    assertNull(config.getModuleConfig());

    // defines
    Map<String, JsonPrimitive> expectedDefines = ImmutableMap.of(
        "goog.userAgent.ASSUME_WEBKIT", new JsonPrimitive(true),
        "goog.DEBUG", new JsonPrimitive(false));
    assertEquals(expectedDefines, config.getDefines());

    // checks
    Map<String, CheckLevel> expectedCheckLevels = ImmutableMap.of(
        "checkTypes", CheckLevel.WARNING);
    assertEquals(expectedCheckLevels, config.getCheckLevelsForDiagnosticGroups());

    // experimental-compiler-options
    JsonObject expectedExperimentalCompilerOptions = new JsonObject();
    expectedExperimentalCompilerOptions.add("instrumentForCoverage",
        new JsonPrimitive(true));
    assertEquals(expectedExperimentalCompilerOptions,
        config.getExperimentalCompilerOptions());
  }

  @Test
  public void testInputNames() throws IOException, CompilationException {
    File configFile = new File("testdata/name-collision/config.js");
    assertTrue("Could not find test config file", configFile.exists());
    Config config = ConfigParser.parseFile(configFile);

    List<String> inputNames = Lists.transform(config.getManifest().getInputsInCompilationOrder(),
        new Function<JsInput, String>() {
          @Override
          public String apply(JsInput input) {
            return input.getName();
          }
    });
    assertEquals(
        ImmutableList.of(
            "/closure/goog/base.js",
            "/closure/goog/deps.js",
            "/closure/goog/i18n/bidi.js",
            "/closure/goog/debug/error.js",
            "/closure/goog/dom/nodetype.js",
            "/closure/goog/string/string.js",
            "/closure/goog/asserts/asserts.js",
            "/closure/goog/array/array.js",
            "/closure/goog/dom/tagname.js",
            "/closure/goog/object/object.js",
            "/closure/goog/dom/tags.js",
            "/closure/goog/string/typedstring.js",
            "/closure/goog/string/const.js",
            "/closure/goog/html/safescript.js",
            "/closure/goog/html/safestyle.js",
            "/closure/goog/html/safestylesheet.js",
            "/closure/goog/fs/url.js",
            "/closure/goog/html/trustedresourceurl.js",
            "/closure/goog/html/safeurl.js",
            "/closure/goog/labs/useragent/util.js",
            "/closure/goog/labs/useragent/browser.js",
            "/closure/goog/html/safehtml.js",
            "/closure/goog/html/uncheckedconversions.js",
            "/closure/goog/structs/structs.js",
            "/closure/goog/structs/collection.js",
            "/closure/goog/functions/functions.js",
            "/closure/goog/math/math.js",
            "/closure/goog/iter/iter.js",
            "/closure/goog/structs/map.js",
            "/closure/goog/structs/set.js",
            "/closure/goog/labs/useragent/engine.js",
            "/closure/goog/labs/useragent/platform.js",
            "/closure/goog/reflect/reflect.js",
            "/closure/goog/useragent/useragent.js",
            "/closure/goog/debug/debug.js",
            "/closure/goog/dom/browserfeature.js",
            "/closure/goog/dom/safe.js",
            "/closure/goog/math/coordinate.js",
            "/closure/goog/math/size.js",
            "/closure/goog/dom/dom.js",
            "/closure/goog/structs/inversionmap.js",
            "/closure/goog/i18n/graphemebreak.js",
            "/closure/goog/format/format.js",
            "/closure/goog/i18n/bidiformatter.js",
            "/closure/goog/html/legacyconversions.js",
            "/closure/goog/uri/utils.js",
            "/closure/goog/uri/uri.js",
            "/closure/goog/soy/data.js",
            "/closure/goog/soy/soy.js",
            "/closure/goog/string/stringbuffer.js",
            "../../closure/closure-templates/javascript/soyutils_usegoog.js",
            "custom/foo/bar.soy",
            "main/foo/bar.soy",
            "main.js"),
        inputNames);
  }
}
