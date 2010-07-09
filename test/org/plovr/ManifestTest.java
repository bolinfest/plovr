package org.plovr;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.JSSourceFile;

/**
 * {@link ManifestTest} is a unit test for {@link Manifest}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class ManifestTest extends TestCase {

  /** Converts a {@link JSSourceFile} to its name. */
  private static Function<JSSourceFile, String> JS_SOURCE_FILE_TO_NAME =
    new Function<JSSourceFile, String>() {
      @Override
      public String apply(JSSourceFile jsSourceFile) {
        return jsSourceFile.getName();
      }
  };

  public void testSimpleManifest() throws MissingProvideException {
    File closureLibraryDirectory = new File("../closure-library/closure/goog/");

    final List<File> dependencies = ImmutableList.of();

    String path = "test/org/plovr/example.js";
    File testFile = new File(path);
    JsSourceFile requiredInput = new JsSourceFile(path, testFile);

    final List<File> externs = ImmutableList.of();

    Manifest manifest = new Manifest(
        closureLibraryDirectory,
        dependencies,
        ImmutableList.<JsInput>of(requiredInput),
        externs);

    CompilerArguments compilerArguments = manifest.getCompilerArguments();
    List<JSSourceFile> inputs = compilerArguments.getInputs();

    List<String> expectedNames = ImmutableList.copyOf(
        new String[] {
            "base.js",
            "/goog/debug/error.js",
            "/goog/string/string.js",
            "/goog/asserts/asserts.js",
            "/goog/array/array.js",
            "/goog/debug/errorhandlerweakdep.js",
            "/goog/disposable/disposable.js",
            "/goog/events/event.js",
            "/goog/useragent/useragent.js",
            "/goog/events/browserevent.js",
            "/goog/events/eventwrapper.js",
            "/goog/events/listener.js",
            "/goog/structs/simplepool.js",
            "/goog/useragent/jscript.js",
            "/goog/events/pools.js",
            "/goog/object/object.js",
            "/goog/events/events.js",
            "test/org/plovr/example.js"
        }
        );
    assertEquals(expectedNames, Lists.transform(inputs, JS_SOURCE_FILE_TO_NAME));
  }
}
