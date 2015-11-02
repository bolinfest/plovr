package org.plovr;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.javascript.jscomp.JSModule;

public final class ModuleConfig {

  private static final Logger logger = Logger.getLogger(ModuleConfig.class
      .getName());

  private final String rootModule;

  private final String productionUri;

  /** a map of modules to modules that depend on that module */
  private final Map<String, List<String>> invertedDependencyTree;

  private final Map<String, ModuleInfo> moduleInfoMap;

  /**
   * A topological sort of the modules. This provides a convenient way to
   * iterate over the modules in a sensible order. Recall from
   * {@link ModuleConfig.Builder#setModuleInfo(JsonObject)} that a topological
   * sort of a module graph is not unique.
   */
  private final List<String> topologicalSort;

  private final Map<String, File> moduleToOutputPath;

  private final File moduleInfoPath;

  private ModuleConfig(String rootModule,
      Map<String, List<String>> invertedDependencyTree,
      Map<String, ModuleInfo> moduleInfo,
      List<String> topologicalSort,
      Map<String, File> moduleToOutputPath, File moduleInfoPath,
      String productionUri) {
    this.rootModule = rootModule;
    this.invertedDependencyTree = invertedDependencyTree;
    this.moduleInfoMap = moduleInfo;
    this.topologicalSort = topologicalSort;
    this.moduleToOutputPath = moduleToOutputPath;
    this.moduleInfoPath = moduleInfoPath;
    this.productionUri = productionUri;
  }

  public String getRootModule() {
    return rootModule;
  }

  public Iterable<String> getModuleNames() {
    return Iterables.unmodifiableIterable(invertedDependencyTree.keySet());
  }

  /**
   * @return the list of inputs specified under "modules" in the config
   */
  List<String> getInputNames() {
    ImmutableList.Builder<String> inputs = ImmutableList.builder();
    for (String module : topologicalSort) {
      inputs.addAll(moduleInfoMap.get(module).getInputs());
    }
    return inputs.build();
  }

  public Map<String, File> getModuleToOutputPath() {
    return this.moduleToOutputPath;
  }

  public ModuleInfo getModuleInfo(String module) {
    return moduleInfoMap.get(module);
  }

  /**
   * @return a map of modules to modules that depend on that module
   */
  public Map<String, List<String>> getInvertedDependencyTree() {
    return invertedDependencyTree;
  }

  public String getProductionUri() {
    return productionUri;
  }

  public File getModuleInfoPath() {
    return moduleInfoPath;
  }

  public boolean excludeModuleInfoFromRootModule() {
    return moduleInfoPath != null;
  }

  public Function<String, String> createModuleNameToUriFunction() {
    final String productionUri = getProductionUri();
    Function<String, String> moduleNameToUri = new Function<String, String>() {
      @Override
      public String apply(String moduleName) {
        return populateModuleNameTemplate(productionUri, moduleName);
      }
    };
    return moduleNameToUri;
  }

  /**
   *
   * @param template
   * @param moduleName
   * @return
   */
  private static String populateModuleNameTemplate(String template,
      String moduleName) {
    String name = template.replace("%s", moduleName);

    return name;
  }

  /**
   *
   * @param inputs
   *          This list is most likely to be produced by
   *          {@link Manifest#getInputsInCompilationOrder()}.
   * @return
   * @throws CompilationException
   */
  List<JSModule> getModules(Manifest manifest) throws CompilationException {
    // There are some important requirements when using modules:
    // (1) Each input must appear in exactly one module.
    // (2) For each input in a module, each of its transitive dependencies
    // must either appear (i) before it in that module, or (ii) in an ancestor
    // module. This means that if two sibling modules have the same dependency,
    // then that dependency should appear in the least common ancestor module
    // of those two modules. For example, consider the following scenario:
    //
    //    A     A contains base.js
    //   / \    B contains useragent.js which depends on string.js
    //  B   C   C contains asserts.js which depends on string.js
    //
    // There are two reasonable options for how the inputs (and their transitive
    // dependencies) should be alloted to the modules:
    //
    // Option 1: Put string.js in A even though the only file initially assigned
    // to A (base.js) does not depend on string.js. Because both B and C depend
    // on A, this ensures that string.js will always be loaded before
    // both useragent.js and asserts.js, as desired.
    //
    // Option 2: Create a "synthetic module" named D which results in a new
    // module dependency tree:
    //
    //    A     A contains base.js
    //    |
    //    D     D contains string.js
    //   / \    B contains useragent.js which depends on string.js
    //  B   C   C contains asserts.js which depends on string.js
    //
    // The benefit of this solution is that the common dependencies of B and C
    // no longer bloat A. If a sophisticated module loader is used on the
    // server and the client, it would be possible to request modules D and B
    // in a single request, such that the server would be responsible for
    // concatenating the contents of both modules and returning them in a single
    // response.
    //
    // For now, only Option 1 is implemented for simplicity. Because the module
    // graph is a rooted DAG, the root module is built up first, followed by its
    // children. As child modules are created, inputs (and their transitive
    // dependencies) may be promoted to ancestor modules to satisfy dependency
    // constraints.
    return buildModulesUsingOption1(manifest);
  }

