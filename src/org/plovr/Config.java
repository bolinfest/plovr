package org.plovr;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonPrimitive;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.ClosureCodingConvention;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.WarningLevel;

public final class Config {

  private static final Logger logger = Logger.getLogger("org.plovr.Config");

  private final String id;

  private final Manifest manifest;

  private final ModuleConfig moduleConfig;

  private final CompilationMode compilationMode;

  private final WarningLevel warningLevel;

  private final boolean printInputDelimiter;

  private final boolean fingerprintJsFiles;

  private final Map<DiagnosticGroup, CheckLevel> diagnosticGroups;

  private final Map<String, JsonPrimitive> defines;

  /**
   * @param id Unique identifier for the configuration. This is used as an
   *        argument to the &lt;script> tag that loads the compiled code.
   * @param manifest
   * @param compilationMode
   */
  private Config(
      String id,
      Manifest manifest,
      ModuleConfig moduleConfig,
      CompilationMode compilationMode,
      WarningLevel warningLevel,
      boolean printInputDelimiter,
      boolean fingerprintJsFiles,
      Map<DiagnosticGroup, CheckLevel> diagnosticGroups,
      Map<String, JsonPrimitive> defines) {
    Preconditions.checkNotNull(defines);

    this.id = id;
    this.manifest = manifest;
    this.moduleConfig = moduleConfig;
    this.compilationMode = compilationMode;
    this.warningLevel = warningLevel;
    this.printInputDelimiter = printInputDelimiter;
    this.fingerprintJsFiles = fingerprintJsFiles;
    this.diagnosticGroups = diagnosticGroups;
    this.defines = ImmutableMap.copyOf(defines);
  }

  public static Builder builder(File relativePathBase) {
    return new Builder(relativePathBase);
  }

  public static Builder builder(Config config) {
    return new Builder(config);
  }

  /**
   * Create a builder that can be used for testing. Paths will be resolved
   * against the root folder of the system.
   */
  @VisibleForTesting
  public static Builder builderForTesting() {
    File rootDirectory = File.listRoots()[0];
    return new Builder(rootDirectory);
  }

  public String getId() {
    return id;
  }

  public Manifest getManifest() {
    return manifest;
  }

  public ModuleConfig getModuleConfig() {
    return moduleConfig;
  }

  public CompilationMode getCompilationMode() {
    return compilationMode;
  }

  public WarningLevel getWarningLevel() {
    return warningLevel;
  }

  public boolean shouldFingerprintJsFiles() {
    return fingerprintJsFiles;
  }

