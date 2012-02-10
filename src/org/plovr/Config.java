package org.plovr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.plovr.util.Pair;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.ClosureCodingConvention;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.CustomPassExecutionTime;
import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.PlovrCompilerOptions;
import com.google.javascript.jscomp.VariableMap;
import com.google.javascript.jscomp.WarningLevel;
import com.google.template.soy.xliffmsgplugin.XliffMsgPluginModule;


public final class Config implements Comparable<Config> {

  private static final Logger logger = Logger.getLogger("org.plovr.Config");

  /**
   * This is the name of the scope that all global variables will be
   * put into if the global-scope-name argument is supplied in the
   * plovr config. This scope name is never externally visible, but it
   * does have the effect of shadowing access to any page-scope
   * globals of that name.
   *
   * For example, if "$" were chosen, then that would shadow the
   * global jQuery object, which would be problematic for developers
   * who were using the Compiler with jQuery. As "a" is unlikely to be
   * supplied as an extern, it is a good choice for the GLOBAL_SCOPE_NAME.
   */
  public static final String GLOBAL_SCOPE_NAME = "a";

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

  private final File testTemplate;

  private final Set<File> testExcludePaths;

  private final ImmutableList<String> soyFunctionPlugins;

  private final CompilationMode compilationMode;

  private final WarningLevel warningLevel;

  private final boolean debug;

  private final boolean prettyPrint;

  private final boolean printInputDelimiter;

  private final File outputFile;

  private final String outputWrapper;

  private final Charset outputCharset;

  private final boolean fingerprintJsFiles;

  private final Map<String, CheckLevel> checkLevelsForDiagnosticGroups;

  private final boolean exportTestFunctions;

  private final boolean treatWarningsAsErrors;

  private final Map<String, JsonPrimitive> defines;

  private final ListMultimap<CustomPassExecutionTime, CompilerPassFactory> customPasses;

  private final File documentationOutputDirectory;

  private final Set<String> stripNameSuffixes;

  private final Set<String> stripTypePrefixes;

  private final Set<String> idGenerators;

  private final boolean ambiguateProperties;

  private final boolean disambiguateProperties;

  @Nullable
  private final JsonObject experimentalCompilerOptions;

  private final String globalScopeName;

  private final File variableMapInputFile;

  private final File variableMapOutputFile;

  private final File propertyMapInputFile;

  private final File propertyMapOutputFile;

  private List<FileWithLastModified> configFileInheritanceChain =
      Lists.newArrayList();

  private final List<File> cssInputs;

  private final List<String> allowedNonStandardCssFunctions;

  private final String gssFunctionMapProviderClassName;