  private List<JSModule> buildModulesUsingOption1(Manifest manifest)
      throws CompilationException {
    Map<String, List<JsInput>> moduleToInputs = partitionInputsIntoModules(manifest);

    // Convert each list of JsInput dependencies for each module into a
    // JSModule and return the JSModules as a topologically sorted list.
    Map<String, JSModule> modulesByName = Maps.newHashMap();
    for (String module : topologicalSort) {
      // Create the module and add the dependencies in order.
      JSModule jsModule = new JSModule(module);
      List<JsInput> deps = moduleToInputs.get(module);
      for (JsInput dep : deps) {
        jsModule.add(Manifest.inputToSourceFile.apply(dep));
      }
      modulesByName.put(module, jsModule);
    }

    // Add the dependencies for each module.
    ImmutableList.Builder<JSModule> builder = ImmutableList.builder();
    for (String module : topologicalSort) {
      JSModule jsModule = modulesByName.get(module);
      for (String parent : moduleInfoMap.get(module).getDeps()) {
        jsModule.addDependency(modulesByName.get(parent));
      }
      builder.add(jsModule);
    }

    return builder.build();
  }

  Map<String, List<JsInput>> partitionInputsIntoModules(Manifest manifest)
      throws CompilationException {
    List<JsInput> inputsInOrder = manifest.getInputsInCompilationOrder();
    JsInput baseJs = null;
    JsInput depsJs = null;

    // When using the Closure Library, base.js is the first item on the list and
    // should be removed.
    if (manifest.isUseClosureLibrary()) {
      baseJs = inputsInOrder.get(0);
      Preconditions.checkArgument(baseJs.equals(manifest.getBaseJs()),
          "base.js should be the first input");

      depsJs = inputsInOrder.get(1);
      Preconditions.checkArgument(depsJs instanceof DepsJsInput,
          "deps.js should be the second input");

      inputsInOrder = inputsInOrder.subList(2, inputsInOrder.size());
    }

    // Step 1: Build the set of transitive dependencies for each module.
    Map<String, LinkedHashSet<JsInput>> moduleToTransitiveDependencies = Maps
        .newHashMap();
    Map<String, JsInput> provideToSource = manifest.getProvideToSource(
        ImmutableSet.copyOf(inputsInOrder));
    for (String module : topologicalSort) {
      ModuleInfo moduleInfo = moduleInfoMap.get(module);
      LinkedHashSet<JsInput> deps = new LinkedHashSet<JsInput>();
      for (String inputName : moduleInfo.getInputs()) {
        JsInput input = manifest.getJsInputByName(inputName);
        manifest.buildDependencies(provideToSource, deps, input);
      }
      moduleToTransitiveDependencies.put(module, deps);
    }

    // Assign inputs to modules.
    // For each module, iterate over its list of inputs and keep track of
    // which inputs have already been seen. For an input that is seen for the
    // first time, find the set of modules that contain that input in its set of
    // transitive dependencies. Once that set is created, find the least common
    // ancestor module for the members of the set, and add the input to that
    // module.
    Map<String, List<JsInput>> moduleToInputs = Maps.newHashMap();
    for (String module : topologicalSort) {
      moduleToInputs.put(module, Lists.<JsInput>newLinkedList());
    }
    Searcher searcher = new Searcher();
    for (JsInput input : inputsInOrder) {
      Set<String> modulesWithInput = findModulesWithInput(
          input, moduleToTransitiveDependencies);
      String ancestor = findLeastCommonAncestor(modulesWithInput, searcher);
      moduleToInputs.get(ancestor).add(input);
    }

    // Because it is a special case, add base.js as the first input in the root
    // module unless the Closure Library is excluded.
    if (manifest.isUseClosureLibrary()) {
      Preconditions.checkNotNull(baseJs);
      List<JsInput> rootModuleInputs = moduleToInputs.get(rootModule);
      rootModuleInputs.add(0, depsJs);
      rootModuleInputs.add(0, baseJs);
    }

    return moduleToInputs;
  }

