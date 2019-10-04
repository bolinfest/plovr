package org.plovr;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.plovr.io.Files;
import org.plovr.io.Streams;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.GoogleJsMessageIdGenerator;
import com.google.javascript.jscomp.JsMessage;
import com.google.javascript.jscomp.JsMessageExtractor;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.PlovrCompilerOptions;
import com.google.javascript.jscomp.PrintStreamErrorManager;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceExcerptProvider;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.SourceMap;

/**
 * {@link Compilation} represents a compilation performed by the Closure
 * Compiler.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class Compilation {

  private static final Logger logger = Logger.getLogger("org.plovr.Compilation");

  private Config config;

  private final List<SourceFile> externs;
  private final List<SourceFile> inputs;
  private final List<JSModule> modules;
  private final Map<String, JSModule> nameToModule;

  private Compiler compiler;
  private Result result;

  /**
   * This is only assigned if compile() is called with a {@link Config} that is
   * in {@link CompilationMode#RAW}.
   */
  private String inputJsConcatenatedInOrder;

  private Compilation(Config config, List<SourceFile> externs,
      List<SourceFile> inputs, List<JSModule> modules) {
    this.config = config;
    this.externs = ImmutableList.copyOf(externs);
    this.inputs = inputs == null ? null : ImmutableList.copyOf(inputs);
    this.modules = modules == null ? null : ImmutableList.copyOf(modules);

    ImmutableMap.Builder<String, JSModule> nameToModuleBuilder;
    if (modules == null) {
      nameToModuleBuilder = null;
    } else {
      nameToModuleBuilder = ImmutableMap.builder();
      for (JSModule module : modules) {
        nameToModuleBuilder.put(module.getName(), module);
      }
    }
    this.nameToModule = (nameToModuleBuilder == null) ? null :
        nameToModuleBuilder.build();
  }

  public static Compilation create(List<SourceFile> externs,
      List<SourceFile> inputs) {
    return new Compilation(null, externs, inputs, null);
  }

  public static Compilation createForModules(List<SourceFile> externs,
      List<JSModule> modules) {
    return new Compilation(null, externs, null, modules);
  }

  /**
   * Creates an object for managing a compilation
   *
   * We need to be able to parse all the JS files to find all the dependencies,
   * so if we can't parse them, we will throw a compilation exception
   */
  public static Compilation create(Config config) throws CompilationException {
    // Generate all the code upfront, and track any syntax errors in individual
    // generated files.
    Set<JsInput> allDependencies = config.getManifest().getAllDependencies();
    List<CompilationException> errors = new ArrayList<>();
    for (JsInput input : allDependencies) {
      try {
        input.getCode();
      } catch (Throwable e) {
        errors.add(toCheckedException(e));
      }
    }

    if (!errors.isEmpty()) {
      throw new CompilationException.Multi(errors);
    }

    try {
      PlovrClosureCompiler dummyCompiler = new PlovrClosureCompiler(config.getErrorStream());
      Compilation compilation = config.getManifest().getCompilerArguments(
          config.getModuleConfig(), config.getCompilerOptions(dummyCompiler));
      compilation.setConfig(config);
      return compilation;
    } catch (Throwable e) {
      throw toCheckedException(e);
    }
  }

  private static CompilationException toCheckedException(Throwable e) {
    if (e instanceof PlovrCoffeeScriptCompilerException) {
      return new CheckedCoffeeScriptCompilerException((PlovrCoffeeScriptCompilerException) e);
    }
    throw Throwables.propagate(e);
  }

  /**
   * Sets the config for the compilation.
   *
   * This method is intended only for legacy callers.
   * Most callers should use Compilation.create(), which will take care of this
   *
   * Over time, the compiler has moved towards auto-discovery of inputs from the config,
   * rather than setting the inputs first and the config later.
   */
  void setConfig(Config c) {
    Preconditions.checkState(config == null, "Config already initialized");
    config = c;
  }

  /**
   * Extracts the i18n messages from this compilation job.
   * You do not need to run a full compile to get the messages.
   */
  public Iterable<JsMessage> extractMessages() throws CompilationException {
    PlovrClosureCompiler compiler = new PlovrClosureCompiler(config.getErrorStream());
    JsMessageExtractor extractor =
        new JsMessageExtractor(
            new GoogleJsMessageIdGenerator(null),
            JsMessage.Style.CLOSURE,
            config.getCompilerOptions(compiler),
            false
        );

    return extractor.extractMessages(inputs);
  }

  public void compile(Config c) throws CompilationException {
    setConfig(c);
    compile();
  }

  public void compile() throws CompilationException {
    Preconditions.checkNotNull(config, "Config not initialized. Please use the compile(Config) method");
    try {
      if (config.getCompilationMode() == CompilationMode.RAW) {
        compileRaw();
      } else {
        PlovrClosureCompiler compiler = new PlovrClosureCompiler(config.getErrorStream());
        compile(compiler, config.getCompilerOptions(compiler));
      }
    } catch (Throwable t) {
      throw toCheckedException(t);
    }
  }

  private void compileRaw() {
    Preconditions.checkArgument(
        config.getCompilationMode() == CompilationMode.RAW,
        "Config must be in RAW mode");
    StringBuilder builder = new StringBuilder();
    for (SourceFile input : inputs) {
      try {
        builder.append(input.getCode());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    inputJsConcatenatedInOrder = builder.toString();

    // Need to have a dummy Result that appears to be a success (i.e., has no
    // errors or warnings).
    Compiler dummyCompiler = new Compiler();
    dummyCompiler.setErrorManager(new PrintStreamErrorManager(System.out));
    this.result = dummyCompiler.getResult();
  }

  /**
   * For now, this method is private so that the client does not get access to
   * the Compiler that was used.
   */
  private void compile(Compiler compiler,
      PlovrCompilerOptions options) {
    Preconditions.checkState(!hasResult(), "Compilation already occurred");
    this.compiler = compiler;

    if (modules == null) {
      this.result = compiler.compile(externs, inputs, options);
    } else {
      this.result = compiler.compileModules(externs, modules, options);
    }
  }

  public boolean usesModules() {
    return modules != null;
  }

  public boolean hasResult() {
    return getResult() != null;
  }

  public Result getResult() {
    return this.result;
  }

  public String getCompiledCode() {
    Preconditions.checkState(hasResult(), "Code has not been compiled yet");

    String compiledCode = inputJsConcatenatedInOrder != null ?
        inputJsConcatenatedInOrder : compiler.toSource();

    String sourceUrl = config.getOutputFile() == null ? "" : config.getOutputFile().toString();
    String outputWrapper = config.getOutputAndGlobalScopeWrapper(true, "", sourceUrl);
    return interpolateOutputWrapper(compiledCode, outputWrapper);
  }

  public String getRootModuleName() {
    return config.getModuleConfig().getRootModule();
  }

  public String getCodeForModule(String moduleName, boolean isDebugMode,
      Function<String, String> moduleNameToUri) {
    final boolean resetSourceMap = true;
    return getCodeForModule(moduleName, isDebugMode, moduleNameToUri, resetSourceMap);
  }

  /**
   * This method always calls compiler.toSource(module), which means that it can
   * be followed by a call to compiler.getSourceMap().appendTo(writer, module),
   * though compiler.getSourceMap().reset() should be called immediately after
   * when that is the case.
   *
   * This method is sychronized in order to fix
   * http://code.google.com/p/plovr/issues/detail?id=31.
   * The problem is that if two modules are requested at the same time,
   * it is often the case that compiler.toSource(module) is executing for the
   * second module while the source map is still being modified for the first
   * module, causing an IllegalStateException. Empirically, synchronizing this
   * method appears to fix things, though a more provably correct (and minimal)
   * solution should be sought.
   */
  private synchronized String getCodeForModule(
      String moduleName,
      boolean isDebugMode,
      Function<String, String> moduleNameToUri,
      boolean resetSourceMap) {
    Preconditions.checkState(hasResult(), "Code has not been compiled yet");
    Preconditions.checkState(modules != null,
        "This compilation does not use modules");

    JSModule module = nameToModule.get(moduleName);
    String moduleCode = compiler.toSource(module);

    String outputWrapper = getModuleOutputWrapper(moduleName, isDebugMode, moduleNameToUri);
    String result = interpolateOutputWrapper(moduleCode, outputWrapper);

    if (resetSourceMap) {
      SourceMap sourceMap = compiler.getSourceMap();
      if (sourceMap != null) sourceMap.reset();
    }

    return result;
  }

  String getModuleOutputWrapper(
      String moduleName,
      boolean isDebugMode,
      Function<String, String> moduleNameToUri) {
    StringBuilder rootModuleInfoBuilder = new StringBuilder();
    ModuleConfig moduleConfig = config.getModuleConfig();
    String rootModule = moduleConfig.getRootModule();
    boolean isRootModule = rootModule.equals(moduleName);

    if (isRootModule) {
      // For the root module, prepend the following global variables:
      //
      // PLOVR_MODULE_INFO
      // PLOVR_MODULE_URIS
      // PLOVR_MODULE_USE_DEBUG_MODE
      //
      // Because the standard way to read these variables in the application is:
      //
      // moduleLoader.setDebugMode(!!goog.global['PLOVR_MODULE_USE_DEBUG_MODE']);
      // moduleManager.setLoader(moduleLoader);
      // moduleManager.setAllModuleInfo(goog.global['PLOVR_MODULE_INFO']);
      // moduleManager.setModuleUris(goog.global['PLOVR_MODULE_URIS']);
      //
      // It is important that the PLOVR variables are guaranteed to be global,
      // which (as much as it pains me) is why "var" is omitted.
      if (!moduleConfig.excludeModuleInfoFromRootModule()) {
        try {
          appendRootModuleInfo(rootModuleInfoBuilder, isDebugMode, moduleNameToUri);
        } catch (IOException e) {
          // This should not occur because data is being appended to an
          // in-memory StringBuilder rather than a file.
          throw new RuntimeException(e);
        }
      }
    }
    String sourceUrl = moduleNameToUri.apply(moduleName);
    return rootModuleInfoBuilder.toString() +
        config.getOutputAndGlobalScopeWrapper(isRootModule, moduleName, sourceUrl);
  }

  private String interpolateOutputWrapper(String code, String outputWrapper) {
    String outputWrapperMarker = config.getOutputWrapperMarker();
    if (outputWrapper.equals(outputWrapperMarker)) {
      return code;
    }

    int pos = outputWrapper.indexOf(outputWrapperMarker);
    if (pos >= 0) {
      String prefix = outputWrapper.substring(0, pos);
      String suffix = outputWrapper.substring(pos + outputWrapperMarker.length());
      SourceMap sourceMap = compiler.getSourceMap();
      if (sourceMap != null) {
        sourceMap.setWrapperPrefix(prefix);
      }
      return prefix + code + suffix;
    } else {
      throw new RuntimeException(
          "output-wrapper did not contain placeholder: " +
          outputWrapperMarker);
    }
  }

  public void appendRootModuleInfo(Appendable appendable, boolean isDebugMode,
      Function<String, String> moduleNameToUri) throws IOException {
    ModuleConfig moduleConfig = config.getModuleConfig();
    JsonObject plovrModuleInfo = createModuleInfo(moduleConfig);
    appendable.append("PLOVR_MODULE_INFO=").
        append(plovrModuleInfo.toString()).append(";\n");
    JsonObject plovrModuleUris = createModuleUris(moduleConfig,
        moduleNameToUri);
    appendable.append("PLOVR_MODULE_URIS=").
        append(plovrModuleUris.toString()).append(";\n");
    appendable.append("PLOVR_MODULE_USE_DEBUG_MODE=" + isDebugMode + ";\n");
  }

  public String getCodeForRootModule(boolean isDebugMode,
      Function<String, String> moduleNameToUri) {
    return getCodeForModule(getRootModuleName(), isDebugMode, moduleNameToUri);
  }

  /**
   * Writes out all of the module files. This method is only applicable when
   * modules are used. This is expected to be used only with the build command.
   * @throws IOException
   */
  public void writeCompiledCodeToFiles(
      final Function<String, String> moduleNameToUri, String sourceMapPath)
      throws IOException {
    if (modules == null) {
      throw new IllegalStateException("This compilation does not use modules");
    }

    ModuleConfig moduleConfig = config.getModuleConfig();
    Map<String, File> moduleToOutputPath = moduleConfig.getModuleToOutputPath();
    final Map<String, String> moduleNameToFingerprint = Maps.newHashMap();
    final boolean isDebugMode = false;

    if (sourceMapPath != null) {
        new File(sourceMapPath).mkdirs();
    }

    for (JSModule module : modules) {
      String moduleName = module.getName();
      File outputFile = moduleToOutputPath.get(moduleName);
      com.google.common.io.Files.createParentDirs(outputFile);

      // Reset the source map if it is not going to be reset later in this loop
      // when the source map is written to disk.
      final boolean resetSourceMap = (sourceMapPath == null);
      String moduleCode = getCodeForModule(
          moduleName, isDebugMode, moduleNameToUri, resetSourceMap);

      // Fingerprint the file, if appropriate.
      if (config.shouldFingerprintJsFiles()) {
        String fileName = outputFile.getName();
        String fingerprint = Md5Util.hashJs(moduleCode);
        moduleNameToFingerprint.put(moduleName, fingerprint);
        fileName = insertFingerprintIntoName(fileName, fingerprint);
        outputFile = new File(outputFile.getParentFile(), fileName);
      }

      Files.write(moduleCode, outputFile);

      // It turns out that the SourceMap will not be populated until after the
      // Compiler's internal representation has been output as source code, so
      // it should only be written out to a file after the compiled code has
      // been generated.
      if (sourceMapPath != null) {
        String sourceMapFileName = config.getSourceMapOutputName().replace("%s", moduleName);
        Writer writer = Streams.createFileWriter(
            new File(sourceMapPath, sourceMapFileName).getPath(), config);
        // This is safe because getCodeForModule() was just called, which has
        // the side-effect of calling compiler.toSource(module).
        SourceMap sourceMap = compiler.getSourceMap();
        sourceMap.appendTo(writer, moduleName);
        sourceMap.reset();
        Closeables.close(writer, false);
      }
    }

    if (moduleConfig.excludeModuleInfoFromRootModule()) {
      File outputFile = moduleConfig.getModuleInfoPath();
      com.google.common.io.Files.createParentDirs(outputFile);

      final Function<String, String> fingerprintedModuleNameToUri =
          new Function<String, String>() {
            @Override
            public String apply(String moduleName) {
              String uri = moduleNameToUri.apply(moduleName);
              String fingerprint = moduleNameToFingerprint.get(moduleName);
              if (fingerprint != null) {
                uri = insertFingerprintIntoName(uri, fingerprint);
              }
              return uri;
            }
      };

      Writer writer = Streams.createFileWriter(outputFile, config);
      appendRootModuleInfo(writer, isDebugMode, fingerprintedModuleNameToUri);
      Closeables.close(writer, false);
    }
  }

  /**
   * This takes a file path and inserts a fingerprint into its name. Currently,
   * there is no support for including a token to indicate where the fingerprint
   * should be inserted, so the fingerprint is inserted according to the
   * following rule:
   * <ul>
   *   <li>If the path contains a dot, then the fingerprint is inserted before
   *       the final dot. For example, if the path were "foo/bar_.js" and the
   *       fingerprint were "2XBD4C", then the fingerprinted path would be
   *       "foo/bar_2XBD4C.js".
   *   <li>If the path does not contain a dot, then the fingerprint is appended
   *       to the end of the path.
   * </ul>
   * @param filePath
   * @param fingerprint
   * @return
   */
  @VisibleForTesting
  static String insertFingerprintIntoName(String filePath, String fingerprint) {
    Preconditions.checkNotNull(filePath);
    Preconditions.checkNotNull(fingerprint);
    int index = filePath.lastIndexOf(".");
    if (index >= 0) {
      filePath = filePath.substring(0, index) + fingerprint +
          filePath.substring(index);
    } else {
      filePath += fingerprint;
    }
    return filePath;
  }

  /**
   * Creates the JSON needed to define the PLOVR_MODULE_INFO variable.
   * @param moduleConfig
   */
  static JsonObject createModuleInfo(ModuleConfig moduleConfig) {
    JsonObject obj = new JsonObject();
    for (String moduleName : moduleConfig.getModuleNames()) {
      JsonArray modulesThatMustBeLoadedFirst = new JsonArray();
      for (String module : moduleConfig.getModuleInfo(moduleName).getDeps()) {
        modulesThatMustBeLoadedFirst.add(new JsonPrimitive(module));
      }
      obj.add(moduleName, modulesThatMustBeLoadedFirst);
    }
    return obj;
  }

  /**
   * Creates the JSON needed to define the PLOVR_MODULE_URIS variable.
   */
  static JsonObject createModuleUris(ModuleConfig moduleConfig,
      Function<String, String> moduleNameToUri) {
    JsonObject obj = new JsonObject();
    for (String moduleName : moduleConfig.getModuleNames()) {
      obj.addProperty(moduleName, moduleNameToUri.apply(moduleName));
    }
    return obj;
  }

  public List<CompilationError> getCompilationErrors() {
    if (this.result == null) {
      throw new IllegalStateException("Compilation has not occurred yet");
    }
    return normalizeErrors(result.errors, compiler);
  }

  public List<CompilationError> getCompilationWarnings() {
    if (this.result == null) {
      throw new IllegalStateException("Compilation has not occurred yet");
    }
    return normalizeErrors(result.warnings, compiler);
  }

  private static List<CompilationError> normalizeErrors(com.google.common.collect.ImmutableList<JSError>errors,
                                                        SourceExcerptProvider sourceExcerptProvider) {
    List<CompilationError> compilationErrors = Lists.newLinkedList();
    for (JSError error : errors) {
      compilationErrors.add(new CompilationError(error, sourceExcerptProvider));
    }
    return compilationErrors;
  }

  /**
   * @return null if the code was compiled in {@link CompilationMode#RAW} mode
   */
  public @Nullable Double getTypedPercent() {
    if (this.inputJsConcatenatedInOrder != null) {
      return null;
    } else {
      return compiler.getErrorManager().getTypedPercent();
    }
  }

  @VisibleForTesting
  public List<SourceFile> getInputs() {
    return inputs;
  }

  @Override
  public String toString() {
    return "Inputs: " + inputs + "; Externs: " + externs + "; Modules: " +
        modules;
  }

}
