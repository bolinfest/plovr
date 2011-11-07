package org.plovr;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


/**
 * {@link ModuleConfigTest} is a unit test for {@link ModuleConfig}.
 *
 * @author imirkin@alum.mit.edu (Ilia Mirkin)
 */
public class ModuleConfigTest {

  private static final Function<JsInput, String> INPUT_TO_NAME =
    new Function<JsInput, String>() {
      @Override
      public String apply(JsInput input) {
        return input.getName();
      }
  };

  private static final Function<List<JsInput>, List<String>> INPUT_TO_NAME_LIST =
    new Function<List<JsInput>, List<String>>() {
      @Override
      public List<String> apply(List<JsInput> input) {
        return Lists.transform(input, INPUT_TO_NAME);
      }
  };

  private final SoyFileOptions soyFileOptions = new SoyFileOptions();

  private ModuleConfig.ModuleInfo newModule(
      String name, String input, String[] deps) {
    ModuleConfig.ModuleInfo moduleInfo = new ModuleConfig.ModuleInfo();
    moduleInfo.setName(name);
    moduleInfo.setInputs(ImmutableList.of(input));
    moduleInfo.setDeps(ImmutableList.copyOf(deps));
    return moduleInfo;
  }

  private void assertCorrectInputs(
      String module, List<String> expected, List<String> actual) {
    assertEquals("Incorrect inputs for module " + module,
        expected, actual);
  }

  @Test
  public void testPartitionInputsIntoModules()
      throws CompilationException, ModuleConfig.BadDependencyTreeException {
    // Make sure that the compiler can handle multipath on module
    // dependencies, whereby a single module has different depths
    // depending on the path. The test is with the following set
    // of dependencies (top to bottom):
    //
    //     A
    //    / \
    //   B   |
    //   |   D
    //   C   |
    //    \ /
    //     E

    File closureLibraryDirectory = new File("closure/closure-library/closure/goog/");

    final List<File> dependencies = ImmutableList.of();
    final List<File> externs = ImmutableList.of();
    final boolean customExternsOnly = false;

    // Set up a set of files so that the depenencies just go up the
    // modules tree, one file per module.
    DummyJsInput a, b, c, d, e;
    a = new DummyJsInput("a", "", ImmutableList.of("a"), null);
    b = new DummyJsInput("b", "", ImmutableList.of("b"), ImmutableList.of("a"));
    c = new DummyJsInput("c", "", ImmutableList.of("c"), ImmutableList.of("b"));
    d = new DummyJsInput("d", "", ImmutableList.of("d"), ImmutableList.of("a"));
    e = new DummyJsInput("e", "", ImmutableList.of("e"),
        ImmutableList.of("c", "d"));

    Manifest manifest = new Manifest(
        closureLibraryDirectory,
        dependencies,
        ImmutableList.<JsInput>of(a, b, c, d, e),
        externs,
        null, // builtInExterns
        soyFileOptions,
        customExternsOnly);

    ModuleConfig.Builder builder = ModuleConfig.builder(new File("."));
    builder.setModuleInfo(
        ImmutableMap.<String, ModuleConfig.ModuleInfo>builder()
        .put("a", newModule("a", "a", new String[]{}))
        .put("b", newModule("b", "b", new String[]{"a"}))
        .put("c", newModule("c", "c", new String[]{"b"}))
        .put("d", newModule("d", "d", new String[]{"a"}))
        .put("e", newModule("e", "e", new String[]{"c", "d"}))
        .build());
    ModuleConfig config = builder.build();

    Map<String, List<String>> partition =
        Maps.transformValues(config.partitionInputsIntoModules(manifest),
            INPUT_TO_NAME_LIST);

    assertEquals("Incorrect partition size", 5, partition.size());
    assertCorrectInputs(
        "a", ImmutableList.of("/base.js", "/deps.js", "a"), partition.get("a"));
    assertCorrectInputs("b", ImmutableList.of("b"), partition.get("b"));
    assertCorrectInputs("c", ImmutableList.of("c"), partition.get("c"));
    assertCorrectInputs("d", ImmutableList.of("d"), partition.get("d"));
    assertCorrectInputs("e", ImmutableList.of("e"), partition.get("e"));
  }

