package org.plovr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;

import org.plovr.io.Settings;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.LineReader;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.SourceFile.Generator;

class ResourceReader {

  private static final Logger logger = Logger.getLogger("org.plovr.ResourceReader");

  private ResourceReader() {}

  /**
   * @return the base.js file for the Closure Library
   */
  static JsInput getBaseJs() {
    return BaseJsHolder.instance.baseJs;
  }

  /** Lazy-loaded singleton pattern for base.js in the Closure Library. */
  private static class BaseJsHolder {
    private static final BaseJsHolder instance = new BaseJsHolder();

    final JsInput baseJs;

    private BaseJsHolder() {
      this.baseJs = new ResourceJsInput("/closure/goog/base.js");
    }
  }

  /**
   * @return a JsInput for each source in the Closure Library
   */
  static List<JsInput> getClosureLibrarySources() {
    return ClosureLibraryHolder.instance.inputs;
  }

  private static final Function<String, JsInput> INPUT_TO_JS_INPUT =
    new Function<String, JsInput>() {
      @Override
      public JsInput apply(String path) {
        return new ResourceJsInput(path);
      }
  };

  private static class ClosureLibraryHolder {
    private static final ClosureLibraryHolder instance = new ClosureLibraryHolder();

    private final List<JsInput> inputs;

    private ClosureLibraryHolder() {
      try {
        List<JsInput> allInputs = Lists.newLinkedList();
        allInputs.addAll(loadFromManifest("/library_manifest.txt",
            "/closure/goog/", INPUT_TO_JS_INPUT));
        allInputs.addAll(loadFromManifest("/third_party_manifest.txt",
            "/third_party/closure/goog/", INPUT_TO_JS_INPUT));
        allInputs.add(new ResourceJsInput("/soy/soyutils_usegoog.js"));
        allInputs.add(new ResourceJsInput("/soy/soyutils.js"));

        inputs = ImmutableList.copyOf(allInputs);
      } catch (IOException e) {
        logger.severe(e.getMessage());
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * @return a JSSourceFile for each externs file bundled with plovr
   */
  static List<JSSourceFile> getDefaultExterns() {
    return ExternsHolder.instance.externs;
  }

  private static final Function<String, JSSourceFile> INPUT_TO_JS_SOURCE_FILE =
    new Function<String, JSSourceFile>() {
      @Override
      public JSSourceFile apply(String path) {
        Generator generator = new ResourceJsInput(path);
        return JSSourceFile.fromGenerator(path, generator);
      }
  };

  private static class ExternsHolder {
    private static final ExternsHolder instance = new ExternsHolder();

    final List<JSSourceFile> externs;

    private ExternsHolder() {
      List<JSSourceFile> externs;
      try {
        externs = loadExternsFromManifest();
      } catch (IOException e) {
        logger.severe(e.getMessage());
        throw new RuntimeException(e);
      }
      this.externs = externs;
    }

    static List<JSSourceFile> loadExternsFromManifest() throws IOException {
      return loadFromManifest("/externs_manifest.txt", "/externs/",
          INPUT_TO_JS_SOURCE_FILE);
    }
  }

  /**
   * Loads a list of resources from a jar file using a manifest file which is
   * also in the jar file.
   * @param manifestFile Absolute path to resource in the jar that lists the
   *        names of the other resources to load
   * @param prefix that should be added to each line in manifestFile to create
   *        the full path to an individual resource
   * @param f Function that takes a complete path to a resource in the jar and
   *        produces a type T
   * @return a list of T produced by f
   * @throws IOException
   */
  private static <T> List<T> loadFromManifest(String manifestFile, String prefix,
      Function<String,T> f) throws IOException {
    InputStream input = ResourceReader.class.getResourceAsStream(
        manifestFile);
    Readable readable = new InputStreamReader(input, Settings.CHARSET);
    LineReader lineReader = new LineReader(readable);

    List<T> results = Lists.newLinkedList();
    String line;
    while ((line = lineReader.readLine()) != null) {
      results.add(f.apply(prefix + line));
    }
    return ImmutableList.copyOf(results);
  }
}
