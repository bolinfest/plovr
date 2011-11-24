package org.plovr;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.javascript.jscomp.JSModule;
import com.google.javascript.jscomp.JSSourceFile;


/**
 * {@link Manifest} represents an ordered list of JavaScript inputs to the
 * Closure Compiler, along with a set of externs. This list is derived from the
 * transitive closure of the dependencies from a set of input files.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class Manifest {

  private static final Logger logger = Logger.getLogger(Manifest.class.getName());

  /**
   * Converts a plovr JsInput to a Closure Compiler JSSourceFile.
   */
  static Function<JsInput, JSSourceFile> inputToSourceFile =
      new Function<JsInput, JSSourceFile>() {
    @Override
    public JSSourceFile apply(JsInput jsInput) {
      return JSSourceFile.fromGenerator(jsInput.getName(), jsInput);
    }
  };

  private final boolean excludeClosureLibrary;
  private final File closureLibraryDirectory;
  private final Set<File> dependencies;
  private final List<JsInput> requiredInputs;
  private final Set<File> externs;
  private final Set<JsInput> builtInExterns;
  private final boolean customExternsOnly;

  /**
   * When RAW mode is used, each input will need to be accessed by name. To make
   * the lookup efficient, populate this map when getInputsInCompilationOrder()
   * is called to prepare for the requests that are about to occur.
   */
  private Map<String, JsInput> lastOrdering;

  private final SoyFileOptions soyFileOptions;

  // If excludeClosureLibrary ends up being a permanent option, then
  // eliminate this constructor.
  Manifest(
      @Nullable File closureLibraryDirectory,
      List<File> dependencies,
      List<JsInput> requiredInputs,
      @Nullable List<File> externs,
      @Nullable List<JsInput> builtInExterns,
      SoyFileOptions soyFileOptions,
      boolean customExternsOnly) {
    this(
        false /* boolean excludeClosureLibrary */,
        closureLibraryDirectory,
        dependencies,
        requiredInputs,
        externs,
        builtInExterns,
        soyFileOptions,
        customExternsOnly);
  }

  /**
   *
   * @param closureLibraryDirectory Directory that is the root of the Closure
   *        Library (must contain base.js)
   * @param dependencies files (or directories) that contain JS inputs that
   *        may be included in the compilation
   * @param requiredInputs files (or directories) that contain JS inputs that
   *        must be included in the compilation
   * @param externs files (or directories) that contain JS externs
   */
  Manifest(
      boolean excludeClosureLibrary,
      @Nullable File closureLibraryDirectory,
      List<File> dependencies,
      List<JsInput> requiredInputs,
      @Nullable List<File> externs,
      @Nullable List<JsInput> builtInExterns,
      SoyFileOptions soyFileOptions,
      boolean customExternsOnly) {
    Preconditions.checkNotNull(dependencies);
    Preconditions.checkNotNull(requiredInputs);
    Preconditions.checkArgument(requiredInputs.size() > 0,
        "No inputs were specified! " +
        "Make sure there is an option named 'inputs' in the config file");
    Preconditions.checkNotNull(soyFileOptions);

    // TODO(bolinfest): Monitor directories for changes and have the JsInput
    // mark itself dirty when there is a change.
    this.excludeClosureLibrary = excludeClosureLibrary;
    this.closureLibraryDirectory = closureLibraryDirectory;
    this.dependencies = ImmutableSet.copyOf(dependencies);
    this.requiredInputs = ImmutableList.copyOf(requiredInputs);
    this.externs = externs == null ? null : ImmutableSet.copyOf(externs);
    this.builtInExterns = builtInExterns == null
        ? null : ImmutableSet.copyOf(builtInExterns);
    this.soyFileOptions = soyFileOptions;
    this.customExternsOnly = customExternsOnly;
  }

  /**
   * @return the set of files (or directories) that contain JS inputs that
   *     may be included in the compilation
   */
  public Set<File> getDependencies() {
    return ImmutableSet.copyOf(dependencies);
  }

  /**
   * @return a list of files (or directories) that contain JS inputs that
   *     must be included in the compilation
   */
  public List<JsInput> getRequiredInputs() {
    return ImmutableList.copyOf(requiredInputs);
  }

  /**
   * @param moduleConfig
   * @return a new {@link Compilation} that reflects the configuration for
   *         this {@link Manifest}.
   * @throws CompilationException
   */
  public Compilation getCompilerArguments(
      @Nullable ModuleConfig moduleConfig) throws CompilationException {
    // Build up the list of externs to use in the compilation.
    ImmutableList.Builder<JSSourceFile> builder = ImmutableList.builder();
    if (!customExternsOnly) {
      builder.addAll(getDefaultExterns());
    }
    if (this.externs != null) {
      builder.addAll(Lists.transform(getExternInputs(), inputToSourceFile));
    }
    if (this.builtInExterns != null) {
      builder.addAll(Iterables.transform(builtInExterns, inputToSourceFile));
    }
    List<JSSourceFile> externs = builder.build();

    if (moduleConfig == null) {
      List<JsInput> jsInputs = getInputsInCompilationOrder();
      List<JSSourceFile> inputs = Lists.transform(jsInputs, inputToSourceFile);
      logger.config("Inputs: " + jsInputs.toString());
      return Compilation.create(externs, inputs);
    } else {
      List<JSModule> modules = moduleConfig.getModules(this);
      return Compilation.createForModules(externs, modules);
    }
  }

  private List<JSSourceFile> getDefaultExterns() {
    logger.fine("Using default externs");
    return ResourceReader.getDefaultExterns();
  }

  public List<JsInput> getInputsInCompilationOrder() throws CompilationException {
    Set<JsInput> allDependencies = getAllDependencies();

    Map<String, JsInput> provideToSource = getProvideToSource(allDependencies);

    LinkedHashSet<JsInput> compilerInputs = new LinkedHashSet<JsInput>();
    if (!this.excludeClosureLibrary) {
      compilerInputs.add(getBaseJs());
      compilerInputs.add(getDepsJs());
    }
    for (JsInput requiredInput : requiredInputs) {
      buildDependencies(provideToSource, compilerInputs, requiredInput);
    }

    // Update lastOrdering before returning.
    Map<String, JsInput> lastOrdering = Maps.newHashMap();
    for (JsInput input : compilerInputs) {
      lastOrdering.put(input.getName(), input);
    }
    this.lastOrdering = lastOrdering;

    return ImmutableList.copyOf(compilerInputs);
  }

  Map<String, JsInput> getProvideToSource(Set<JsInput> allDependencies) {
    // Build up the dependency graph.
    Map<String, JsInput> provideToSource = Maps.newHashMap();
    for (JsInput input : allDependencies) {
      List<String> provides = input.getProvides();
      for (String provide : provides) {
        JsInput existingProvider = provideToSource.get(provide);
        if (existingProvider != null) {
          throw new IllegalStateException(provide + " is provided by both " +
              existingProvider + " and " + input);
        }
        provideToSource.put(provide, input);
      }
    }
    return provideToSource;
  }

  boolean isUseClosureLibrary() {
    return !this.excludeClosureLibrary;
  }

  JsInput getBaseJs() {
    if (isBuiltInClosureLibrary()) {
      return ResourceReader.getBaseJs();
    } else {
      // TODO: Use a Supplier so that this is only done once.
      return new JsSourceFile("/base.js",
          new File(closureLibraryDirectory, "base.js"));
    }
  }

  boolean isBuiltInClosureLibrary() {
    return closureLibraryDirectory == null;
  }

  JsInput getDepsJs() {
    Preconditions.checkState(!this.excludeClosureLibrary);
    String depsJs = buildDepsJs();
    return new DepsJsInput(getBaseJs(), depsJs);
  }

  JsInput getJsInputByName(String name) {
    if (lastOrdering == null) {
      // It is possible that a file could be requested from the manifest before
      // a compilation is done, such as when a user navigates directly to /view.
      // In that case, lastOrdering will be null, so invoke
      // getInputsInCompilationOrder() so that it gets initialized.
      try {
        // TODO(bolinfest): Create a utility method that just traverses the list
        // of inputs and dependencies as the ordering is not actually needed at
        // this point. Such a utility method would not throw a
        // CompilationException.
        getInputsInCompilationOrder();
      } catch (CompilationException e) {
        throw new RuntimeException(e);
      }
    }
    return lastOrdering.get(name);
  }

  void buildDependencies(Map<String, JsInput> provideToSource,
      LinkedHashSet<JsInput> transitiveDependencies, JsInput input)
      throws CompilationException {
    buildDependenciesInternal(provideToSource, transitiveDependencies,
        new LinkedHashSet<JsInput>(), input);
  }

  void buildDependenciesInternal(Map<String, JsInput> provideToSource,
      LinkedHashSet<JsInput> transitiveDependencies,
      LinkedHashSet<JsInput> currentDependencyChain,
      JsInput input)
      throws CompilationException {
    // Avoid infinite by going depth-first and never revisiting a node.
    if (currentDependencyChain.contains(input)) {
      throw new CircularDependencyException(input, currentDependencyChain);
    }
    currentDependencyChain.add(input);

    for (String require : input.getRequires()) {
      JsInput provide = provideToSource.get(require);
      if (provide == null) {
        throw new MissingProvideException(input, require);
      }
      // It is possible that this dependency has already been included in the
      // set of transitive dependencies, in which case its dependencies should
      // not be built again.
      if (transitiveDependencies.contains(provide)) {
        continue;
      }
      buildDependenciesInternal(provideToSource, transitiveDependencies,
          currentDependencyChain, provide);
    }
    transitiveDependencies.add(input);

    currentDependencyChain.remove(input);
  }

  Set<JsInput> getAllDependencies() {
    Set<JsInput> allDependencies = Sets.newHashSet();
    final boolean externsOnly = false;
    if (isBuiltInClosureLibrary()) {
      allDependencies.addAll(ResourceReader.getClosureLibrarySources());
    } else {
      allDependencies.addAll(getFiles(closureLibraryDirectory, externsOnly));
    }
    // Add the requiredInputs first so that if a file is both an "input" and a
    // "path" under different names (such as "hello.js" and "/./hello.js"), the
    // name used to specify the input is preferred.
    allDependencies.addAll(requiredInputs);
    allDependencies.addAll(getFiles(dependencies, externsOnly));
    return allDependencies;
  }

  private List<JsInput> getExternInputs() {
    final boolean externsOnly = true;
    List<JsInput> externInputs = Lists.newArrayList(getFiles(externs,
        externsOnly));
    return ImmutableList.copyOf(externInputs);
  }

  private Set<JsInput> getFiles(File fileToExpand, boolean externsOnly) {
    return getFiles(Sets.newHashSet(fileToExpand), externsOnly);
  }

  private Set<JsInput> getFiles(Set<File> filesToExpand, boolean externsOnly) {
    Set<JsInput> inputs = Sets.newHashSet();
    for (File file : filesToExpand) {
      getInputs(file, inputs, externsOnly, file);
    }
    return ImmutableSet.copyOf(inputs);
  }

  /**
   *
   * @param rootOfSearch is the directory where this search started -- this is
   *     used to determine the relative path of the resulting input
   */
  private void getInputs(File file, Set<JsInput> output, boolean externsOnly,
      final File rootOfSearch) {
    // Some editors may write backup files whose names start with a
    // dot. Furthermore, Emacs will create symlinks that start with a
    // dot that don't point at actual files, causing file.exists() to
    // not work. Such files should be ignored. (If this turns out to
    // be an issue, this could be changed so it is configurable.) One
    // common exception is when the name is simply ".", referring to
    // the current directory.
    if (file.getName().startsWith(".") && !".".equals(file.getName())) {
      logger.info("Ignoring: " + file);
      return;
    }

    Preconditions.checkArgument(file.exists(), "File not found at: " +
        file.getAbsolutePath());

    Function<File, String> getRelativePath = new Function<File, String>() {
      @Override
      public String apply(File file) {
        // If the root of the search is a file rather than a directory, then it
        // should be the file parameter to getInputs(). In that case, just use
        // the name of the file as the name of the JsInput.
        if (!rootOfSearch.isDirectory()) {
          Preconditions.checkArgument(rootOfSearch.equals(file));
          return file.getName();
        }

        String rootPath = rootOfSearch.toURI().toString();
        String fullPath = file.toURI().toString();
        return "/" + fullPath.substring(rootPath.length());
      }
    };

    if (file.isFile()) {
      String fileName = file.getName();
      if (fileName.endsWith(".js") ||
          (!externsOnly && fileName.endsWith(".soy")) ||
          (!externsOnly && fileName.endsWith(".coffee"))) {
        // Using "." as the value for "paths" in the config file results in ugly
        // names for JsInputs because of the way the relative path is resolved,
        // so strip the leading "/./" from the JsInput name in this case.
        String name = getRelativePath.apply(file);
        final String uglyPrefix = "/./";
        if (name.startsWith(uglyPrefix)) {
          name = name.substring(uglyPrefix.length());
        }
        JsInput input = LocalFileJsInput.createForFileWithName(file, name,
            soyFileOptions);
        logger.config("Dependency: " + input);
        output.add(input);
      }
    } else if (file.isDirectory()) {
      logger.config("Directory to explore: " + file);
      for (File entry : file.listFiles()) {
        getInputs(entry, output, externsOnly, rootOfSearch);
      }
    }
  }

  /**
   * For compilation, the exact path to the input in the generated deps.js file
   * does not matter.
   */
  private String buildDepsJs() {
    return buildDepsJs(new Function<JsInput, String>() {
      @Override
      public String apply(JsInput input) {
        return input.getName();
      }
    });
  }

  String buildDepsJs(Function<JsInput, String> converter) {
    StringBuilder builder = new StringBuilder();
    SortedSet<JsInput> inputs = ImmutableSortedSet.copyOf(
        JsInputComparator.SINGLETON, getAllDependencies());

    final Gson gson = new Gson();

    Function<String, String> toJsString = new Function<String, String>() {
      @Override
      public String apply(String str) {
        return gson.toJson(str);
      }
    };

    Joiner comma = Joiner.on(", ");

    for (JsInput input : inputs) {
      builder.append("goog.addDependency(" +
          toJsString.apply(converter.apply(input)) +
          ", [" +
          comma.join(Lists.transform(input.getProvides(), toJsString)) +
          "], [" +
          comma.join(Lists.transform(input.getRequires(), toJsString)) +
          "]);\n");
    }
    return builder.toString();
  }

}
