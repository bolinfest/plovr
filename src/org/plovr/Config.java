package org.plovr;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.ClosureCodingConvention;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.WarningLevel;

final class Config {

  private static final Logger logger = Logger.getLogger("org.plovr.Config");

  private final String id;

  private final Manifest manifest;

  private final CompilationMode compilationMode;

  private final WarningLevel warningLevel;

  private final boolean printInputDelimiter;

  /**
   * @param id Unique identifier for the configuration. This is used as an
   *        argument to the &lt;script> tag that loads the compiled code.
   * @param manifest
   * @param compilationMode
   */
  private Config(
      String id,
      Manifest manifest,
      CompilationMode compilationMode,
      WarningLevel warningLevel,
      boolean printInputDelimiter) {
    this.id = id;
    this.manifest = manifest;
    this.compilationMode = compilationMode;
    this.warningLevel = warningLevel;
    this.printInputDelimiter = printInputDelimiter;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static Builder builder(Config config) {
    return new Builder(config);
  }

  public String getId() {
    return id;
  }

  public Manifest getManifest() {
    return manifest;
  }

  public CompilationMode getCompilationMode() {
    return compilationMode;
  }

  public CompilerOptions getCompilerOptions() {
    CompilationLevel level = compilationMode.getCompilationLevel();
    logger.info("Compiling with level: " + level);
    CompilerOptions options = new CompilerOptions();
    level.setOptionsForCompilationLevel(options);
    options.setCodingConvention(new ClosureCodingConvention());
    warningLevel.setOptionsForWarningLevel(options);
    options.printInputDelimiter = printInputDelimiter;
    if (printInputDelimiter) {
      options.inputDelimiter = "// Input %num%: %name%";
    }

    // TODO(bolinfest): This is a hack to work around the fact that a SourceMap
    // will not be created unless a file is specified to which the SourceMap
    // should be written.
    try {
      File tempFile = File.createTempFile("source", "map");
      options.sourceMapOutputPath = tempFile.getAbsolutePath();
    } catch (IOException e) {
      logger.severe("A temp file for the Source Map could not be created");
    }

    options.enableExternExports(true);

    return options;
  }

  @Override
  public String toString() {
    return id;
  }

  final static class Builder {

    private String id = null;

    private final Manifest manifest;

    private String pathToClosureLibrary = null;

    private ImmutableList.Builder<String> paths = ImmutableList.builder();

    private ImmutableList.Builder<String> inputs = ImmutableList.builder();

    private ImmutableList.Builder<String> externs = null;

    private CompilationMode compilationMode = CompilationMode.SIMPLE;

    private WarningLevel warningLevel = WarningLevel.DEFAULT;

    private boolean printInputDelimiter = false;

    private Builder() {
      manifest = null;
    }

    private Builder(Config config) {
      Preconditions.checkNotNull(config);
      this.id = config.id;
      this.manifest = config.manifest;
      this.compilationMode = config.compilationMode;
      this.warningLevel = config.warningLevel;
      this.printInputDelimiter = config.printInputDelimiter;
    }

    public void setId(String id) {
      Preconditions.checkNotNull(id);
      this.id = id;
    }

    public void addPath(String path) {
      Preconditions.checkNotNull(path);
      paths.add(path);
    }

    public void addInput(String input) {
      Preconditions.checkNotNull(input);
      inputs.add(input);
    }

    public void addExtern(String extern) {
      if (externs == null) {
        externs = ImmutableList.builder();
      }
      externs.add(extern);
    }
    public void setPathToClosureLibrary(String pathToClosureLibrary) {
      this.pathToClosureLibrary = pathToClosureLibrary;
    }

    public void setCompilationMode(CompilationMode mode) {
      Preconditions.checkNotNull(mode);
      this.compilationMode = mode;
    }

    public void setWarningLevel(WarningLevel level) {
      Preconditions.checkNotNull(level);
      this.warningLevel = level;
    }

    public void setPrintInputDelimiter(boolean printInputDelimiter) {
      this.printInputDelimiter = printInputDelimiter;
    }

    public Config build() {
      File closureLibraryDirectory = pathToClosureLibrary != null
          ? new File(pathToClosureLibrary)
          : null;

      Manifest manifest;
      if (this.manifest == null) {
        List<File> externs = this.externs == null ? null
            : Lists.transform(this.externs.build(), STRING_TO_FILE);

        manifest = new Manifest(closureLibraryDirectory,
          Lists.transform(paths.build(), STRING_TO_FILE),
          Lists.transform(inputs.build(), STRING_TO_JS_INPUT),
          externs);
      } else {
        manifest = this.manifest;
      }

      Config config = new Config(
          id,
          manifest,
          compilationMode,
          warningLevel,
          printInputDelimiter);

      return config;
    }

  }
  private static Function<String, File> STRING_TO_FILE =
    new Function<String, File>() {
      @Override
      public File apply(String s) {
        return new File(s);
      }
    };

  private static Function<String, JsInput> STRING_TO_JS_INPUT =
    new Function<String, JsInput>() {
      @Override
      public JsInput apply(String fileName) {
        return LocalFileJsInput.createForName(fileName);
      }
    };

}