  /**
   * Using the moduleToTransitiveDependencies map, returns the set of modules
   * that contain the specified input as a transitive dependency.
   */
  private static Set<String> findModulesWithInput(
      JsInput input,
      Map<String, LinkedHashSet<JsInput>> moduleToTransitiveDependencies) {
    Set<String> modules = Sets.newHashSet();
    for (Map.Entry<String, LinkedHashSet<JsInput>> entry :
        moduleToTransitiveDependencies.entrySet()) {
      LinkedHashSet<JsInput> inputs = entry.getValue();
      if (inputs.contains(input)) {
        modules.add(entry.getKey());
      }
    }
    return modules;
  }

  private String findLeastCommonAncestor(Set<String> modules, Searcher searcher) {
    Preconditions.checkArgument(modules.size() > 0);

    // If the set contains the root module, then the least common ancestor of
    // the set must be the root module, so check for that first.
    String rootModule = getRootModule();
    if (modules.contains(rootModule)) {
      return rootModule;
    }

    // If the set has exactly one element, then it must be its own least common
    // ancestor.
    Iterator<String> iter = modules.iterator();
    String firstModule = iter.next();
    if (modules.size() == 1) {
      return firstModule;
    }

    // The input must satisfy one of the following cases:
    // 1. One of the modules in the set is a common ancestor for all of the
    // modules in the set, in which case it is must be the least common ancestor
    // for all of the modules in the set.
    // 2. A module that is not in the set is the least common ancestor for all
    // of the modules.
    // It cannot be the case that no least common module exists because the root
    // module is a common ancestor for any set of modules in the graph.
    return searcher.findCommonAncestor(modules);
  }

  private class Searcher {

    /**
     * This is a map from Set of modules -> least common ancestor for
     * these modules. Note that when inserting elements, the key must
     * be an ImmutableSet, otherwise map invariants are not
     * maintained.
     */
    private final Map<Set<String>,String> cache = Maps.newHashMap();

    /** The index of a module in the distanceMap. */
    private final Map<String, Integer> moduleToIndex = Maps.newHashMap();

    /**
     * This is a 2D array of minimum distances between modules along
     * the DAG. The first index is the source module, and the second
     * index is the ancestor module. The value is the minimum distance
     * between the two. null means that there is no connection. The
     * module -> integer index mapping is done by looking at
     * moduleToIndex.
     */
    private final Integer[][] distanceMap;

    private Searcher() {
      // Compute moduleToIndex map
      int i = 0;
      for (String module : moduleInfoMap.keySet()) {
        moduleToIndex.put(module, i);
        i++;
      }

      distanceMap = new Integer[i][i];

      for (Map.Entry<String, Integer> module : moduleToIndex.entrySet()) {
        populateModuleDistances(
            module.getKey(),
            module.getValue(),
            module.getValue(),
            0);
      }
    }

    /**
     * Returns the module that is the least common ancestor of the specified
     * modules. Note that in a graph such as the following:
     * <pre>
     *       A
     *     /   \
     *    B     C
     *    | \ / |   In this example, both D and E depend on both B and C.
     *    | / \ |   Therefore, either B or C could be returned as the least
     *    D     E   common ancestor of D and E.
     * </pre>
     * There may not be a unique solution.
     */
    public String findCommonAncestor(Set<String> modules) {
      String ancestor = cache.get(modules);
      if (ancestor != null) {
        return ancestor;
      }

      // For the source modules of interest (the ones in the input
      // array) we need to find all the common ancestors (those are
      // all the modules that have non-null values for each of the
      // source modules). We consider an ancestor with the lowest
      // distance to any of the source modules to be the least common
      // ancestor.

      String minModule = null;
      int minDistance = Integer.MAX_VALUE;
      for (Map.Entry<String, Integer> module : moduleToIndex.entrySet()) {
        int moduleIndex = module.getValue();

        int min = Integer.MAX_VALUE;
        int nonNull = 0;
        for (String sourceModule : modules) {
          int sourceModuleIndex = moduleToIndex.get(sourceModule);

          Integer distance = distanceMap[sourceModuleIndex][moduleIndex];
          if (distance != null) {
            nonNull++;
            min = Math.min(min, distance);
          }
        }
        if (nonNull != modules.size()) {
          // Not every seed module reached this module.
          continue;
        }
        if (min < minDistance) {
          minDistance = min;
          minModule = module.getKey();
        }
      }

      Preconditions.checkNotNull("No common ancestor found", minModule);

      cache.put(ImmutableSet.copyOf(modules), minModule);

      return minModule;
    }