  public CompilerOptions getCompilerOptions() {
    Preconditions.checkArgument(compilationMode != CompilationMode.RAW,
        "Cannot compile using RAW mode");
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

    // Apply this.defines.
    for (Map.Entry<String, JsonPrimitive> entry : defines.entrySet()) {
      String name = entry.getKey();
      JsonPrimitive primitive = entry.getValue();
      if (primitive.isBoolean()) {
        options.setDefineToBooleanLiteral(name, primitive.getAsBoolean());
      } else if (primitive.isString()) {
        options.setDefineToStringLiteral(name, primitive.getAsString());
      } else if (primitive.isNumber()) {
        Number num = primitive.getAsNumber();
        double value = num.doubleValue();
        // Heuristic to determine whether the value is an int.
        if (value == Math.floor(value)) {
          options.setDefineToNumberLiteral(name, primitive.getAsInt());
        } else {
          options.setDefineToDoubleLiteral(name, primitive.getAsDouble());
        }
      }
    }

    if (moduleConfig != null) {
      options.crossModuleCodeMotion = true;
      options.crossModuleMethodMotion = true;
    }

    if (diagnosticGroups != null) {
      for (Map.Entry<DiagnosticGroup, CheckLevel> entry :
          diagnosticGroups.entrySet()) {
        DiagnosticGroup group = entry.getKey();
        CheckLevel checkLevel = entry.getValue();
        options.setWarningLevel(group, checkLevel);
      }
    }

    // This is a hack to work around the fact that a SourceMap
    // will not be created unless a file is specified to which the SourceMap
    // should be written.
    // TODO(bolinfest): Change com.google.javascript.jscomp.CompilerOptions so
    // that this is configured by a boolean, just like enableExternExports() was
    // added to support generating externs without writing them to a file.
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

    private final File relativePathBase;

    private String id = null;

    private final Manifest manifest;

    private String pathToClosureLibrary = null;

    private ImmutableList.Builder<String> paths = ImmutableList.builder();

    private ImmutableList.Builder<JsInput> inputs = ImmutableList.builder();

    private ImmutableList.Builder<String> externs = null;

    private boolean customExternsOnly = false;

    private CompilationMode compilationMode = CompilationMode.SIMPLE;

    private WarningLevel warningLevel = WarningLevel.DEFAULT;

    private boolean printInputDelimiter = false;

    private boolean fingerprintJsFiles = false;

    private Map<DiagnosticGroup, CheckLevel> diagnosticGroups = null;

    private ModuleConfig.Builder moduleConfigBuilder = null;

    private final Map<String, JsonPrimitive> defines;

    /**
     * Pattern to validate a config id. A config id may not contain funny
     * characters, such as slashes, because ids are used in RESTful URLs, so
     * such characters would make proper URL parsing difficult.
     */
    private static final Pattern ID_PATTERN = Pattern.compile("\\w+");

    private Builder(File relativePathBase) {
      Preconditions.checkNotNull(relativePathBase);
      Preconditions.checkArgument(relativePathBase.isDirectory(),
          relativePathBase + " is not a directory");
      this.relativePathBase = relativePathBase;
      manifest = null;
      defines = Maps.newHashMap();
    }

    private Builder(Config config) {
      Preconditions.checkNotNull(config);
      this.relativePathBase = null;
      this.id = config.id;
      this.manifest = config.manifest;
      this.moduleConfigBuilder = (config.moduleConfig == null)
          ? null
          : ModuleConfig.builder(config.moduleConfig);
      this.compilationMode = config.compilationMode;
      this.warningLevel = config.warningLevel;
      this.printInputDelimiter = config.printInputDelimiter;
      this.fingerprintJsFiles = config.fingerprintJsFiles;
      this.diagnosticGroups = config.diagnosticGroups;
      this.defines = Maps.newHashMap(config.defines);
    }

    /** Directory against which relative paths should be resolved. */
    public File getRelativePathBase() {
      return this.relativePathBase;
    }

    public void setId(String id) {
      Preconditions.checkNotNull(id);
      Preconditions.checkArgument(ID_PATTERN.matcher(id).matches(),
          String.format("Not a valid config id: %s", id));
      this.id = id;
    }

    public void addPath(String path) {
      Preconditions.checkNotNull(path);
      paths.add(path);
    }

    public void addInput(File file, String name) {
      Preconditions.checkNotNull(file);
      Preconditions.checkNotNull(name);
      inputs.add(LocalFileJsInput.createForFileWithName(file, name));
    }

    public void addExtern(String extern) {
      if (externs == null) {
        externs = ImmutableList.builder();
      }
      externs.add(extern);
    }

    public void setCustomExternsOnly(boolean customExternsOnly) {
      this.customExternsOnly = customExternsOnly;
    }

    public void setPathToClosureLibrary(String pathToClosureLibrary) {
      this.pathToClosureLibrary = pathToClosureLibrary;
    }

    public ModuleConfig.Builder getModuleConfigBuilder() {
      if (moduleConfigBuilder == null) {
        moduleConfigBuilder = ModuleConfig.builder(relativePathBase);
      }
      return moduleConfigBuilder;
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

    public void setFingerprintJsFiles(boolean fingerprint) {
      this.fingerprintJsFiles = fingerprint;
    }

    public void setDiagnosticGroups(Map<DiagnosticGroup, CheckLevel> groups) {
      this.diagnosticGroups = groups;
    }

    public void addDefine(String name, JsonPrimitive primitive) {
      defines.put(name, primitive);
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
          inputs.build(),
          externs,
          customExternsOnly);
      } else {
        manifest = this.manifest;
      }

      ModuleConfig moduleConfig = (moduleConfigBuilder == null)
          ? null
          : moduleConfigBuilder.build();

      Config config = new Config(
          id,
          manifest,
          moduleConfig,
          compilationMode,
          warningLevel,
          printInputDelimiter,
          fingerprintJsFiles,
          diagnosticGroups,
          defines);

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

}
