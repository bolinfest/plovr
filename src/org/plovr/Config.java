package org.plovr;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonPrimitive;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.ClosureCodingConvention;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.WarningLevel;

public final class Config implements Comparable<Config> {

  private static final Logger logger = Logger.getLogger("org.plovr.Config");

  private final String id;

  /**
   * The content of the config file used to create this {@link Config}.
   * Once "config inheritance" is supported, this is going to be a little
   * more complicated.
   */
  private final String rootConfigFileContent;

  private final Manifest manifest;

  @Nullable
  private final ModuleConfig moduleConfig;

  private final CompilationMode compilationMode;

  private final WarningLevel warningLevel;

  private final boolean debug;

  private final boolean prettyPrint;

  private final boolean printInputDelimiter;

  private final String outputWrapper;

  private final Charset outputCharset;

  private final boolean fingerprintJsFiles;

  private final Map<DiagnosticGroup, CheckLevel> diagnosticGroups;

  private final boolean treatWarningsAsErrors;

  private final Map<String, JsonPrimitive> defines;

  private final Set<String> stripNameSuffixes;

  private final Set<String> stripTypePrefixes;

  private final Set<String> idGenerators;

  private final boolean ambiguateProperties;

  private final boolean disambiguateProperties;

  /**
   * @param id Unique identifier for the configuration. This is used as an
   *        argument to the &lt;script> tag that loads the compiled code.
   * @param manifest
   * @param compilationMode
   */
  private Config(
      String id,
      String rootConfigFileContent,
      Manifest manifest,
      @Nullable ModuleConfig moduleConfig,
      CompilationMode compilationMode,
      WarningLevel warningLevel,
      boolean debug,
      boolean prettyPrint,
      boolean printInputDelimiter,
      @Nullable String outputWrapper,
      Charset outputCharset,
      boolean fingerprintJsFiles,
      Map<DiagnosticGroup, CheckLevel> diagnosticGroups,
      boolean treatWarningsAsErrors,
      Map<String, JsonPrimitive> defines,
      Set<String> stripNameSuffixes,
      Set<String> stripTypePrefixes,
      Set<String> idGenerators,
      boolean ambiguateProperties,
      boolean disambiguateProperties) {
    Preconditions.checkNotNull(defines);

    this.id = id;
    this.rootConfigFileContent = rootConfigFileContent;
    this.manifest = manifest;
    this.moduleConfig = moduleConfig;
    this.compilationMode = compilationMode;
    this.warningLevel = warningLevel;
    this.debug = debug;
    this.prettyPrint = prettyPrint;
    this.printInputDelimiter = printInputDelimiter;
    this.outputWrapper = outputWrapper;
    this.outputCharset = outputCharset;
    this.fingerprintJsFiles = fingerprintJsFiles;
    this.diagnosticGroups = diagnosticGroups;
    this.treatWarningsAsErrors = treatWarningsAsErrors;
    this.defines = ImmutableMap.copyOf(defines);
    this.stripNameSuffixes = ImmutableSet.copyOf(stripNameSuffixes);
    this.stripTypePrefixes = ImmutableSet.copyOf(stripTypePrefixes);
    this.idGenerators = ImmutableSet.copyOf(idGenerators);
    this.ambiguateProperties = ambiguateProperties;
    this.disambiguateProperties = disambiguateProperties;
  }

  public static Builder builder(File relativePathBase,
      String rootConfigFileContent) {
    return new Builder(relativePathBase, rootConfigFileContent);
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
    return new Builder(rootDirectory, "");
  }

  public String getId() {
    return id;
  }

  public String getRootConfigFileContent() {
    return rootConfigFileContent;
  }

  public Manifest getManifest() {
    return manifest;
  }

  public ModuleConfig getModuleConfig() {
    return moduleConfig;
  }

  public boolean hasModules() {
    return moduleConfig != null;
  }

  public CompilationMode getCompilationMode() {
    return compilationMode;
  }

  public WarningLevel getWarningLevel() {
    return warningLevel;
  }

  /**
   * @return null if no output wrapper has been set
   */
  public String getOutputWrapper() {
    return outputWrapper;
  }

  public Charset getOutputCharset() {
    return outputCharset;
  }

  /**
   * The value of the Content-Type header to use when writing JavaScript content
   * in response to an HTTP request.
   */
  public String getJsContentType() {
    return "text/javascript; charset=" + outputCharset.name();
  }

