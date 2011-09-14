package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
        "path should be a subdirectory of inherits, not child",
        path.getAbsolutePath().endsWith("inherits/child/../fakedir"));

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
  }
}
