package org.plovr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.inject.internal.ImmutableMap;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceExcerptProvider;

/**
 * {@link Compilation} represents a compilation performed by the Closure
 * Compiler.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class Compilation {

  private final List<JSSourceFile> externs;
  private final List<JSSourceFile> inputs;
  private final List<JSModule> modules;
  private final Map<String, JSModule> nameToModule;

  private Config config;
  private Compiler compiler;
  private Result result;

  private Compilation(List<JSSourceFile> externs,
      List<JSSourceFile> inputs, List<JSModule> modules) {
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

  public static Compilation create(List<JSSourceFile> externs,
      List<JSSourceFile> inputs) {
    return new Compilation(externs, inputs, null);
  }

  public static Compilation createForModules(List<JSSourceFile> externs,
      List<JSModule> modules) {
    return new Compilation(externs, null, modules);
  }

  public void compile(Config config) {
    compile(config, new Compiler(), config.getCompilerOptions());
  }

  /**
   * For now, this method is private so that the client does not get access to
   * the Compiler that was used.
   */
  private void compile(Config config, Compiler compiler, CompilerOptions options) {
    if (this.result != null) {
      throw new IllegalStateException("Compilation already occurred");
    }
    this.config = config;
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
    if (!hasResult()) {
      throw new IllegalStateException("Code has not been compiled yet");
    }
    return compiler.toSource();
  }

  public String getRootModuleName() {
    return config.getModuleConfig().getRootModule();
  }

  public String getCodeForModule(String moduleName, boolean isDebugMode,
      Function<String, String> moduleNameToUri) {
    if (modules == null) {
      throw new IllegalStateException("This compilation does not use modules");
    }

    StringBuilder builder = new StringBuilder();
    ModuleConfig moduleConfig = config.getModuleConfig();
    String rootModule = moduleConfig.getRootModule();

    if (rootModule.equals(moduleName)) {
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
      JsonObject plovrModuleInfo = createModuleInfo(moduleConfig);
      builder.append("PLOVR_MODULE_INFO=").
          append(plovrModuleInfo.toString()).append(";\n");
      JsonObject plovrModuleUris = createModuleUris(moduleConfig,
          moduleNameToUri);
      builder.append("PLOVR_MODULE_URIS=").
          append(plovrModuleUris.toString()).append(";\n");
      builder.append("PLOVR_MODULE_USE_DEBUG_MODE=" + isDebugMode + ";\n");
    }

    JSModule module = nameToModule.get(moduleName);
    String moduleCode = compiler.toSource(module);
    builder.append(moduleCode);

    // http://code.google.com/p/closure-library/issues/detail?id=196
    // http://blog.getfirebug.com/2009/08/11/give-your-eval-a-name-with-sourceurl/
    // non-root modules are loaded with eval, give it a sourceURL for better debugging
    if (!rootModule.equals(moduleName)) {
        builder.append("\n//@ sourceURL=" + moduleNameToUri.apply(moduleName));
    }

    return builder.toString();
  }

  public String getCodeForRootModule(boolean isDebugMode,
      Function<String, String> moduleNameToUri) {
    return getCodeForModule(getRootModuleName(), isDebugMode, moduleNameToUri);
  }

  /**
   * Writes out all of the module files. This method is only applicable when
   * modules are used.
   * @throws IOException
   */
  public void writeCompiledCodeToFiles(Function<String, String> moduleNameToUri, String sourceMapPath)
      throws IOException {
    if (modules == null) {
      throw new IllegalStateException("This compilation does not use modules");
    }

    ModuleConfig moduleConfig = config.getModuleConfig();
    Map<String, File> moduleToOutputPath = moduleConfig.getModuleToOutputPath();
    final boolean isDebugMode = false;
    for (JSModule module : modules) {
      String moduleName = module.getName();
      File outputFile = moduleToOutputPath.get(moduleName);
      createParentDirs(outputFile);
      String moduleCode = getCodeForModule(moduleName, isDebugMode, moduleNameToUri);
      Files.write(moduleCode, outputFile, Charsets.UTF_8);

      // It turns out that the SourceMap will not be populated until after the
      // Compiler's internal representation has been output as source code, so
      // it should only be written out to a file after the compiled code has
      // been generated.
      if (sourceMapPath != null) {
        Writer writer = new BufferedWriter(new FileWriter(sourceMapPath + "_" + moduleName));
        this.result.sourceMap.appendTo(writer, moduleName);
        Closeables.closeQuietly(writer);
      }
    }
  }

  // TODO(bolinfest): Currently, this is copied from com.google.common.io.Files
  // because plovr is built in such a way that it copies different versions of
  // Guava from the Closure Compiler and Closure Templates projects. Once this
  // is cleaned up, plovr will compile against the newest Guava, which provides
  // this method.
  /**
   * Creates any necessary but nonexistent parent directories of the specified
   * file. Note that if this operation fails it may have succeeded in creating
   * some (but not all) of the necessary parent directories.
   *
   * @throws IOException if an I/O error occurs, or if any necessary but
   *     nonexistent parent directories of the specified file could not be
   *     created.
   * @since 4
   */
  private static void createParentDirs(File file) throws IOException {
    File parent = file.getCanonicalFile().getParentFile();
    // TODO: return if parent is null
    parent.mkdirs();
    if (!parent.exists()) { // TODO: change to isDirectory
      throw new IOException("Unable to create parent directories of " + file);
    }
  }

  /**
   * Creates the JSON needed to define the PLOVR_MODULE_INFO variable.
   * @param moduleConfig
   */
  static JsonObject createModuleInfo(ModuleConfig moduleConfig) {
    Map<String, List<String>> invertedDependencyTree = moduleConfig.
      getInvertedDependencyTree();
    JsonObject obj = new JsonObject();
    for (Map.Entry<String, List<String>> entry : invertedDependencyTree.entrySet()) {
      String moduleName = entry.getKey();
      JsonArray modulesThatMustBeLoadedFirst = new JsonArray();
      for (String module : entry.getValue()) {
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
    Map<String, List<String>> invertedDependencyTree = moduleConfig.
        getInvertedDependencyTree();
    JsonObject obj = new JsonObject();
    for (Map.Entry<String, List<String>> entry : invertedDependencyTree.entrySet()) {
      String moduleName = entry.getKey();
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

  private static List<CompilationError> normalizeErrors(JSError[] errors,
      SourceExcerptProvider sourceExcerptProvider) {
    List<CompilationError> compilationErrors = Lists.newLinkedList();
    for (JSError error : errors) {
      compilationErrors.add(new CompilationError(error, sourceExcerptProvider));
    }
    return compilationErrors;
  }

  @VisibleForTesting
  public List<JSSourceFile> getInputs() {
    return inputs;
  }

  @Override
  public String toString() {
    return "Inputs: " + inputs + "; Externs: " + externs + "; Modules: " +
        modules;
  }

}