  /**
   * @return null if no output wrapper has been set
   */
  public String getOutputWrapperMarker() {
    return "%output%";
  }

  public boolean shouldFingerprintJsFiles() {
    return fingerprintJsFiles;
  }

  public boolean getTreatWarningsAsErrors() {
    return treatWarningsAsErrors;
  }

  public CompilerOptions getCompilerOptions() {
    Preconditions.checkArgument(compilationMode != CompilationMode.RAW,
        "Cannot compile using RAW mode");
    CompilationLevel level = compilationMode.getCompilationLevel();
    logger.info("Compiling with level: " + level);
    CompilerOptions options = new CompilerOptions();
    level.setOptionsForCompilationLevel(options);
    if (debug) {
      level.setDebugOptionsForCompilationLevel(options);
    }
    options.setCodingConvention(new ClosureCodingConvention());
    warningLevel.setOptionsForWarningLevel(options);
    options.prettyPrint = prettyPrint;
    options.printInputDelimiter = printInputDelimiter;
    if (printInputDelimiter) {
      options.inputDelimiter = "// Input %num%: %name%";
    }
    options.setOutputCharset(getOutputCharset().name());

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

    options.stripNameSuffixes = stripNameSuffixes;
    options.stripTypePrefixes = stripTypePrefixes;
    options.setIdGenerators(idGenerators);
    options.ambiguateProperties = ambiguateProperties;
    options.disambiguateProperties = disambiguateProperties;

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

  public final static class Builder {

    private final File relativePathBase;

    private final String rootConfigFileContent;

    private String id = null;

    private final Manifest manifest;

    private String pathToClosureLibrary = null;

    private ImmutableList.Builder<String> paths = ImmutableList.builder();

    private ImmutableList.Builder<JsInput> inputs = ImmutableList.builder();

    private ImmutableList.Builder<String> externs = null;

    private boolean customExternsOnly = false;

    private CompilationMode compilationMode = CompilationMode.SIMPLE;

    private WarningLevel warningLevel = WarningLevel.DEFAULT;

    private boolean debug = false;

    private boolean prettyPrint = false;

    private boolean printInputDelimiter = false;

    private String outputWrapper = null;

    private Charset outputCharset = Charsets.US_ASCII;

    private boolean fingerprintJsFiles = false;

    private Map<DiagnosticGroup, CheckLevel> diagnosticGroups = null;

    private boolean treatWarningsAsErrors = false;

    private ModuleConfig.Builder moduleConfigBuilder = null;

    private Set<String> stripNameSuffixes = ImmutableSet.of();

    private Set<String> stripTypePrefixes = ImmutableSet.of();

    private Set<String> idGenerators = ImmutableSet.of();

    private boolean ambiguateProperties;

    private boolean disambiguateProperties;

    private final Map<String, JsonPrimitive> defines;

    /**
     * Pattern to validate a config id. A config id may not contain funny
     * characters, such as slashes, because ids are used in RESTful URLs, so
     * such characters would make proper URL parsing difficult.
     */
    private static final Pattern ID_PATTERN = Pattern.compile(
        AbstractGetHandler.CONFIG_ID_PATTERN);

    private Builder(File relativePathBase, String rootConfigFileContent) {
      Preconditions.checkNotNull(relativePathBase);
      Preconditions.checkArgument(relativePathBase.isDirectory(),
          relativePathBase + " is not a directory");
      Preconditions.checkNotNull(rootConfigFileContent);
      this.relativePathBase = relativePathBase;
      this.rootConfigFileContent = rootConfigFileContent;
      manifest = null;
      defines = Maps.newHashMap();
    }

    /** Effectively a copy constructor. */
    private Builder(Config config) {
      Preconditions.checkNotNull(config);
      this.relativePathBase = null;
      this.rootConfigFileContent = config.rootConfigFileContent;
      this.id = config.id;
      this.manifest = config.manifest;
      this.moduleConfigBuilder = (config.moduleConfig == null)
          ? null
          : ModuleConfig.builder(config.moduleConfig);
      this.compilationMode = config.compilationMode;
      this.warningLevel = config.warningLevel;
      this.debug = config.debug;
      this.prettyPrint = config.prettyPrint;
      this.printInputDelimiter = config.printInputDelimiter;
      this.outputWrapper = config.outputWrapper;
      this.outputCharset = config.outputCharset;
      this.fingerprintJsFiles = config.fingerprintJsFiles;
      this.diagnosticGroups = config.diagnosticGroups;
      this.treatWarningsAsErrors = config.treatWarningsAsErrors;
      this.stripNameSuffixes = config.stripNameSuffixes;
      this.stripTypePrefixes = config.stripTypePrefixes;
      this.idGenerators = config.idGenerators;
      this.ambiguateProperties = config.ambiguateProperties;
      this.disambiguateProperties = config.disambiguateProperties;
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

    public void addInputByName(String name) {
      String resolvedPath = ConfigOption.maybeResolvePath(name, this);
      addInput(new File(resolvedPath), name);
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

    public void setDebugOptions(boolean debug) {
      this.debug = debug;
    }

    public void setPrettyPrint(boolean prettyPrint) {
      this.prettyPrint = prettyPrint;
    }

    public void setPrintInputDelimiter(boolean printInputDelimiter) {
      this.printInputDelimiter = printInputDelimiter;
    }

    public void setOutputWrapper(String outputWrapper) {
      this.outputWrapper = outputWrapper;
    }

    public void setOutputCharset(Charset outputCharset) {
      this.outputCharset = outputCharset;
    }

    public void setFingerprintJsFiles(boolean fingerprint) {
      this.fingerprintJsFiles = fingerprint;
    }

    public void setDiagnosticGroups(Map<DiagnosticGroup, CheckLevel> groups) {
      this.diagnosticGroups = groups;
    }

    public void setTreatWarningsAsErrors(boolean treatWarningsAsErrors) {
      this.treatWarningsAsErrors = treatWarningsAsErrors;
    }

    public void addDefine(String name, JsonPrimitive primitive) {
      defines.put(name, primitive);
    }

    public void setStripNameSuffixes(Set<String> stripNameSuffixes) {
      this.stripNameSuffixes = ImmutableSet.copyOf(stripNameSuffixes);
    }

    public void setStripTypePrefixes(Set<String> stripTypePrefixes) {
      this.stripTypePrefixes = ImmutableSet.copyOf(stripTypePrefixes);
    }

    public void setIdGenerators(Set<String> idGenerators) {
      this.idGenerators = ImmutableSet.copyOf(idGenerators);
    }

    public void setAmbiguateProperties(boolean ambiguateProperties) {
      this.ambiguateProperties = ambiguateProperties;
    }

    public void setDisambiguateProperties(boolean disambiguateProperties) {
      this.disambiguateProperties = disambiguateProperties;
    }

    public Config build() {
      File closureLibraryDirectory = pathToClosureLibrary != null
          ? new File(pathToClosureLibrary)
          : null;

      ModuleConfig moduleConfig = (moduleConfigBuilder == null)
          ? null
          : moduleConfigBuilder.build();

      Manifest manifest;
      if (this.manifest == null) {
        List<File> externs = this.externs == null ? null
            : Lists.transform(this.externs.build(), STRING_TO_FILE);

        // If there is a module configuration, then add all of the
        // inputs from that.
        // TODO: Consider throwing an error if both "modules" and "inputs" are
        // specified.
        if (moduleConfig != null) {
          for (String inputName : moduleConfig.getInputNames()) {
            addInputByName(inputName);
          }
        }

        manifest = new Manifest(closureLibraryDirectory,
          Lists.transform(paths.build(), STRING_TO_FILE),
          inputs.build(),
          externs,
          customExternsOnly);
      } else {
        manifest = this.manifest;
      }

      Config config = new Config(
          id,
          rootConfigFileContent,
          manifest,
          moduleConfig,
          compilationMode,
          warningLevel,
          debug,
          prettyPrint,
          printInputDelimiter,
          outputWrapper,
          outputCharset,
          fingerprintJsFiles,
          diagnosticGroups,
          treatWarningsAsErrors,
          defines,
          stripNameSuffixes,
          stripTypePrefixes,
          idGenerators,
          ambiguateProperties,
          disambiguateProperties);

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

  /**
   * Configs are compared by their id so they can be sorted alphabetically.
   */
  @Override
  public int compareTo(Config otherConfig) {
    return getId().compareTo(otherConfig.getId());
  }
}