    /**
     * Populates the distance map by recursing down a module's
     * dependencies. This function call should be initiated with
     * identical moduleIndex and sourceModuleIndex, and will populate
     * the distanceMap for that sourceModuleIndex.
     */
    private void populateModuleDistances(
        String module,
        int moduleIndex,
        int sourceModuleIndex,
        int depth) {
      Integer current = distanceMap[sourceModuleIndex][moduleIndex];
      if (current == null || depth < current) {
        distanceMap[sourceModuleIndex][moduleIndex] = depth;
      }

      for (String dep : moduleInfoMap.get(module).getDeps()) {
        populateModuleDistances(
            dep,
            moduleToIndex.get(dep),
            sourceModuleIndex,
            depth + 1);
      }
    }
  }

  public static Builder builder(File relativePathBase) {
    Preconditions.checkNotNull(relativePathBase);
    return new Builder(relativePathBase);
  }

  public static Builder builder(ModuleConfig moduleConfig) {
    Preconditions.checkNotNull(moduleConfig);
    return new Builder(moduleConfig);
  }

  final static class Builder {

    /** Directory against which relative path names will be resolved. */
    private final File relativePathBase;

    // Either moduleToOutputPath will be assigned in the constructor, or
    // outputPath will be defined later and will be used to create a
    // moduleToOutputPath map when build() is invoked.
    private final Map<String, File> moduleToOutputPath;
    private String outputPath = "module_%s.js";

    private String rootModule;

    private String productionUri = "module_%s.js";

    private Map<String, List<String>> dependencyTree;

    private Map<String, ModuleInfo> moduleInfo;

    private List<String> topologicalSort;

    // Either moduleInfoPath will be assigned in the constructor, or
    // infoPath will be defined later and will be used to create a
    // moduleInfoPath when build() is invoked.
    private final File moduleInfoPath;
    private String infoPath;

    private Builder(File relativePathBase) {
      Preconditions.checkNotNull(relativePathBase);
      Preconditions.checkArgument(relativePathBase.isDirectory(),
          relativePathBase + " is not a directory");
      this.relativePathBase = relativePathBase;
      this.moduleToOutputPath = null;
      this.moduleInfoPath = null;
    }

    private Builder(ModuleConfig moduleConfig) {
      this.relativePathBase = null;
      this.rootModule = moduleConfig.rootModule;
      this.productionUri = moduleConfig.productionUri;

      // An outputPath set on this builder will be ignored as the inherited
      // moduleToOutputPath will be used instead.
      this.moduleToOutputPath = moduleConfig.moduleToOutputPath;
      this.outputPath = null;

      this.moduleInfoPath = moduleConfig.moduleInfoPath;
      this.infoPath = null;

      this.dependencyTree = moduleConfig.invertedDependencyTree;
      this.moduleInfo = moduleConfig.moduleInfoMap;
      this.topologicalSort = moduleConfig.topologicalSort;
    }

    /**
     * @return a mapping of a module to the modules that depend on it
     */
    private static Map<String, List<String>> invertDependencyTree(
        Collection<ModuleInfo> moduleInfo) throws BadDependencyTreeException {
      // dependencies maps a module to the modules that depend on it. A module
      // must be loaded before any of its dependencies.
      Map<String, List<String>> dependencies = Maps.newHashMap();

      // Populate the keys of dependencies with the keys of the "deps" object
      // literal from the config file.
      for (ModuleInfo info : moduleInfo) {
        dependencies.put(info.getName(), Lists.<String> newLinkedList());
      }

      for (ModuleInfo info : moduleInfo) {
        String dependentModule = info.getName();
        for (String ancestorModule : info.getDeps()) {
          List<String> moduleDeps = dependencies.get(ancestorModule);
          if (moduleDeps != null) {
            moduleDeps.add(dependentModule);
          } else {
            // A dependent module must appear as a key in the "deps" object
            // literal from the config file.
            throw new BadDependencyTreeException(ancestorModule
                + " is not a key in the \"deps\" map");
          }
        }
      }

      return dependencies;
    }

