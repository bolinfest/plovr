package org.plovr;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
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
import com.google.common.collect.Sets;
import com.google.common.css.JobDescription;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.Injector;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.ClosureCodingConvention;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.CustomPassExecutionTime;
import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.PlovrCompilerOptions;
import com.google.javascript.jscomp.SourceMap.LocationMapping;
import com.google.javascript.jscomp.StrictWarningsGuard;
import com.google.javascript.jscomp.VariableMap;
import com.google.javascript.jscomp.WarningLevel;
import com.google.javascript.jscomp.XtbMessageBundle;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.SoyMsgBundleHandler;
import com.google.template.soy.msgs.SoyMsgException;
import com.google.template.soy.msgs.SoyMsgPlugin;
import com.google.template.soy.xliffmsgplugin.XliffMsgPluginModule;

import org.plovr.util.Pair;
import org.plovr.webdriver.WebDriverFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.Nullable;


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
   * who were using the Compiler with jQuery. As "z" is unlikely to be
   * supplied as an extern, it is a good choice for the GLOBAL_SCOPE_NAME.
   */
  public static final String GLOBAL_SCOPE_NAME = "z";

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

  private final List<WebDriverFactory> testDrivers;

  private final File testTemplate;

  private final Set<File> testExcludePaths;

  private final ImmutableList<String> soyFunctionPlugins;

  private final String soyTranslationPlugin;

  private final boolean soyUseInjectedData;

  private final CompilationMode compilationMode;

  private final WarningLevel warningLevel;

  private final boolean debug;

  private final boolean prettyPrint;

  private final boolean printInputDelimiter;

  private final File outputFile;

  private final String outputWrapper;

  private final Charset outputCharset;

  private final File cacheOutputFile;

  private final boolean fingerprintJsFiles;

  private final Map<String, CheckLevel> checkLevelsForDiagnosticGroups;

  private final boolean exportTestFunctions;

  private final boolean treatWarningsAsErrors;

  private final Map<String, JsonPrimitive> defines;

  private final ListMultimap<CustomPassExecutionTime, CompilerPassFactory> customPasses;

  private final List<WarningsGuardFactory> customWarningsGuards;

  private final File documentationOutputDirectory;

  private final Set<String> stripNameSuffixes;

  private final Set<String> stripTypePrefixes;

  private final Set<String> idGenerators;

  private final boolean ambiguateProperties;

  private final boolean disambiguateProperties;

  private final LanguageMode languageIn;

  private final LanguageMode languageOut;

  private final boolean newTypeInference;

  @Nullable
  private final JsonObject experimentalCompilerOptions;

  private final String globalScopeName;

  private final File variableMapInputFile;

  private final File variableMapOutputFile;

  private final File propertyMapInputFile;

  private final File propertyMapOutputFile;

  private final String sourceMapBaseUrl;

  private final String sourceMapOutputName;

  private List<FileWithLastModified> configFileInheritanceChain =
      Lists.newArrayList();

  private final List<File> cssInputs;

  private final Set<String> cssDefines;

  private final List<String> allowedUnrecognizedProperties;

  private final List<String> allowedNonStandardCssFunctions;

  private final String gssFunctionMapProviderClassName;

  private final File cssOutputFile;

  private final File translationsDirectory;

  private final String language;

  private final JobDescription.OutputFormat cssOutputFormat;

  private final PrintStream errorStream;

  private final List<LocationMapping> locationMappings;

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
      List<WebDriverFactory> testDrivers,
      @Nullable ModuleConfig moduleConfig,
      File testTemplate,
      List<File> testExcludePaths,
      List<String> soyFunctionPlugins,
      String soyTranslationPlugin,
      boolean soyUseInjectedData,
      CompilationMode compilationMode,
      WarningLevel warningLevel,
      boolean debug,
      boolean prettyPrint,
      boolean printInputDelimiter,
      @Nullable File outputFile,
      @Nullable String outputWrapper,
      Charset outputCharset,
      @Nullable File cacheOutputFile,
      boolean fingerprintJsFiles,
      Map<String, CheckLevel> checkLevelsForDiagnosticGroups,
      boolean exportTestFunctions,
      boolean treatWarningsAsErrors,
      Map<String, JsonPrimitive> defines,
      ListMultimap<CustomPassExecutionTime, CompilerPassFactory> customPasses,
      List<WarningsGuardFactory> customWarningsGuards,
      File documentationOutputDirectory,
      Set<String> stripNameSuffixes,
      Set<String> stripTypePrefixes,
      Set<String> idGenerators,
      boolean ambiguateProperties,
      boolean disambiguateProperties,
      LanguageMode languageIn,
      LanguageMode languageOut,
      boolean newTypeInference,
      JsonObject experimentalCompilerOptions,
      List<FileWithLastModified> configFileInheritanceChain,
      String globalScopeName,
      File variableMapInputFile,
      File variableMapOutputFile,
      File propertyMapInputFile,
      File propertyMapOutputFile,
      String sourceMapBaseUrl,
      String sourceMapOutputName,
      List<File> cssInputs,
      Set<String> cssDefines,
      List<String> allowedUnrecognizedProperties,
      List<String> allowedNonStandardCssFunctions,
      String gssFunctionMapProviderClassName,
      File cssOutputFile,
      JobDescription.OutputFormat cssOutputFormat,
      PrintStream errorStream,
      List<LocationMapping> locationMappings,
      File translationsDirectory,
      String language) {
    Preconditions.checkNotNull(defines);

    this.id = id;
    this.rootConfigFileContent = rootConfigFileContent;
    this.manifest = manifest;
    this.moduleConfig = moduleConfig;
    this.testDrivers = ImmutableList.copyOf(testDrivers);
    this.testTemplate = testTemplate;
    this.testExcludePaths = ImmutableSet.copyOf(testExcludePaths);
    this.soyFunctionPlugins = ImmutableList.copyOf(soyFunctionPlugins);
    this.soyTranslationPlugin = soyTranslationPlugin;
    this.soyUseInjectedData = soyUseInjectedData;
    this.compilationMode = compilationMode;
    this.warningLevel = warningLevel;
    this.debug = debug;
    this.prettyPrint = prettyPrint;
    this.printInputDelimiter = printInputDelimiter;
    this.outputFile = outputFile;
    this.outputWrapper = outputWrapper;
    this.outputCharset = outputCharset;
    this.cacheOutputFile = cacheOutputFile;
    this.fingerprintJsFiles = fingerprintJsFiles;
    this.checkLevelsForDiagnosticGroups = checkLevelsForDiagnosticGroups;
    this.exportTestFunctions = exportTestFunctions;
    this.treatWarningsAsErrors = treatWarningsAsErrors;
    this.customPasses = customPasses;
    this.customWarningsGuards = customWarningsGuards;
    this.documentationOutputDirectory = documentationOutputDirectory;
    this.defines = ImmutableMap.copyOf(defines);
    this.stripNameSuffixes = ImmutableSet.copyOf(stripNameSuffixes);
    this.stripTypePrefixes = ImmutableSet.copyOf(stripTypePrefixes);
    this.idGenerators = ImmutableSet.copyOf(idGenerators);
    this.ambiguateProperties = ambiguateProperties;
    this.disambiguateProperties = disambiguateProperties;
    this.languageIn = languageIn;
    this.languageOut = languageOut;
    this.newTypeInference = newTypeInference;
    this.experimentalCompilerOptions = experimentalCompilerOptions;
    this.configFileInheritanceChain = ImmutableList.copyOf(configFileInheritanceChain);
    this.globalScopeName = globalScopeName;
    this.variableMapInputFile = variableMapInputFile;
    this.variableMapOutputFile = variableMapOutputFile;
    this.propertyMapInputFile = propertyMapInputFile;
    this.propertyMapOutputFile = propertyMapOutputFile;
    this.sourceMapBaseUrl = sourceMapBaseUrl;
    this.sourceMapOutputName = sourceMapOutputName;
    this.cssInputs = ImmutableList.copyOf(cssInputs);
    this.cssDefines = ImmutableSet.copyOf(cssDefines);
    this.allowedUnrecognizedProperties = ImmutableList.copyOf(
        allowedUnrecognizedProperties);
    this.allowedNonStandardCssFunctions = ImmutableList.copyOf(
        allowedNonStandardCssFunctions);
    this.gssFunctionMapProviderClassName = gssFunctionMapProviderClassName;
    this.cssOutputFile = cssOutputFile;
    this.cssOutputFormat = cssOutputFormat;
    this.errorStream = Preconditions.checkNotNull(errorStream);
    this.locationMappings = locationMappings;
    this.translationsDirectory = translationsDirectory;
    this.language = language;
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

  public boolean getSoyUseInjectedData() {
    return soyUseInjectedData;
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
   * Gets a Key to cache output files by.
   *
   * This should include any value with a ConfigOption.update
   * method, because those can change on a per-request basis.
   */
  public Object getCacheOutputKey() {
    return ImmutableMap.<String, Object>builder()
        .put("id", Strings.nullToEmpty(getId()))
        .put("mode", getCompilationMode())
        .put("level", getWarningLevel())
        .put("debug", debug)
        .put("pretty-print", prettyPrint)
        .put("print-input-delimeter", printInputDelimiter)
        .put("soy-use-injected-data", getSoyUseInjectedData())
        .put("css-output-format", getCssOutputFormat())
        .put("language", Strings.nullToEmpty(getLanguage()))
        .build();
  }

  public File getCacheOutputFile() {
      return cacheOutputFile;
  }

  /**
   * @return null if no output wrapper has been set
   */
  public String getOutputWrapper() {
    return outputWrapper;
  }

  /**
   * @return A complete output wrapper, including the wrapper for global re-scoping.
   */
  public String getOutputAndGlobalScopeWrapper(boolean isRootModule, String moduleName, String sourceUrl) {
    String outputWrapper = getOutputWrapper();
    String outputWrapperMarker = getOutputWrapperMarker();
    if (Strings.isNullOrEmpty(outputWrapper)) {
      outputWrapper = outputWrapperMarker;
    }

    boolean hasGlobalScopeName =
        !Strings.isNullOrEmpty(getGlobalScopeName()) &&
        getCompilationMode() != CompilationMode.WHITESPACE;

    if (hasGlobalScopeName) {
      // Initialize the global scope if not initialized yet.
      String globalScopeNameWrapper = "";
      if (isRootModule) {
        globalScopeNameWrapper += "var " + getGlobalScopeName() + "={};";
      }
      globalScopeNameWrapper +=
          "(function(" + GLOBAL_SCOPE_NAME + "){\n" +
          outputWrapperMarker +
          "}).call(this, " + getGlobalScopeName() + ");";
      outputWrapper = outputWrapper.replace(outputWrapperMarker, globalScopeNameWrapper);
    }

    // http://code.google.com/p/closure-library/issues/detail?id=196
    // http://blog.getfirebug.com/2009/08/11/give-your-eval-a-name-with-sourceurl/
    // non-root modules are loaded with eval, give it a sourceURL for better debugging
    if (!isRootModule && !Strings.isNullOrEmpty(sourceUrl)) {
      outputWrapper += "\n//# sourceURL=" + sourceUrl;
    }

    String sourceMapFileName = getSourceMapOutputName().replace("%s", moduleName);
    if (!Strings.isNullOrEmpty(getSourceMapBaseUrl())) {
      try {
        outputWrapper += "\n//# sourceMappingURL=" +
            new URI(getSourceMapBaseUrl()).resolve(sourceMapFileName).toString();
      } catch (URISyntaxException e) {
        // ignore
      }
    }

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
   * Check if any of the files in the ancestor chain have changed
   * since this timestamp.
   */
  public boolean hasChangedSince(long timestamp) {
    for (FileWithLastModified file : configFileInheritanceChain) {
      if (file.file.lastModified() > timestamp) {
        return true;
      }
    }
    return false;
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
  List<WarningsGuardFactory> getCustomWarningsGuards() {
    return customWarningsGuards;
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

  public String getSourceMapBaseUrl() {
    return sourceMapBaseUrl;
  }

  public String getSourceMapOutputName() {
    if (Strings.isNullOrEmpty(sourceMapOutputName)) {
      if (hasModules()) {
        return getId() + "_%s.map";
      } else {
        return getId() + ".map";
      }
    }
    return sourceMapOutputName;
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

  public Set<String> getCssDefines() {
    return cssDefines;
  }

  public List<String> getAllowedUnrecognizedProperties() {
    return allowedUnrecognizedProperties;
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

  public JobDescription.OutputFormat getCssOutputFormat() {
    return cssOutputFormat;
  }

  public PrintStream getErrorStream() {
    return errorStream;
  }

  public List<WebDriverFactory> getWebDriverFactories() {
    return ImmutableList.copyOf(testDrivers);
  }

  public File getTranslationsDirectory() {
    return translationsDirectory;
  }

  public String getLanguage() {
    return language;
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

  LanguageMode getLanguageIn() {
    return languageIn;
  }

  public PlovrCompilerOptions getCompilerOptions(
      PlovrClosureCompiler compiler) {
    Preconditions.checkArgument(compilationMode != CompilationMode.RAW,
        "Cannot compile using RAW mode");
    CompilationLevel level = compilationMode.getCompilationLevel();
    PlovrCompilerOptions options = new PlovrCompilerOptions();

    options.setTreatWarningsAsErrors(getTreatWarningsAsErrors());

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
    options.setOutputCharset(getOutputCharset());

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
    options.setAmbiguateProperties(ambiguateProperties);
    options.setDisambiguateProperties(disambiguateProperties);
    if (languageIn != null) {
      options.setLanguageIn(languageIn);
    }
    if (languageOut != null) {
      options.setLanguageOut(languageOut);
    }
    options.setNewTypeInference(newTypeInference);

    // Instantiate custom warnings guards.
    for (WarningsGuardFactory factory : customWarningsGuards) {
      options.addWarningsGuard(factory.createWarningsGuard());
    }

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
    }

    if (!Strings.isNullOrEmpty(globalScopeName)) {
      Preconditions.checkState(
          options.collapseAnonymousFunctions == true ||
          level != CompilationLevel.ADVANCED_OPTIMIZATIONS,
          "For reasons unknown, setting this to false ends up " +
          "with a fairly larger final output, even though we just go " +
          "and re-anonymize the functions a few steps later.");
      options.renamePrefixNamespace = GLOBAL_SCOPE_NAME;
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
        options.setInputVariableMap(VariableMap.load(
            variableMapInputFile.getAbsolutePath()));
      } catch (IOException e) {
        logger.severe("The variable map input file '" + variableMapInputFile +
                      "' could not be loaded: " + e.getMessage());
      }
    }

    if (propertyMapInputFile != null) {
      try {
        options.setInputPropertyMap(VariableMap.load(
            propertyMapInputFile.getAbsolutePath()));
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
      tempFile.deleteOnExit();
      options.sourceMapOutputPath = tempFile.getAbsolutePath();
    } catch (IOException e) {
      logger.severe("A temp file for the Source Map could not be created");
    }

    options.setExternExports(true);

    File translationsFile = getTranslationsFile(translationsDirectory, language);
    if (translationsFile != null) {
      try {
        if (translationsFile.getName().endsWith(".xtb")) {
          options.setMessageBundle(
              new XtbMessageBundle(new FileInputStream(translationsFile), null));
        } else {
          options.setMessageBundle(
              new XliffMessageBundle(new FileInputStream(translationsFile), null));
        }
      } catch (IOException e) {
        logger.severe("Unable to load translations file: " + e.getMessage());
      }
    }

    // Add location mapping for paths in source map.
    options.setSourceMapLocationMappings(locationMappings);

    if (getTreatWarningsAsErrors()) {
      options.addWarningsGuard(new StrictWarningsGuard());
    }

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
        options.getCustomPasses();
    if (customPasses == null) {
      customPasses = ArrayListMultimap.create();
      options.setCustomPasses(customPasses);
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
            try {
              Method setter = PlovrCompilerOptions.class.getMethod(setterName, Charset.class);
              setter.invoke(options, Charset.forName(primitive.getAsString()));
              continue;
            } catch (NoSuchMethodException e2) {
              // Ignore exception and try setting value as an enum instead.
              if (setCompilerOptionToEnumValue(
                      options, setterName, primitive.getAsString())) {
                continue;
              }
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

    private final List<ConfigPath> paths = Lists.newArrayList();

    private final List<File> testExcludePaths;

    /** List of (file, path) pairs for inputs */
    private final List<Pair<File, String>> inputs = Lists.newArrayList();
    private final List<JsInput> jsInputs = Lists.newArrayList();

    private List<String> externs = null;

    private List<JsInput> builtInExterns = null;

    private List<WebDriverFactory> testDrivers = Lists.newArrayList();

    private File testTemplate = null;

    private ImmutableList.Builder<String> soyFunctionPlugins = null;

    private String soyTranslationPlugin = "";

    private boolean soyUseInjectedData = false;

    private ListMultimap<CustomPassExecutionTime, CompilerPassFactory> customPasses = ImmutableListMultimap.of();

    private ImmutableList.Builder<WarningsGuardFactory> customWarningsGuards = ImmutableList.builder();

    private File documentationOutputDirectory = null;

    private boolean customExternsOnly = false;

    private CompilationMode compilationMode = CompilationMode.SIMPLE;

    private WarningLevel warningLevel = WarningLevel.DEFAULT;

    private boolean debug = false;

    private boolean prettyPrint = false;

    private boolean printInputDelimiter = false;

    private File outputFile = null;

    private File cacheOutputFile = null;

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

    private LanguageMode languageIn;

    private LanguageMode languageOut;

    private boolean newTypeInference;

    private JsonObject experimentalCompilerOptions;

    private String globalScopeName;

    private File variableMapInputFile;

    private File variableMapOutputFile;

    private File propertyMapInputFile;

    private File propertyMapOutputFile;

    private String sourceMapBaseUrl;

    private String sourceMapOutputName;

    private final Map<String, JsonPrimitive> defines;

    /************************* CSS OPTIONS *************************/

    private List<File> cssInputs = Lists.newArrayList();

    private Set<String> cssDefines = Sets.newHashSet();

    private List<String> allowedUnrecognizedProperties = Lists.newArrayList();

    private List<String> allowedNonStandardFunctions = Lists.newArrayList();

    private String gssFunctionMapProviderClassName;

    private File cssOutputFile = null;

    private File translationsDirectory = null;

    private String language = null;

    private JobDescription.OutputFormat cssOutputFormat = JobDescription.OutputFormat.PRETTY_PRINTED;

    private PrintStream errorStream = System.err;

    private List<LocationMapping> locationMappings = Lists.newArrayList();

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
      this.soyTranslationPlugin = config.soyTranslationPlugin;
      this.soyUseInjectedData = config.soyUseInjectedData;
      this.customPasses = config.customPasses;
      this.customWarningsGuards = new ImmutableList.Builder<WarningsGuardFactory>()
        .addAll(config.customWarningsGuards);
      this.documentationOutputDirectory = config.documentationOutputDirectory;
      this.compilationMode = config.compilationMode;
      this.warningLevel = config.warningLevel;
      this.debug = config.debug;
      this.prettyPrint = config.prettyPrint;
      this.printInputDelimiter = config.printInputDelimiter;
      this.outputFile = config.outputFile;
      this.outputWrapper = config.outputWrapper;
      this.outputCharset = config.outputCharset;
      this.cacheOutputFile = config.cacheOutputFile;
      this.fingerprintJsFiles = config.fingerprintJsFiles;
      this.checkLevelsForDiagnosticGroups = config.checkLevelsForDiagnosticGroups;
      this.exportTestFunctions = config.exportTestFunctions;
      this.treatWarningsAsErrors = config.treatWarningsAsErrors;
      this.stripNameSuffixes = config.stripNameSuffixes;
      this.stripTypePrefixes = config.stripTypePrefixes;
      this.idGenerators = config.idGenerators;
      this.ambiguateProperties = config.ambiguateProperties;
      this.disambiguateProperties = config.disambiguateProperties;
      this.languageIn = config.languageIn;
      this.languageOut = config.languageOut;
      this.newTypeInference = config.newTypeInference;
      this.experimentalCompilerOptions = config.experimentalCompilerOptions;
      this.globalScopeName = config.globalScopeName;
      this.variableMapInputFile = config.variableMapInputFile;
      this.variableMapOutputFile = config.variableMapOutputFile;
      this.propertyMapInputFile = config.propertyMapInputFile;
      this.propertyMapOutputFile = config.propertyMapOutputFile;
      this.sourceMapBaseUrl = config.sourceMapBaseUrl;
      this.sourceMapOutputName = config.sourceMapOutputName;
      this.defines = Maps.newHashMap(config.defines);
      this.cssInputs = Lists.newArrayList(config.cssInputs);
      this.cssDefines = Sets.newHashSet(config.cssDefines);
      this.allowedUnrecognizedProperties = Lists.newArrayList(
          config.allowedUnrecognizedProperties);
      this.allowedNonStandardFunctions = Lists.newArrayList(
          config.allowedNonStandardCssFunctions);
      this.gssFunctionMapProviderClassName = config.
          gssFunctionMapProviderClassName;
      this.cssOutputFile = config.cssOutputFile;
      this.translationsDirectory = config.translationsDirectory;
      this.language = config.language;
      this.cssOutputFormat = config.cssOutputFormat;
      this.errorStream = config.errorStream;
    }

    /** Directory against which relative paths should be resolved. */
    public File getRelativePathBase() {
      return this.relativePathBase;
    }

    public Builder setId(String id) {
      Preconditions.checkNotNull(id);
      Preconditions.checkArgument(ID_PATTERN.matcher(id).matches(),
              String.format("Not a valid config id: %s", id));
      this.id = id;
      return this;
    }

    public void addPath(ConfigPath path) {
      Preconditions.checkNotNull(path);
      paths.add(path);
    }

    public void resetPaths() {
      paths.clear();
    }

    public Builder addInput(File file, String name) {
      Preconditions.checkNotNull(file);
      Preconditions.checkNotNull(name);
      inputs.add(Pair.of(file, name));
      return this;
    }

    public Builder addInput(JsInput input) {
      jsInputs.add(input);
      return this;
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

    public void addTestDriverFactory(WebDriverFactory factory) {
      testDrivers.add(factory);
    }

    public void resetTestDrivers() {
      testDrivers.clear();
    }

    public void setTestTemplate(File testTemplate) {
      this.testTemplate = testTemplate;
    }

    public void addTestExcludePath(final File testExcludePath) {
      Preconditions.checkNotNull(testExcludePath);

      ConfigPath pathThatContainsExclude = Iterables.find(paths,
          new Predicate<ConfigPath>() {
        @Override
        public boolean apply(ConfigPath path) {
          return FileUtil.contains(path.getFile(), testExcludePath);
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

    /**
     * Sets a plugin for translation with templates.
     *
     * By default, we expect Closure Compiler to do translation.
     *
     * If a plugin is set, we'll use that to load the message bundle and
     * translate the messages instead.
     *
     * Be sure that you're installing the plugin module with addSoyFunctionPlugin.
     * The Xliff plugin module is installed by default.
     *
     * <pre>
     *   setSoyTranslationPlugin("com.google.template.soy.xliffmsgplugin.XliffMsgPlugin")
     * </pre>
     *
     * @param name the plugin class name
     */
    public void setSoyTranslationPlugin(String name) {
      soyTranslationPlugin = name;
    }

    public void setDocumentationOutputDirectory(File documentationOutputDirectory) {
      Preconditions.checkNotNull(documentationOutputDirectory);
      this.documentationOutputDirectory = documentationOutputDirectory;
    }

    public void setSoyUseInjectedData(boolean soyUseInjectedData) {
      this.soyUseInjectedData = soyUseInjectedData;
    }

    public void setCustomPasses(
        ListMultimap<CustomPassExecutionTime, CompilerPassFactory> customPasses) {
      this.customPasses = ImmutableListMultimap.copyOf(customPasses);
    }

    public void resetCustomPasses() {
      this.customPasses = null;
    }

    public void addCustomWarningsGuard(String className) {
      Class<?> clazz;
      try {
        clazz = Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      this.customWarningsGuards.add(new WarningsGuardFactory(clazz));
    }

    public void resetCustomWarningsGuards() {
      this.customWarningsGuards = ImmutableList.builder();
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

    public Builder setCompilationMode(CompilationMode mode) {
      Preconditions.checkNotNull(mode);
      this.compilationMode = mode;
      return this;
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

    public void setCacheOutputFile(File cacheOutputFile) {
      this.cacheOutputFile = cacheOutputFile;
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

    public void setLocationMappings(JsonObject locationMappings) {
      if ( locationMappings != null ) {
        Set<Map.Entry<String, JsonElement>> entries = locationMappings.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
          String prefix = entry.getKey();
          String replacement = entry.getValue().getAsString();
          replacement = replacement.replace("%s", id);
          this.locationMappings.add(new LocationMapping(prefix, replacement));
        }
      }
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

    public void setLanguageIn(LanguageMode newVal) {
      this.languageIn = newVal;
    }

    public void setLanguageOut(LanguageMode newVal) {
      this.languageOut = newVal;
    }

    public void setNewTypeInference(boolean newVal) {
      this.newTypeInference = newVal;
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

    public void setSourceMapBaseUrl(String sourceMapBaseUrl) {
      this.sourceMapBaseUrl = sourceMapBaseUrl;
    }

    public void setSourceMapOutputName(String sourceMapOutputName) {
      this.sourceMapOutputName = sourceMapOutputName;
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

    public void addCssDefine(String define) {
      cssDefines.add(define);
    }

    public void resetCssDefines() {
      cssDefines.clear();
    }

    public void addAllowedNonStandardCssFunction(String function) {
      allowedNonStandardFunctions.add(function);
    }

    public void resetAllowedNonStandardCssFunctions() {
      allowedNonStandardFunctions.clear();
    }

    public void addAllowedUnrecognizedProperty(String property) {
      allowedUnrecognizedProperties.add(property);
    }

    public void resetAllowedUnrecognizedProperties() {
      allowedUnrecognizedProperties.clear();
    }

    public void setGssFunctionMapProvider(
        String gssFunctionMapProviderClassName) {
      this.gssFunctionMapProviderClassName = gssFunctionMapProviderClassName;
    }

    public void setCssOutputFile(File cssOutputFile) {
      this.cssOutputFile = cssOutputFile;
    }

    public void setTranslationsDirectory(File translationsDirectory) {
      this.translationsDirectory = translationsDirectory;
    }

    public void setLanguage(String language) {
      this.language = language;
    }

    public void setCssOutputFormat(JobDescription.OutputFormat cssOutputFormat) {
      this.cssOutputFormat = cssOutputFormat;
    }

    public void setErrorStream(PrintStream errorStream) {
      this.errorStream = Preconditions.checkNotNull(errorStream);
    }

    private SoyMsgBundle getSoyMsgBundle() {
      if (soyTranslationPlugin.isEmpty()) {
        return null;
      }

      File file = getTranslationsFile(translationsDirectory, language);
      if (file == null) {
        return null;
      }

      try {
        Class<?> msgPluginClass = Class.forName(soyTranslationPlugin);
        Injector injector = SoyFile.createInjector(createSoyFunctionPluginNames());
        Object msgPlugin = injector.getInstance(msgPluginClass);
        return new SoyMsgBundleHandler((SoyMsgPlugin) msgPlugin).createFromFile(file);
      } catch (ClassNotFoundException e) {
        throw Throwables.propagate(e);
      } catch (IOException e) {
        throw Throwables.propagate(e);
      } catch (SoyMsgException e) {
        throw Throwables.propagate(e);
      }
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

        SoyFileOptions soyFileOptions = new SoyFileOptions.Builder()
            .setPluginModuleNames(soyFunctionNames)
            .setUseClosureLibrary(!this.excludeClosureLibrary)
            .setIsUsingInjectedData(this.soyUseInjectedData)
            .setMsgBundle(getSoyMsgBundle())
            .build();

        manifest = new Manifest(
            excludeClosureLibrary,
            closureLibraryDirectory,
            paths,
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
          testDrivers,
          moduleConfig,
          testTemplate,
          testExcludePaths,
          soyFunctionNames,
          soyTranslationPlugin,
          this.soyUseInjectedData,
          compilationMode,
          warningLevel,
          debug,
          prettyPrint,
          printInputDelimiter,
          outputFile,
          outputWrapper,
          outputCharset,
          cacheOutputFile,
          fingerprintJsFiles,
          checkLevelsForDiagnosticGroups,
          exportTestFunctions,
          treatWarningsAsErrors,
          defines,
          customPasses,
          customWarningsGuards.build(),
          documentationOutputDirectory,
          stripNameSuffixes,
          stripTypePrefixes,
          idGenerators,
          ambiguateProperties,
          disambiguateProperties,
          languageIn,
          languageOut,
          newTypeInference,
          experimentalCompilerOptions,
          configFileInheritanceChain,
          globalScopeName,
          variableMapInputFile,
          variableMapOutputFile,
          propertyMapInputFile,
          propertyMapOutputFile,
          sourceMapBaseUrl,
          sourceMapOutputName,
          cssInputs,
          cssDefines,
          allowedUnrecognizedProperties,
          allowedNonStandardFunctions,
          gssFunctionMapProviderClassName,
          cssOutputFile,
          cssOutputFormat,
          errorStream,
          locationMappings,
          translationsDirectory,
          language);

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

      jsInputs.addAll(this.jsInputs);

      return jsInputs;
    }

    private List<String> createSoyFunctionPluginNames() {
      if (this.soyFunctionPlugins == null) {
        return ImmutableList.of(XliffMsgPluginModule.class.getName());
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

  private static File getTranslationsFile(File translationsDirectory, final String language) {
    if (translationsDirectory == null || language == null) {
      return null;
    }

    File[] files = translationsDirectory.listFiles(new FilenameFilter() {
        @Override public boolean accept(File dir, String name) {
          return name.startsWith(language) && (
              name.endsWith(".xtb") ||
              name.endsWith(".xliff") ||
              name.endsWith(".xlf"));
        }
    });
    if (files.length == 0) {
      logger.severe("Unable to find translations file for " + language);
      return null;
    } else {
      return files[0];
    }
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
