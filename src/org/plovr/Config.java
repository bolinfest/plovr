package org.plovr;

import java.util.logging.Logger;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;

public class Config {

  private static final Logger logger = Logger.getLogger("org.plovr.Config");

  private final String id;

  private final Manifest manifest;

  private final boolean useExplicitQueryParameters;

  private CompilationMode compilationMode;

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
    return options;
  }

  @Override
  public String toString() {
    return id;
  }

}
