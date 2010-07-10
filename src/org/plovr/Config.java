package org.plovr;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.javascript.jscomp.ClosureCodingConvention;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.SourceMap;
import com.google.javascript.jscomp.WarningLevel;

final class Config {

  private static final Logger logger = Logger.getLogger("org.plovr.Config");

  private final String id;

  private final Manifest manifest;

  private final boolean useExplicitQueryParameters;

  private CompilationMode compilationMode;

  private SourceMap sourceMap;

  private String exportsAsExterns;

  /**
   * @param id Unique identifier for the configuration. This is used as an
   *        argument to the &lt;script> tag that loads the compiled code.
   * @param manifest
   * @param compilationLevel
   */
  public Config(
      String id,
      Manifest manifest,
      boolean useExplicitQueryParameters) {
    this.id = id;
    this.manifest = manifest;
    this.useExplicitQueryParameters = useExplicitQueryParameters;
    this.compilationMode = CompilationMode.SIMPLE;
  }

  public Config(Config config) {
    this.id = config.id;
    this.manifest = config.manifest;
    this.useExplicitQueryParameters = config.useExplicitQueryParameters;
    this.compilationMode = config.compilationMode;
  }

  public String getId() {
    return id;
  }

  public Manifest getManifest() {
    return manifest;
  }

  public boolean isUseExplicitQueryParameters() {
    return useExplicitQueryParameters;
  }

  public CompilationMode getCompilationMode() {
    return compilationMode;
  }

  public void setCompilationMode(CompilationMode compilationMode) {
    this.compilationMode = compilationMode;
  }

  public CompilerOptions getCompilerOptions() {
    CompilationLevel level = compilationMode.getCompilationLevel();
    logger.info("Compiling with level: " + level);
    CompilerOptions options = new CompilerOptions();
    level.setOptionsForCompilationLevel(options);
    options.setCodingConvention(new ClosureCodingConvention());

    // TODO(bolinfest): Make this configurable
    WarningLevel.VERBOSE.setOptionsForWarningLevel(options);

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

  public SourceMap getSourceMapFromLastCompilation() {
    return sourceMap;
  }

  public void setSourceMapFromLastCompilation(SourceMap sourceMap) {
    this.sourceMap = sourceMap;
  }

  public String getExportsAsExterns() {
    return exportsAsExterns;
  }

  public void setExportsAsExterns(String exportsAsExterns) {
    this.exportsAsExterns = exportsAsExterns;
  }

  @Override
  public String toString() {
    return id;
  }

}
