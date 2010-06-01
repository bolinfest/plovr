package org.plovr;

import java.util.logging.Logger;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;

public class Config {

  private static final Logger logger = Logger.getLogger("org.plovr.Config");

  private final String id;

  private final Manifest manifest;
  
  private final CompilationLevel compilationLevel;

  /**
   * @param id Unique identifier for the configuration. This is used as an
   *        argument to the &lt;script> tag that loads the compiled code.
   * @param manifest
   * @param compilationLevel
   */
  public Config(String id, Manifest manifest, CompilationLevel compilationLevel) {
    this.id = id;
    this.manifest = manifest;
    this.compilationLevel = compilationLevel;
  }

  public String getId() {
    return id;
  }

  public Manifest getManifest() {
    return manifest;
  }

  public CompilerOptions getCompilerOptions() {
    logger.info("Compiling with level: " + compilationLevel);
    CompilerOptions options = new CompilerOptions();
    compilationLevel.setOptionsForCompilationLevel(options);
    return options;
  }

  @Override
  public String toString() {
    return id;
  }

}