  private final File cssOutputFile;

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
      File testTemplate,
      List<File> testExcludePaths,
      List<String> soyFunctionPlugins,
      CompilationMode compilationMode,
      WarningLevel warningLevel,
      boolean debug,
      boolean prettyPrint,
      boolean printInputDelimiter,
      @Nullable File outputFile,
      @Nullable String outputWrapper,
      Charset outputCharset,
      boolean fingerprintJsFiles,
      Map<String, CheckLevel> checkLevelsForDiagnosticGroups,
      boolean exportTestFunctions,
      boolean treatWarningsAsErrors,
      Map<String, JsonPrimitive> defines,
      ListMultimap<CustomPassExecutionTime, CompilerPassFactory> customPasses,
      File documentationOutputDirectory,
      Set<String> stripNameSuffixes,
      Set<String> stripTypePrefixes,
      Set<String> idGenerators,
      boolean ambiguateProperties,
      boolean disambiguateProperties,
      JsonObject experimentalCompilerOptions,
      List<FileWithLastModified> configFileInheritanceChain,
      String globalScopeName,
      File variableMapInputFile,
      File variableMapOutputFile,
      File propertyMapInputFile,
      File propertyMapOutputFile,
      List<File> cssInputs,
      List<String> allowedNonStandardCssFunctions,
      String gssFunctionMapProviderClassName,
      File cssOutputFile) {
    Preconditions.checkNotNull(defines);

    this.id = id;
    this.rootConfigFileContent = rootConfigFileContent;
    this.manifest = manifest;
    this.moduleConfig = moduleConfig;
    this.testTemplate = testTemplate;
    this.testExcludePaths = ImmutableSet.copyOf(testExcludePaths);
    this.soyFunctionPlugins = ImmutableList.copyOf(soyFunctionPlugins);
    this.compilationMode = compilationMode;
    this.warningLevel = warningLevel;
    this.debug = debug;
    this.prettyPrint = prettyPrint;
    this.printInputDelimiter = printInputDelimiter;
    this.outputFile = outputFile;
    this.outputWrapper = outputWrapper;
    this.outputCharset = outputCharset;
    this.fingerprintJsFiles = fingerprintJsFiles;
    this.checkLevelsForDiagnosticGroups = checkLevelsForDiagnosticGroups;
    this.exportTestFunctions = exportTestFunctions;
    this.treatWarningsAsErrors = treatWarningsAsErrors;
    this.customPasses = customPasses;
    this.documentationOutputDirectory = documentationOutputDirectory;
    this.defines = ImmutableMap.copyOf(defines);
    this.stripNameSuffixes = ImmutableSet.copyOf(stripNameSuffixes);
    this.stripTypePrefixes = ImmutableSet.copyOf(stripTypePrefixes);
    this.idGenerators = ImmutableSet.copyOf(idGenerators);
    this.ambiguateProperties = ambiguateProperties;
    this.disambiguateProperties = disambiguateProperties;
    this.experimentalCompilerOptions = experimentalCompilerOptions;
    this.configFileInheritanceChain = ImmutableList.copyOf(configFileInheritanceChain);
    this.globalScopeName = globalScopeName;
    this.variableMapInputFile = variableMapInputFile;
    this.variableMapOutputFile = variableMapOutputFile;
    this.propertyMapInputFile = propertyMapInputFile;
    this.propertyMapOutputFile = propertyMapOutputFile;
    this.cssInputs = ImmutableList.copyOf(cssInputs);
    this.allowedNonStandardCssFunctions = ImmutableList.copyOf(
        allowedNonStandardCssFunctions);
    this.gssFunctionMapProviderClassName = gssFunctionMapProviderClassName;
    this.cssOutputFile = cssOutputFile;
  }

  public static Builder builder(File relativePathBase, File configFile,
      String rootConfigFileContent) {
    return new Builder(relativePathBase, configFile, rootConfigFileContent);
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

  public ImmutableList<String> getSoyFunctionPlugins() {
    return soyFunctionPlugins;
  }

  public boolean hasSoyFunctionPlugins() {
    return !soyFunctionPlugins.isEmpty();
  }

  public CompilationMode getCompilationMode() {
    return compilationMode;
  }

  public WarningLevel getWarningLevel() {
    return warningLevel;
  }

  public File getOutputFile() {
    return outputFile;
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

  public File getDocumentationOutputDirectory() {
    return documentationOutputDirectory;
  }

  /**
   * Gets the file that was loaded by plovr to create this config. Note that
   * there may be other files that were loaded as part of the config
   * inheritance change in order to create this config.
   */
  public File getConfigFile() {
    int lastIndex = configFileInheritanceChain.size() - 1;
    return configFileInheritanceChain.get(lastIndex).file;
  }

  /**
   * @return true if the last modified time for the underlying config file
   *     (or any of the config files that it inherited) has changed since this
   *     config was originally created
   */
  public boolean isOutOfDate() {
    for (FileWithLastModified file : configFileInheritanceChain) {
      if (file.isOutOfDate()) {
        return true;
      }
    }
    return false;
  }

  @VisibleForTesting
  Set<String> getIdGenerators() {
    return idGenerators;
  }

  @VisibleForTesting
  ListMultimap<CustomPassExecutionTime, CompilerPassFactory> getCustomPasses() {
    return customPasses;
  }

  @VisibleForTesting
  Map<String, JsonPrimitive> getDefines() {
    return defines;
  }

  @VisibleForTesting
  Map<String, CheckLevel> getCheckLevelsForDiagnosticGroups() {
    return checkLevelsForDiagnosticGroups;
  }

  @VisibleForTesting
  JsonObject getExperimentalCompilerOptions() {
    return experimentalCompilerOptions;
  }

  public String getGlobalScopeName() {
    return globalScopeName;
  }

  public File getVariableMapInputFile() {
    return variableMapInputFile;
  }

  public File getVariableMapOutputFile() {
    return variableMapOutputFile;
  }

  public File getPropertyMapInputFile() {
    return propertyMapInputFile;
  }

  public File getPropertyMapOutputFile() {
    return propertyMapOutputFile;
  }

  public File getTestTemplate() {
    return testTemplate;
  }

  public Set<File> getTestExcludePaths() {
    return testExcludePaths;
  }

  public List<File> getCssInputs() {
    return cssInputs;
  }

  public List<String> getAllowedNonStandardCssFunctions() {
    return allowedNonStandardCssFunctions;
  }

  public String getGssFunctionMapProviderClassName() {
    return gssFunctionMapProviderClassName;
  }

  public File getCssOutputFile() {
    return cssOutputFile;
  }

  /**
   * @param path a relative path, such as "foo/bar_test.js" or
   *     "foo/bar_test.html".
   * @return the file under a test directory, if it exists, or null
   */
  public @Nullable File getTestFile(String path) {
    for (File dependency : manifest.getDependencies()) {
      if (!dependency.isDirectory()) {
        continue;
      }

      File testFile = new File(dependency, path);
      // Make sure the file exists and is a child of the dependency directory so
      // there are no security leaks by requesting a path with ../ in it.
      if (testFile.exists() && FileUtil.contains(dependency, testFile)) {
        return testFile;
      }
    }
    return null;
  }

  public PlovrCompilerOptions getCompilerOptions(
      PlovrClosureCompiler compiler) {
    Preconditions.checkArgument(compilationMode != CompilationMode.RAW,
        "Cannot compile using RAW mode");
    CompilationLevel level = compilationMode.getCompilationLevel();
    logger.info("Compiling with level: " + level);
    PlovrCompilerOptions options = new PlovrCompilerOptions();
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

    options.exportTestFunctions = exportTestFunctions;
    options.stripNameSuffixes = stripNameSuffixes;
    options.stripTypePrefixes = stripTypePrefixes;
    options.setIdGenerators(idGenerators);
    options.ambiguateProperties = ambiguateProperties;
    options.disambiguateProperties = disambiguateProperties;

    // Instantiate the custom compiler passes and register any DiagnosticGroups
    // from those passes.
    PlovrDiagnosticGroups groups = compiler.getDiagnosticGroups();
    Multimap<CustomPassExecutionTime, CompilerPass> passes = getCustomPasses(options);
    for (Map.Entry<CustomPassExecutionTime, Collection<CompilerPassFactory>> entry :
        customPasses.asMap().entrySet()) {
      CustomPassExecutionTime executionTime = entry.getKey();
      Collection<CompilerPassFactory> factories = entry.getValue();
      for (CompilerPassFactory factory : factories) {
        CompilerPass compilerPass = factory.createCompilerPass(compiler, this);
        passes.put(executionTime, compilerPass);
        if (compilerPass instanceof DiagnosticGroupRegistrar) {
          DiagnosticGroupRegistrar registrar = (DiagnosticGroupRegistrar)compilerPass;
          registrar.registerDiagnosticGroupsWith(groups);
        }
      }
    }

    if (moduleConfig != null) {
      options.crossModuleCodeMotion = true;
      options.crossModuleMethodMotion = true;
      if (!Strings.isNullOrEmpty(globalScopeName)) {
        Preconditions.checkState(
            options.collapseAnonymousFunctions == true ||
            level != CompilationLevel.ADVANCED_OPTIMIZATIONS,
            "For reasons unknown, setting this to false ends up " +
            "with a fairly larger final output, even though we just go " +
            "and re-anonymize the functions a few steps later.");
        options.globalScopeName = GLOBAL_SCOPE_NAME;
      }
    }

    // Now that custom passes have registered with the PlovrDiagnosticGroups,
    // warning levels as specified in the "checks" config option should be
    // applied.
    if (checkLevelsForDiagnosticGroups != null) {
      for (Map.Entry<String, CheckLevel> entry :
          checkLevelsForDiagnosticGroups.entrySet()) {
        DiagnosticGroup group = groups.forName(entry.getKey());
        if (group == null) {
          System.err.printf("WARNING: UNRECOGNIZED CHECK \"%s\" in your " +
                            "plovr config. Ignoring.\n", entry.getKey());
          continue;
        }
        CheckLevel checkLevel = entry.getValue();
        options.setWarningLevel(group, checkLevel);
      }
    }

    if (variableMapInputFile != null) {
      try {
        options.inputVariableMapSerialized = VariableMap.load(
            variableMapInputFile.getAbsolutePath()).toBytes();
      } catch (IOException e) {
        logger.severe("The variable map input file '" + variableMapInputFile +
                      "' could not be loaded: " + e.getMessage());
      }
    }

    if (propertyMapInputFile != null) {
      try {
        options.inputPropertyMapSerialized = VariableMap.load(
            propertyMapInputFile.getAbsolutePath()).toBytes();
      } catch (IOException e) {
        logger.severe("The property map input file '" + propertyMapInputFile +
                      "' could not be loaded: " + e.getMessage());
      }
    }

    // This is a hack to work around the fact that a SourceMap
    // will not be created unless a file is specified to which the SourceMap
    // should be written.
    // TODO(bolinfest): Change com.google.javascript.jscomp.PlovrCompilerOptions so
    // that this is configured by a boolean, just like enableExternExports() was
    // added to support generating externs without writing them to a file.
    try {
      File tempFile = File.createTempFile("source", "map");
      options.sourceMapOutputPath = tempFile.getAbsolutePath();
    } catch (IOException e) {
      logger.severe("A temp file for the Source Map could not be created");
    }

    options.setExternExports(true);

    // After all of the options are set, apply the experimental Compiler
    // options, which may override existing options that are set.
    applyExperimentalCompilerOptions(experimentalCompilerOptions, options);

    return options;
  }

  /**
   * Lazily creates and returns the customPasses ListMultimap for a PlovrCompilerOptions.
   */
  private static Multimap<CustomPassExecutionTime, CompilerPass> getCustomPasses(
      PlovrCompilerOptions options) {
    Multimap<CustomPassExecutionTime, CompilerPass> customPasses =
        options.customPasses;
    if (customPasses == null) {
      customPasses = ArrayListMultimap.create();
      options.customPasses = customPasses;
    }
    return customPasses;
  }

  @VisibleForTesting
  static void applyExperimentalCompilerOptions(
      JsonObject experimentalCompilerOptions,
      PlovrCompilerOptions options) {
    // This method needs to be refactored, but all of the checked exceptions
    // make refactoring it difficult.
    if (experimentalCompilerOptions == null) {
      return;
    }

    for (Map.Entry<String, JsonElement> entry :
        experimentalCompilerOptions.entrySet()) {
      JsonElement el = entry.getValue();
      // Currently, only primitive values are considered, though in the
      // future, it would be good to support lists, maps, and sets.
      if (el == null || !el.isJsonPrimitive()) {
        System.err.println("No support for values like: " + el);
        continue;
      }
      JsonPrimitive primitive = el.getAsJsonPrimitive();

      String name = entry.getKey();
      Field field;
      try {
        try {
          field = PlovrCompilerOptions.class.getField(name);
        } catch (NoSuchFieldException e) {
          field = null;
        }

        // TODO: If the field is private, use field.setAccessible(true), though
        // only if there is no public setter method (which may have other side-
        // effects, which is why it should be preferred).

        if (field != null) {
          Class<?> fieldClass = field.getType();

          if (primitive.isBoolean() &&
              (Boolean.class.equals(fieldClass) ||
              boolean.class.equals(fieldClass))) {
            field.set(options, primitive.getAsBoolean());
            continue;
          } else if (primitive.isNumber() && isNumber(fieldClass)) {
            field.set(options, primitive.getAsNumber());
            continue;
          } else if (primitive.isString()) {
            if (String.class.equals(fieldClass)) {
              field.set(options, primitive.getAsString());
              continue;
            } else if (fieldClass.isEnum()) {
              String enumName = primitive.getAsString();
              Method valueOf = fieldClass.getMethod("valueOf", String.class);
              Object enumValue = valueOf.invoke(null, enumName);
              field.set(options, enumValue);
              continue;
            }
          }
        }

        // At this point, either there was no field with the specified name
        // or the field could not be set. Try to find an appropriate setter
        // method to set the option instead.
        String setterName = "set" + createSetterMethodNameForFieldName(name);
        if (primitive.isBoolean()) {
          Method setter = PlovrCompilerOptions.class.getMethod(setterName, boolean.class);
          setter.invoke(options, primitive.getAsBoolean());
          continue;
        } else if (primitive.isNumber()) {
          // TODO(bolinfest): Support the numeric setter. Need to test whether
          // it works with an int or a double.
        } else if (primitive.isString()) {
          try {
            Method setter = PlovrCompilerOptions.class.getMethod(setterName, String.class);
            setter.invoke(options, primitive.getAsString());
            continue;
          } catch (NoSuchMethodException e) {
            // Ignore exception and try setting value as an enum instead.
            if (setCompilerOptionToEnumValue(
                options, setterName, primitive.getAsString())) {
              continue;
            }
          }
        }
      } catch (SecurityException e) {
        // OK
      } catch (IllegalArgumentException e) {
        // OK
      } catch (IllegalAccessException e) {
        // OK
      } catch (NoSuchMethodException e) {
        // OK
      } catch (InvocationTargetException e) {
        // OK
      }

      System.err.println("Could not set experimental compiler option: " +
          name);
    }
  }

  /**
   * @return true if this was successful
   */
  private static boolean setCompilerOptionToEnumValue(
      PlovrCompilerOptions options,
      String setterMethodName,
      String value) {
    for (Method m : PlovrCompilerOptions.class.getMethods()) {
      if (setterMethodName.equals(m.getName())) {
        Class<?> paramClass = m.getParameterTypes()[0];
        if (paramClass.isEnum()) {
          try {
            Method valueOf = paramClass.getMethod("valueOf", String.class);
            Object enumValue = valueOf.invoke(null, value);
            m.invoke(options, enumValue);
          } catch (IllegalArgumentException e) {
            // OK
          } catch (IllegalAccessException e) {
            // OK
          } catch (InvocationTargetException e) {
            // OK
          } catch (SecurityException e) {
            // OK
          } catch (NoSuchMethodException e) {
            // OK
          }
          return true;
        }
      }
    }
    return false;
  }
  // TODO(bolinfest): Figure out a better way to do this isNumber() stuff.
  @SuppressWarnings("unchecked")
  private static final Set<Class<? extends Number>> numericClasses =
      ImmutableSet.<Class<? extends Number>>of(
      int.class,
      Integer.class,
      long.class,
      Long.class,
      float.class,
      Float.class,
      double.class,
      Double.class);

  private static boolean isNumber(Class<?> clazz) {
    return numericClasses.contains(clazz);
  }

  private static String createSetterMethodNameForFieldName(String fieldName) {
    return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
  }

  @Override
  public String toString() {
    return id;
  }

  public final static class Builder {

    private final File relativePathBase;

    /**
     * This is the inheritance chain of config files that produced this config.
     * Descendants are added to the end of the list.
     */
    private List<FileWithLastModified> configFileInheritanceChain =
        Lists.newArrayList();

    private final String rootConfigFileContent;

    private String id = null;

    private final Manifest manifest;

    private String pathToClosureLibrary = null;

    private boolean excludeClosureLibrary = false;

    private final List<String> paths = Lists.newArrayList();

    private final List<File> testExcludePaths;

    /** List of (file, path) pairs for inputs */
    private final List<Pair<File, String>> inputs = Lists.newArrayList();

    private List<String> externs = null;

    private List<JsInput> builtInExterns = null;

    private File testTemplate = null;

    private ImmutableList.Builder<String> soyFunctionPlugins = null;

    private ListMultimap<CustomPassExecutionTime, CompilerPassFactory> customPasses = ImmutableListMultimap.of();

    private File documentationOutputDirectory = null;

    private boolean customExternsOnly = false;

    private CompilationMode compilationMode = CompilationMode.SIMPLE;

    private WarningLevel warningLevel = WarningLevel.DEFAULT;

    private boolean debug = false;

    private boolean prettyPrint = false;

    private boolean printInputDelimiter = false;

    private File outputFile = null;

    private String outputWrapper = null;

    private Charset outputCharset = Charsets.US_ASCII;

    private boolean fingerprintJsFiles = false;

    private Map<String, CheckLevel> checkLevelsForDiagnosticGroups = null;

    private boolean exportTestFunctions = false;

    private boolean treatWarningsAsErrors = false;

    private ModuleConfig.Builder moduleConfigBuilder = null;

    private Set<String> stripNameSuffixes = ImmutableSet.of();

    private Set<String> stripTypePrefixes = ImmutableSet.of();

    private Set<String> idGenerators = ImmutableSet.of();

    private boolean ambiguateProperties;

    private boolean disambiguateProperties;

    private JsonObject experimentalCompilerOptions;

    private String globalScopeName;

    private File variableMapInputFile;

    private File variableMapOutputFile;

    private File propertyMapInputFile;

    private File propertyMapOutputFile;

    private final Map<String, JsonPrimitive> defines;

    /************************* CSS OPTIONS *************************/

    private List<File> cssInputs = Lists.newArrayList();

    private List<String> allowedNonStandardFunctions = Lists.newArrayList();

    private String gssFunctionMapProviderClassName;

    private File cssOutputFile = null;

    /**
     * Pattern to validate a config id. A config id may not contain funny
     * characters, such as slashes, because ids are used in RESTful URLs, so
     * such characters would make proper URL parsing difficult.
     */
    private static final Pattern ID_PATTERN = Pattern.compile(
        AbstractGetHandler.CONFIG_ID_PATTERN);

    private Builder(File relativePathBase, File configFile, String rootConfigFileContent) {
      this(relativePathBase, rootConfigFileContent);
      addConfigFile(configFile);
    }

    private Builder(File relativePathBase, String rootConfigFileContent) {
      Preconditions.checkNotNull(relativePathBase);
      Preconditions.checkArgument(relativePathBase.isDirectory(),
          relativePathBase + " is not a directory");
      Preconditions.checkNotNull(rootConfigFileContent);
      this.relativePathBase = relativePathBase;
      this.rootConfigFileContent = rootConfigFileContent;
      testExcludePaths = Lists.newArrayList();
      manifest = null;
      defines = Maps.newHashMap();
    }

    /** Effectively a copy constructor. */
    private Builder(Config config) {
      Preconditions.checkNotNull(config);
      this.relativePathBase = null;
      this.configFileInheritanceChain = Lists.newArrayList(
          config.configFileInheritanceChain);
      this.rootConfigFileContent = config.rootConfigFileContent;
      this.id = config.id;
      this.manifest = config.manifest;
      this.moduleConfigBuilder = (config.moduleConfig == null)
          ? null
          : ModuleConfig.builder(config.moduleConfig);
      this.testTemplate = config.testTemplate;
      this.testExcludePaths = Lists.newArrayList(config.testExcludePaths);
      this.soyFunctionPlugins = config.hasSoyFunctionPlugins()
          ? new ImmutableList.Builder<String>().addAll(config.getSoyFunctionPlugins())
          : null;
      this.customPasses = config.customPasses;
      this.documentationOutputDirectory = config.documentationOutputDirectory;
      this.compilationMode = config.compilationMode;
      this.warningLevel = config.warningLevel;
      this.debug = config.debug;
      this.prettyPrint = config.prettyPrint;
      this.printInputDelimiter = config.printInputDelimiter;
      this.outputFile = config.outputFile;
      this.outputWrapper = config.outputWrapper;
      this.outputCharset = config.outputCharset;
      this.fingerprintJsFiles = config.fingerprintJsFiles;
      this.checkLevelsForDiagnosticGroups = config.checkLevelsForDiagnosticGroups;
      this.exportTestFunctions = config.exportTestFunctions;
      this.treatWarningsAsErrors = config.treatWarningsAsErrors;
      this.stripNameSuffixes = config.stripNameSuffixes;
      this.stripTypePrefixes = config.stripTypePrefixes;
      this.idGenerators = config.idGenerators;
      this.ambiguateProperties = config.ambiguateProperties;
      this.disambiguateProperties = config.disambiguateProperties;
      this.experimentalCompilerOptions = config.experimentalCompilerOptions;
      this.globalScopeName = config.globalScopeName;
      this.variableMapInputFile = config.variableMapInputFile;
      this.variableMapOutputFile = config.variableMapOutputFile;
      this.propertyMapInputFile = config.propertyMapInputFile;
      this.propertyMapOutputFile = config.propertyMapOutputFile;
      this.defines = Maps.newHashMap(config.defines);
      this.cssInputs = Lists.newArrayList(config.cssInputs);
      this.allowedNonStandardFunctions = Lists.newArrayList(
          config.allowedNonStandardCssFunctions);
      this.gssFunctionMapProviderClassName = config.
          gssFunctionMapProviderClassName;
      this.cssOutputFile = config.cssOutputFile;
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

    public void resetPaths() {
      paths.clear();
    }

    public void addInput(File file, String name) {
      Preconditions.checkNotNull(file);
      Preconditions.checkNotNull(name);
      inputs.add(Pair.of(file, name));
    }

    public void addInputByName(String name) {
      String resolvedPath = ConfigOption.maybeResolvePath(name, this);
      addInput(new File(resolvedPath), name);
    }

    public void resetInputs() {
      inputs.clear();
    }

    public void addExtern(String extern) {
      if (externs == null) {
        externs = Lists.newArrayList();
      }
      externs.add(extern);
    }

    /**
     * @param builtInExtern should be of the form "//chrome_extensions.js"
     */
    public void addBuiltInExtern(String builtInExtern) {
      Preconditions.checkArgument(builtInExtern.startsWith("//"));
      if (builtInExterns == null) {
        builtInExterns = Lists.newArrayList();
      }
      String path = builtInExtern.replace("//", "/contrib/");
      JsInput extern = new ResourceJsInput(path);
      builtInExterns.add(extern);
    }

    public void resetExterns() {
      externs = null;
      builtInExterns = null;
    }

    public void setCustomExternsOnly(boolean customExternsOnly) {
      this.customExternsOnly = customExternsOnly;
    }

    public void setPathToClosureLibrary(String pathToClosureLibrary) {
      this.pathToClosureLibrary = pathToClosureLibrary;
    }

    public void setExcludeClosureLibrary(boolean excludeClosureLibrary) {
      this.excludeClosureLibrary = excludeClosureLibrary;
    }

    /**
     * Appends the specified file to the end of the config file inheritance
     * chain for this builder.
     */
    void addConfigFile(File configFile) {
      Preconditions.checkNotNull(configFile);
      configFileInheritanceChain.add(new FileWithLastModified(configFile));
    }

    public ModuleConfig.Builder getModuleConfigBuilder() {
      if (moduleConfigBuilder == null) {
        moduleConfigBuilder = ModuleConfig.builder(relativePathBase);
      }
      return moduleConfigBuilder;
    }

    public void resetModuleConfigBuilder() {
      moduleConfigBuilder = null;
    }

    public void setTestTemplate(File testTemplate) {
      this.testTemplate = testTemplate;
    }

    public void addTestExcludePath(final File testExcludePath) {
      Preconditions.checkNotNull(testExcludePath);

      Set<File> paths = ImmutableSet.copyOf(
          Lists.transform(this.paths, STRING_TO_FILE));
      File pathThatContainsExclude = Iterables.find(paths,
          new Predicate<File>() {
        @Override
        public boolean apply(File path) {
          return FileUtil.contains(path, testExcludePath);
        }
      }, null);
      Preconditions.checkNotNull(pathThatContainsExclude,
          "No path contains test exclude: " + testExcludePath);

      testExcludePaths.add(testExcludePath);
    }

    public void resetTestExcludePaths() {
      testExcludePaths.clear();
    }

    /**
     * Adds a soy plugin module.
     *
     * <pre>
     *   addSoyFunctionPlugin("org.plovr.soy.function.PlovrModule")
     * </pre>
     *
     * @param qualifiedName the module class name
     */
    public void addSoyFunctionPlugin(String qualifiedName) {
      Preconditions.checkNotNull(qualifiedName);

      if (soyFunctionPlugins == null) {
        soyFunctionPlugins = ImmutableList.builder();
        // always add this one
        soyFunctionPlugins.add(XliffMsgPluginModule.class.getName());
      }
      soyFunctionPlugins.add(qualifiedName);
    }

    public void resetSoyFunctionPlugins() {
      soyFunctionPlugins = null;
    }

    public void setDocumentationOutputDirectory(File documentationOutputDirectory) {
      Preconditions.checkNotNull(documentationOutputDirectory);
      this.documentationOutputDirectory = documentationOutputDirectory;
    }

    public void setCustomPasses(
        ListMultimap<CustomPassExecutionTime, CompilerPassFactory> customPasses) {
      this.customPasses = ImmutableListMultimap.copyOf(customPasses);
    }

    public void resetCustomPasses() {
      this.customPasses = null;
    }

    /**
     * @return an immutable {@link ListMultimap}
     */
    public ListMultimap<CustomPassExecutionTime, CompilerPassFactory> getCustomPasses() {
      if (customPasses != null) {
        return customPasses;
      } else {
        return ImmutableListMultimap.of();
      }
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

    public void setOutputFile(File outputFile) {
      this.outputFile = outputFile;
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

    /**
     * Each key in groups should correspond to a {@link DiagnosticGroup};
     * however, a key cannot map to a {@link DiagnosticGroup} yet because
     * custom compiler passes may add their own entries to the
     * {@link PlovrDiagnosticGroups} collection, which is not populated until
     * the {@link PlovrCompilerOptions} are created.
     * @param groups
     */
    public void setCheckLevelsForDiagnosticGroups(Map<String, CheckLevel> groups) {
      this.checkLevelsForDiagnosticGroups = groups;
    }

    public void resetChecks() {
      this.checkLevelsForDiagnosticGroups = null;
    }

    public void setExportTestFunctions(boolean exportTestFunctions) {
      this.exportTestFunctions = exportTestFunctions;
    }

    public void setTreatWarningsAsErrors(boolean treatWarningsAsErrors) {
      this.treatWarningsAsErrors = treatWarningsAsErrors;
    }

    public void addDefine(String name, JsonPrimitive primitive) {
      defines.put(name, primitive);
    }

    public void resetDefines() {
      defines.clear();
    }

    public void setStripNameSuffixes(Set<String> stripNameSuffixes) {
      this.stripNameSuffixes = ImmutableSet.copyOf(stripNameSuffixes);
    }

    public void resetStripNameSuffixes() {
      this.stripNameSuffixes = null;
    }

    public void setStripTypePrefixes(Set<String> stripTypePrefixes) {
      this.stripTypePrefixes = ImmutableSet.copyOf(stripTypePrefixes);
    }

    public void resetStripTypePrefixes() {
      this.stripTypePrefixes = null;
    }

    public void setIdGenerators(Set<String> idGenerators) {
      this.idGenerators = ImmutableSet.copyOf(idGenerators);
    }

    public void resetIdGenerators() {
      this.idGenerators = null;
    }

    public void setAmbiguateProperties(boolean ambiguateProperties) {
      this.ambiguateProperties = ambiguateProperties;
    }

    public void setDisambiguateProperties(boolean disambiguateProperties) {
      this.disambiguateProperties = disambiguateProperties;
    }

    public void setExperimentalCompilerOptions(
        JsonObject experimentalCompilerOptions) {
      this.experimentalCompilerOptions = experimentalCompilerOptions;
    }

    public JsonObject getExperimentalCompilerOptions() {
      return experimentalCompilerOptions;
    }

    public void resetExperimentalCompilerOptions() {
      this.experimentalCompilerOptions = null;
    }

    public void setGlobalScopeName(String scope) {
      this.globalScopeName = scope;
    }

    public void setVariableMapInputFile(File file) {
      this.variableMapInputFile = file;
    }

    public void setVariableMapOutputFile(File file) {
      this.variableMapOutputFile = file;
    }

    public void setPropertyMapInputFile(File file) {
      this.propertyMapInputFile = file;
    }

    public void setPropertyMapOutputFile(File file) {
      this.propertyMapOutputFile = file;
    }

    public void addCssInput(File cssInput) {
      Preconditions.checkNotNull(cssInput);
      Preconditions.checkArgument(cssInput.exists(),
          "CSS input %s must exist", cssInput.getAbsolutePath());
      Preconditions.checkArgument(cssInput.isFile(),
          "CSS input %s must be a file", cssInput.getAbsolutePath());
      cssInputs.add(cssInput);
    }

    public void resetCssInputs() {
      cssInputs.clear();
    }

    public void addAllowedNonStandardCssFunction(String function) {
      allowedNonStandardFunctions.add(function);
    }

    public void resetAllowedNonStandardCssFunctions() {
      allowedNonStandardFunctions.clear();
    }

    public void setGssFunctionMapProvider(
        String gssFunctionMapProviderClassName) {
      this.gssFunctionMapProviderClassName = gssFunctionMapProviderClassName;
    }

    public void setCssOutputFile(File cssOutputFile) {
      this.cssOutputFile = cssOutputFile;
    }

    public Config build() {
      File closureLibraryDirectory = pathToClosureLibrary != null
          ? new File(pathToClosureLibrary)
          : null;

      ModuleConfig moduleConfig = (moduleConfigBuilder == null)
          ? null
          : moduleConfigBuilder.build();

      List<String> soyFunctionNames = createSoyFunctionPluginNames();

      Manifest manifest;
      if (this.manifest == null) {
        List<File> externs = this.externs == null ? null
            : Lists.transform(this.externs, STRING_TO_FILE);

        // If there is a module configuration, then add all of the
        // inputs from that.
        // TODO: Consider throwing an error if both "modules" and "inputs" are
        // specified.
        if (moduleConfig != null) {
          for (String inputName : moduleConfig.getInputNames()) {
            addInputByName(inputName);
          }
        }

        SoyFileOptions soyFileOptions = new SoyFileOptions(soyFunctionNames,
            !this.excludeClosureLibrary);

        manifest = new Manifest(
            excludeClosureLibrary,
            closureLibraryDirectory,
            Lists.transform(paths, STRING_TO_FILE),
            createJsInputs(soyFileOptions),
            externs,
            builtInExterns != null ? ImmutableList.copyOf(builtInExterns) : null,
            soyFileOptions,
            customExternsOnly);
      } else {
        manifest = this.manifest;
      }

      Config config = new Config(
          id,
          rootConfigFileContent,
          manifest,
          moduleConfig,
          testTemplate,
          testExcludePaths,
          soyFunctionNames,
          compilationMode,
          warningLevel,
          debug,
          prettyPrint,
          printInputDelimiter,
          outputFile,
          outputWrapper,
          outputCharset,
          fingerprintJsFiles,
          checkLevelsForDiagnosticGroups,
          exportTestFunctions,
          treatWarningsAsErrors,
          defines,
          customPasses,
          documentationOutputDirectory,
          stripNameSuffixes,
          stripTypePrefixes,
          idGenerators,
          ambiguateProperties,
          disambiguateProperties,
          experimentalCompilerOptions,
          configFileInheritanceChain,
          globalScopeName,
          variableMapInputFile,
          variableMapOutputFile,
          propertyMapInputFile,
          propertyMapOutputFile,
          cssInputs,
          allowedNonStandardFunctions,
          gssFunctionMapProviderClassName,
          cssOutputFile);

      return config;
    }

    private List<JsInput> createJsInputs(SoyFileOptions soyFileOptions) {
      ImmutableList<Pair<File, String>> inputFiles = ImmutableList.copyOf(inputs);
      List<JsInput> jsInputs = Lists.newArrayListWithCapacity(inputFiles.size());
      for (Pair<File, String> pair : inputFiles) {
        File file = pair.getFirst();
        String name = pair.getSecond();

        jsInputs.add(
            LocalFileJsInput.createForFileWithName(file, name, soyFileOptions));
      }

      return jsInputs;
    }

    private List<String> createSoyFunctionPluginNames() {
      if (this.soyFunctionPlugins == null) {
        return ImmutableList.of();
      }
      // TODO: Do we need to add any other modules than what we've configured?
      return this.soyFunctionPlugins.build();
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

  private static class FileWithLastModified {
    public final File file;
    public final long lastModified;
    private FileWithLastModified(File file) {
      Preconditions.checkNotNull(file);
      this.file = file;
      this.lastModified = file.lastModified();
    }
    public boolean isOutOfDate() {
      // true if the last modified time has changed
      return this.lastModified != file.lastModified();
    }
  }
}