    /**
     * @param dependencies
     * @return the name of the root module
     * @throws BadDependencyTreeException
     *           if the dependencies are not well-formed.
     */
    private static String findRootModule(Collection<ModuleInfo> dependencies)
        throws BadDependencyTreeException {
      String moduleWithNoDependencies = null;
      for (ModuleInfo info : dependencies) {
        List<String> moduleDeps = info.getDeps();
        if (moduleDeps.size() == 0) {
          if (moduleWithNoDependencies == null) {
            moduleWithNoDependencies = info.getName();
          } else {
            throw new BadDependencyTreeException("Both "
                + moduleWithNoDependencies + " and " + info.getName()
                + " have no dependencies, so this"
                + " dependency graph does not form a tree");
          }
        }
      }

      if (moduleWithNoDependencies == null) {
        throw new BadDependencyTreeException("There was no module with zero"
            + " dependencies, so this tree has no root.");
      }

      return moduleWithNoDependencies;
    }

    /**
     *
     * @param dependencyTree
     *          map of module to modules that depend on it
     * @param rootModule
     *          the root module of the tree
     * @return
     * @throws BadDependencyTreeException
     */
    private static List<String> buildDependencies(
        Map<String, List<String>> dependencyTree, String rootModule)
        throws BadDependencyTreeException {
      LinkedHashSet<String> transitiveDependencies = new LinkedHashSet<String>();
      buildDependencies(dependencyTree, transitiveDependencies, rootModule,
          new LinkedHashSet<String>());

      // Because the dependencies were built up in reverse order, add the
      // results of the iterator in reverse order to create a new list.
      LinkedList<String> dependencies = Lists.newLinkedList();
      for (String module : transitiveDependencies) {
        dependencies.addFirst(module);
      }
      return dependencies;
    }

    /**
     *
     * @param dependencies
     * @param transitiveDependencies
     *          the Iterator of this set returns the dependencies in reverse
     *          order
     * @param dependency
     * @param currentChain Tracks what the current path down the tree is
     * @throws BadDependencyTreeException
     */
    private static void buildDependencies(
        Map<String, List<String>> dependencies,
        LinkedHashSet<String> transitiveDependencies,
        String dependency,
        LinkedHashSet<String> currentChain)
        throws BadDependencyTreeException {
      if (currentChain.contains(dependency)) {
        throw new BadDependencyTreeException("Circular module dependency: " +
            currentChain + " -> " + dependency);
      }
      if (transitiveDependencies.contains(dependency)) {
        // We've already visited this node, so no need to go down the
        // dependency list again. If there was a loop, we would have
        // found it last time we had hit this node.
        return;
      }
      currentChain.add(dependency);
      for (String dependencyList : dependencies.get(dependency)) {
        buildDependencies(dependencies, transitiveDependencies, dependencyList,
            currentChain);
      }
      transitiveDependencies.add(dependency);
      currentChain.remove(dependency);
    }

    /**
     *
     * @param json
     * @return
     * @throws BadDependencyTreeException
     */
    public void setModuleInfo(JsonObject json)
        throws BadDependencyTreeException {

      // Convert the JSON into a list of module entries.
      Map<String, ModuleInfo> moduleInfos = Maps.newHashMap();
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
        // Each JsonElement should be an object with "deps" and "inputs" fields.
        String name = entry.getKey();
        JsonElement element = entry.getValue();
        if (!element.isJsonObject()) {
          throw new IllegalArgumentException("Value for " + name +
              " must be a JSON object: " + element);
        }
        JsonObject obj = element.getAsJsonObject();

        // Read the values out of the JSON object to populate the moduleInfo.
        ModuleInfo moduleInfo = new ModuleInfo();
        moduleInfo.setName(name);
        List<String> deps = GsonUtil.toListOfStrings(obj.get("deps"));
        moduleInfo.setDeps(deps);
        List<String> inputs = GsonUtil.toListOfStrings(obj.get("inputs"));
        if (inputs == null) {
          throw new IllegalArgumentException("Module " + name +
              " must specify inputs" +
              // TODO(bolinfest): Remove after this change has been out for awhile
              "\nMake sure you specify \"inputs\" in the config file rather " +
              "than \"input\" as the name has changed to reflect that the " +
              "value may be either a single string or a list of strings.");
        }
        moduleInfo.setInputs(inputs);
        moduleInfos.put(name, moduleInfo);
      }

