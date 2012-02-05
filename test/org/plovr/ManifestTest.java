package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.JSSourceFile;

/**
 * {@link ManifestTest} is a unit test for {@link Manifest}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class ManifestTest {

  /** Converts a {@link JSSourceFile} to its name. */
  private static Function<JSSourceFile, String> JS_SOURCE_FILE_TO_NAME =
    new Function<JSSourceFile, String>() {
      @Override
      public String apply(JSSourceFile jsSourceFile) {
        return jsSourceFile.getName();
      }
  };

  /** Converts a {@link JsInput} to its name. */
  private static Function<JsInput, String> JS_INPUT_TO_NAME =
    new Function<JsInput, String>() {
      @Override
      public String apply(JsInput jsInput) {
        return jsInput.getName();
      }
  };

  private final SoyFileOptions soyFileOptions = new SoyFileOptions();

  @Test
  public void testSimpleManifest() throws CompilationException {
    File closureLibraryDirectory = new File("closure/closure-library/closure/goog/");

    final List<File> dependencies = ImmutableList.of();

    String path = "test/org/plovr/example.js";
    File testFile = new File(path);
    JsSourceFile requiredInput = new JsSourceFile(path, testFile);

    final List<File> externs = ImmutableList.of();
    final boolean customExternsOnly = false;

    Manifest manifest = new Manifest(
        closureLibraryDirectory,
        dependencies,
        ImmutableList.<JsInput>of(requiredInput),
        externs,
        null, // builtInExterns
        soyFileOptions,
        customExternsOnly);

    final ModuleConfig moduleConfig = null;
    Compilation compilerArguments = manifest.getCompilerArguments(moduleConfig);
    List<JSSourceFile> inputs = compilerArguments.getInputs();

    List<String> expectedNames = ImmutableList.copyOf(
        new String[] {
            "/base.js",
            "/deps.js",
            "/debug/error.js",
            "/string/string.js",
            "/asserts/asserts.js",
            "/array/array.js",
            "/debug/entrypointregistry.js",
            "/debug/errorhandlerweakdep.js",
            "/useragent/useragent.js",
            "/events/browserfeature.js",
            "/disposable/idisposable.js",
            "/disposable/disposable.js",
            "/events/event.js",
            "/events/eventtype.js",
            "/reflect/reflect.js",
            "/events/browserevent.js",
            "/events/eventwrapper.js",
            "/events/listener.js",
            "/object/object.js",
            "/events/events.js",
            "test/org/plovr/example.js"
        }
        );
    assertEquals(expectedNames, Lists.transform(inputs, JS_SOURCE_FILE_TO_NAME));
  }

  @Test
  public void testCompilationOrder() throws CompilationException {
    File closureLibraryDirectory = new File("closure/closure-library/closure/goog/");

    final List<File> dependencies = ImmutableList.of();
    final List<File> externs = ImmutableList.of();
    final boolean customExternsOnly = false;

    // Set up a set of files so that there's a dependency loop of
    // a -> b -> c -> a
    DummyJsInput a, b, c;
    a = new DummyJsInput("a", "", ImmutableList.of("a"), ImmutableList.of("b"));
    b = new DummyJsInput("b", "", ImmutableList.of("b"), ImmutableList.of("c"));
    c = new DummyJsInput("c", "", ImmutableList.of("c"), ImmutableList.of("a"));

    Manifest manifest = new Manifest(
        closureLibraryDirectory,
        dependencies,
        ImmutableList.<JsInput>of(a, b, c),
        externs,
        null, // builtInExterns
        soyFileOptions,
        customExternsOnly);

    List<JsInput> order;

    try {
      order = manifest.getInputsInCompilationOrder();
      fail("Got order for unorderable inputs: " + order);
    } catch (CircularDependencyException e) {
      Collection<JsInput> circularDepdenency = e.getCircularDependency();
      assertEquals(ImmutableList.copyOf(circularDepdenency),
          ImmutableList.of(a, b, c));
    }

    // Now adjust c so that it no longer creates a loop
    c = new DummyJsInput("c", "", ImmutableList.of("c"), null);

    manifest = new Manifest(
        closureLibraryDirectory,
        dependencies,
        ImmutableList.<JsInput>of(a, b, c),
        externs,
        null, // builtInExterns
        soyFileOptions,
        customExternsOnly);

    order = manifest.getInputsInCompilationOrder();

    List<String> expectedNames = ImmutableList.of("/base.js",
        "/deps.js", "c", "b", "a");
    assertEquals(expectedNames, Lists.transform(order, JS_INPUT_TO_NAME));
  }

  @Test
  public void testGetAllDependencies() {
    testGetAllDependenciesContainsBaseJs(null /* closureLibraryDirectory */,
        "/closure/goog/base.js");

    testGetAllDependenciesContainsBaseJs(new File("testdata/manifest/"),
        "/base.js");
  }

  private void testGetAllDependenciesContainsBaseJs(
      @Nullable File closureLibraryDirectory, final String baseJsName) {
    DummyJsInput input = new DummyJsInput(
        "dummy.js",
        "// Sample code\n",
        ImmutableList.of("test.dummy"),
        ImmutableList.of("goog.string"));

    Manifest manifest = new Manifest(
        closureLibraryDirectory,
        ImmutableList.<File>of() /* dependencies */,
        ImmutableList.<JsInput>of(input) /* requiredInputs */,
        null /* externs */,
        null /* builtInExterns */,
        new SoyFileOptions(),
        false /* customExternsOnly */);
    Set<JsInput> dependencies = manifest.getAllDependencies(
        false /* cacheDependencies */);

    JsInput valueIfNotFound = null;
    JsInput foundValue = Iterables.find(dependencies, new Predicate<JsInput>() {
      @Override
      public boolean apply(JsInput dep) {
        return baseJsName.equals(dep.getName());
      }
    }, valueIfNotFound);
    assertNotNull("Dependencies for Closure Library should contain " +
        baseJsName, foundValue);
  }
}