  @Test
  public void testPartitionInputsIntoModules2()
      throws CompilationException, ModuleConfig.BadDependencyTreeException {
    // Test that input file migration works as expected. The tree of
    // modules is as follows:
    //
    //     A
    //     |
    //     B
    //    / \
    //   C   D
    //   |  /|
    //   E / |
    //   |/  |
    //   F   G
    //
    // On top of a input file attached at each node with no other
    // depedencencies, there will be a file that is depended on by
    // both modules F and G, as well as a file that is depended on by
    // both D and E. The former should end up in module D, while the
    // latter should end up in module B.

    File closureLibraryDirectory = new File("closure/closure-library/closure/goog/");

    final List<File> dependencies = ImmutableList.of();
    final List<File> externs = ImmutableList.of();
    final boolean customExternsOnly = false;

    // These inputs are used as the "floating" inputs described above
    // that will end up getting "migrated" up the tree.
    DummyJsInput dep1, dep2;
    dep1 = new DummyJsInput("dep1", "", ImmutableList.of("dep1"), null);
    dep2 = new DummyJsInput("dep2", "", ImmutableList.of("dep2"), null);

    // Set up a set of files so that the depenencies just go up the
    // modules tree, one file per module, and additional dependencies
    // on dep1 and dep2 as described in the block comment above.
    DummyJsInput a, b, c, d, e, f, g;
    a = new DummyJsInput("a", "", ImmutableList.of("a"), null);
    b = new DummyJsInput("b", "", ImmutableList.of("b"), ImmutableList.of("a"));
    c = new DummyJsInput("c", "", ImmutableList.of("c"), ImmutableList.of("b"));
    d = new DummyJsInput("d", "", ImmutableList.of("d"),
        ImmutableList.of("b", "dep2"));
    e = new DummyJsInput("e", "", ImmutableList.of("e"),
        ImmutableList.of("c", "dep2"));
    f = new DummyJsInput("f", "", ImmutableList.of("f"),
        ImmutableList.of("d", "e", "dep1"));
    g = new DummyJsInput("g", "", ImmutableList.of("g"),
        ImmutableList.of("d", "dep1"));

    Manifest manifest = new Manifest(
        closureLibraryDirectory,
        dependencies,
        ImmutableList.<JsInput>of(a, b, c, d, e, f, g, dep1, dep2),
        externs,
        null, // builtInExterns
        soyFileOptions,
        customExternsOnly);

    ModuleConfig.Builder builder = ModuleConfig.builder(new File("."));
    builder.setModuleInfo(
        ImmutableMap.<String, ModuleConfig.ModuleInfo>builder()
        .put("a", newModule("a", "a", new String[]{}))
        .put("b", newModule("b", "b", new String[]{"a"}))
        .put("c", newModule("c", "c", new String[]{"b"}))
        .put("d", newModule("d", "d", new String[]{"b"}))
        .put("e", newModule("e", "e", new String[]{"c"}))
        .put("f", newModule("f", "f", new String[]{"d", "e"}))
        .put("g", newModule("g", "g", new String[]{"d"}))
        .build());
    ModuleConfig config = builder.build();

    Map<String, List<String>> partition =
        Maps.transformValues(config.partitionInputsIntoModules(manifest),
            INPUT_TO_NAME_LIST);

    assertEquals("Incorrect partition size", 7, partition.size());
    assertCorrectInputs(
        "a", ImmutableList.of("/base.js", "/deps.js", "a"), partition.get("a"));
    assertCorrectInputs("b", ImmutableList.of("b", "dep2"), partition.get("b"));
    assertCorrectInputs("c", ImmutableList.of("c"), partition.get("c"));
    assertCorrectInputs("d", ImmutableList.of("d", "dep1"), partition.get("d"));
    assertCorrectInputs("e", ImmutableList.of("e"), partition.get("e"));
    assertCorrectInputs("f", ImmutableList.of("f"), partition.get("f"));
    assertCorrectInputs("g", ImmutableList.of("g"), partition.get("g"));
  }

  @Test
  public void testPartitionInputsIntoModules3()
      throws CompilationException, ModuleConfig.BadDependencyTreeException {
    // Test that input file migration works as expected. The tree of
    // modules is as follows:
    //
    //       A
    //     /   \
    //    B     C
    //    | \ / |
    //    | / \ |
    //    D     E
    //
    // An input file depended on by C, D, and E should migrate up to C.

    File closureLibraryDirectory = new File("closure/closure-library/closure/goog/");

    final List<File> dependencies = ImmutableList.of();
    final List<File> externs = ImmutableList.of();
    final boolean customExternsOnly = false;

    // This input is used as the "floating" input described above
    // that will end up getting "migrated" up the tree.
    DummyJsInput dep1 =
        new DummyJsInput("dep1", "", ImmutableList.of("dep1"), null);

    // Set up a set of files so that the depenencies just go up the
    // modules tree, one file per module, and additional dependencies
    // on dep1 and dep2 as described in the block comment above.
    DummyJsInput a, b, c, d, e;
    a = new DummyJsInput("a", "", ImmutableList.of("a"), null);
    b = new DummyJsInput("b", "", ImmutableList.of("b"), ImmutableList.of("a"));
    c = new DummyJsInput("c", "", ImmutableList.of("c"),
        ImmutableList.of("a", "dep1"));
    d = new DummyJsInput("d", "", ImmutableList.of("d"),
        ImmutableList.of("b", "c", "dep1"));
    e = new DummyJsInput("e", "", ImmutableList.of("e"),
        ImmutableList.of("b", "c", "dep1"));

    Manifest manifest = new Manifest(
        closureLibraryDirectory,
        dependencies,
        ImmutableList.<JsInput>of(a, b, c, d, e, dep1),
        externs,
        null, // builtInExterns
        soyFileOptions,
        customExternsOnly);

    ModuleConfig.Builder builder = ModuleConfig.builder(new File("."));
    builder.setModuleInfo(
        ImmutableMap.<String, ModuleConfig.ModuleInfo>builder()
        .put("a", newModule("a", "a", new String[]{}))
        .put("b", newModule("b", "b", new String[]{"a"}))
        .put("c", newModule("c", "c", new String[]{"a"}))
        .put("d", newModule("d", "d", new String[]{"b", "c"}))
        .put("e", newModule("e", "e", new String[]{"b", "c"}))
        .build());
    ModuleConfig config = builder.build();

    Map<String, List<String>> partition =
        Maps.transformValues(config.partitionInputsIntoModules(manifest),
            INPUT_TO_NAME_LIST);

    assertEquals("Incorrect partition size", 5, partition.size());
    assertCorrectInputs(
        "a", ImmutableList.of("/base.js", "/deps.js", "a"), partition.get("a"));
    assertCorrectInputs("b", ImmutableList.of("b"), partition.get("b"));
    assertCorrectInputs("c", ImmutableList.of("dep1", "c"), partition.get("c"));
    assertCorrectInputs("d", ImmutableList.of("d"), partition.get("d"));
    assertCorrectInputs("e", ImmutableList.of("e"), partition.get("e"));
  }
}
