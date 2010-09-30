package org.plovr;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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

  private final File closureLibraryDirectory;
  private final Set<File> dependencies;
  private final List<JsInput> requiredInputs;
  private final Set<File> externs;
  private final boolean customExternsOnly;

  /**
   * When RAW mode is used, each input will need to be accessed by name. To make
   * the lookup efficient, populate this map when getInputsInCompilationOrder()
   * is called to prepare for the requests that are about to occur.
   */
  private Map<String, JsInput> lastOrdering;

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
      @Nullable File closureLibraryDirectory,
      List<File> dependencies,
      List<JsInput> requiredInputs,
      @Nullable List<File> externs,
      boolean customExternsOnly) {
    Preconditions.checkNotNull(dependencies);
    Preconditions.checkNotNull(requiredInputs);

    // TODO(bolinfest): Monitor directories for changes and have the JsInput
    // mark itself dirty when there is a change.
    this.closureLibraryDirectory = closureLibraryDirectory;
    this.dependencies = ImmutableSet.copyOf(dependencies);
    this.requiredInputs = ImmutableList.copyOf(requiredInputs);
    this.externs = externs == null ? null : ImmutableSet.copyOf(externs);
    this.customExternsOnly = customExternsOnly;
  }

  /**
   * @param moduleConfig
   * @return a new {@link Compilation} that reflects the configuration for
   *         this {@link Manifest}.
   * @throws MissingProvideException
   */
  public Compilation getCompilerArguments(
      @Nullable ModuleConfig moduleConfig) throws MissingProvideException {
    // Build up the list of externs to use in the compilation.
    ImmutableList.Builder<JSSourceFile> builder = ImmutableList.builder();
    if (!customExternsOnly) {
      builder.addAll(getDefaultExterns());
    }
    if (this.externs != null) {
      builder.addAll(Lists.transform(getExternInputs(), inputToSourceFile));
    }
    List<JSSourceFile> externs = builder.build();

    List<JsInput> jsInputs = getInputsInCompilationOrder();
    if (moduleConfig == null) {
      List<JSSourceFile> inputs = Lists.transform(jsInputs, inputToSourceFile);
      logger.info("Inputs: " + jsInputs.toString());
      return Compilation.create(externs, inputs);
    } else {
      List<JSModule> modules = moduleConfig.getModules(jsInputs);
      return Compilation.createForModules(externs, modules);
    }
  }

  private List<JSSourceFile> getDefaultExterns() {
    logger.fine("Using default externs");
    return ResourceReader.getDefaultExterns();
  }

  public List<JsInput> getInputsInCompilationOrder() throws MissingProvideException {
    Set<JsInput> allDependencies = getAllDependencies();

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

    LinkedHashSet<JsInput> compilerInputs = new LinkedHashSet<JsInput>();
    JsInput inputToAdd;
    if (closureLibraryDirectory == null) {
      inputToAdd = ResourceReader.getBaseJs();
    } else {
      String path = "base.js";
      inputToAdd = new JsSourceFile(path, new File(closureLibraryDirectory, path));
    }
    compilerInputs.add(inputToAdd);
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
        // MissingProvideException.
        getInputsInCompilationOrder();
      } catch (MissingProvideException e) {
        return null;
      }
    }
    return lastOrdering.get(name);
  }

  private void buildDependencies(Map<String, JsInput> provideToSource,
      LinkedHashSet<JsInput> transitiveDependencies, JsInput input)
      throws MissingProvideException {
    for (String require : input.getRequires()) {
      JsInput provide = provideToSource.get(require);
      if (provide == null) {
        throw new MissingProvideException(input, require);
      }
      buildDependencies(provideToSource, transitiveDependencies, provide);
    }
    transitiveDependencies.add(input);
  }

  private Set<JsInput> getAllDependencies() {
    Set<JsInput> allDependencies = Sets.newHashSet();
    final boolean includeSoy = true;
    if (closureLibraryDirectory == null) {
      allDependencies.addAll(ResourceReader.getClosureLibrarySources());
    } else {
      allDependencies.addAll(getFiles(closureLibraryDirectory, includeSoy));
    }
    // Add the requiredInputs first so that if a file is both an "input" and a
    // "path" under different names (such as "hello.js" and "/./hello.js"), the
    // name used to specify the input is preferred.
    allDependencies.addAll(requiredInputs);
    allDependencies.addAll(getFiles(dependencies, includeSoy));
    return allDependencies;
  }

  private List<JsInput> getExternInputs() {
    final boolean includeSoy = false;
    List<JsInput> externInputs = Lists.newArrayList(getFiles(externs,
        includeSoy));
    return ImmutableList.copyOf(externInputs);
  }

  private Set<JsInput> getFiles(File fileToExpand, boolean includeSoy) {
    return getFiles(Sets.newHashSet(fileToExpand), includeSoy);
  }

  private Set<JsInput> getFiles(Set<File> filesToExpand, boolean includeSoy) {
    Set<JsInput> inputs = Sets.newHashSet();
    for (File file : filesToExpand) {
      getInputs(file, "", inputs, includeSoy);
    }
    return ImmutableSet.copyOf(inputs);
  }

  private void getInputs(File file, String path, Set<JsInput> output,
      boolean includeSoy) {
    Preconditions.checkArgument(file.exists(), "File not found at: " +
        file.getAbsolutePath());

    // Some editors, such as Emacs, may write backup files whose names start
    // with a dot. Such files should be ignored. (If this turns out to be an
    // issue, this could be changed so it is configurable.) One common
    // exception is when the name is simply ".", referring to the current
    // directory.
    if (file.getName().startsWith(".") && !".".equals(file.getName())) {
      logger.info("Ignoring: " + file);
      return;
    }

    if (file.isFile()) {
      String fileName = file.getName();
      if (fileName.endsWith(".js") || (includeSoy && fileName.endsWith(".soy"))) {
        // Using "." as the value for "paths" in the config file results in ugly
        // names for JsInputs because of the way the relative path is resolved,
        // so strip the leading "/./" from the JsInput name in this case.
        String name = path + "/" + fileName;
        final String uglyPrefix = "/./";
        if (name.startsWith(uglyPrefix)) {
          name = name.substring(uglyPrefix.length());
        }
        JsInput input = LocalFileJsInput.createForFileWithName(file, name);
        logger.config("Dependency: " + input);
        output.add(input);
      }
    } else if (file.isDirectory()) {
      logger.config("Directory to explore: " + file);
      path += "/" + file.getName();
      for (File entry : file.listFiles()) {
        getInputs(entry, path, output, includeSoy);
      }
    }
  }
}