      setModuleInfo(moduleInfos);
    }

    void setModuleInfo(Map<String, ModuleInfo> moduleInfo)
        throws BadDependencyTreeException {
      // In dependencyTree, modules point to the modules that depend on them.
      final Map<String, List<String>> dependencyTree = invertDependencyTree(moduleInfo
          .values());

      String rootModule = findRootModule(moduleInfo.values());
      // Calling buildDependencies() confirms that the module dependencies are
      // well-formed by producing a topological sort of the modules.
      // For example, if the config file contained:
      //
      // "deps" : {
      //   "A": [],           A
      //   "B": ["A"],       / \
      //   "C": ["A"],      B   C
      //   "D": ["B","C"],   \ / \
      //   "E": ["C"]         D   E
      // }
      //
      // Then the iteration of transitiveDependencies would produce either:
      //
      // ["A", "B", "C", "D", "E"] OR ["A", "B", "C", "E", "D"]
      //
      // Both are valid results because the topological sort of the dependency
      // graph is not unique.
      List<String> topologicalSort = buildDependencies(dependencyTree,
          rootModule);
      logger.config(topologicalSort.toString());

      this.rootModule = rootModule;
      this.dependencyTree = dependencyTree;
      this.moduleInfo = moduleInfo;
      this.topologicalSort = topologicalSort;
    }

    public void setProductionUri(String productionUri) {
      ConfigOption.assertContainsModuleNamePlaceholder(productionUri);
      this.productionUri = productionUri;
    }

    public void setOutputPath(String outputPath) {
      Preconditions.checkState(moduleToOutputPath == null,
          "The output paths of this config cannot be redefined.");
      ConfigOption.assertContainsModuleNamePlaceholder(outputPath);
      this.outputPath = outputPath;
    }

    public void setModuleInfoPath(String moduleInfoPath) {
      this.infoPath = moduleInfoPath;
    }

    public ModuleConfig build() {
      Preconditions.checkState(dependencyTree != null, "No modules were set");

      // Set the paths to write the compiled module files to.
      Map<String, File> moduleToOutputPath;
      if (this.moduleToOutputPath == null) {
        moduleToOutputPath = Maps.newHashMap();
        for (String moduleName : dependencyTree.keySet()) {
          String partialPath = populateModuleNameTemplate(outputPath,
              moduleName);
          File moduleFile = new File(ConfigOption.maybeResolvePath(partialPath,
              relativePathBase));
          moduleToOutputPath.put(moduleName, moduleFile);
        }
      } else {
        moduleToOutputPath = this.moduleToOutputPath;
      }

      // Set the path to write the module info to.
      File moduleInfoPath;
      if (this.moduleInfoPath == null) {
        if (infoPath == null) {
          moduleInfoPath = null;
        } else {
          moduleInfoPath = new File(ConfigOption.maybeResolvePath(infoPath,
              relativePathBase));
        }
      } else {
        moduleInfoPath = this.moduleInfoPath;
      }

      return new ModuleConfig(rootModule, dependencyTree, moduleInfo,
          topologicalSort, moduleToOutputPath, moduleInfoPath, productionUri);
    }
  }

  public static class ModuleInfo {
    private String name;
    private List<String> inputs;
    private List<String> deps;

    public ModuleInfo() {
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public List<String> getInputs() {
      return inputs;
    }

    public void setInputs(List<String> inputs) {
      Preconditions.checkNotNull(inputs);
      this.inputs = ImmutableList.copyOf(inputs);
    }

    public List<String> getDeps() {
      return deps;
    }

    public void setDeps(List<String> deps) {
      Preconditions.checkNotNull(deps);
      this.deps = ImmutableList.copyOf(deps);
    }

    @Override
    public String toString() {
      return name + ":" + getDeps();
    }
  }

  public static class BadDependencyTreeException extends Exception {

    private static final long serialVersionUID = -6236569978296669755L;

    BadDependencyTreeException(String message) {
      super(message);
    }
  }
}
