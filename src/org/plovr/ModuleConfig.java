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

import org.plovr.util.Pair;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.internal.ImmutableList;
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
   */
  Map<String, List<JsInput>> partitionInputsIntoModules(List<JsInput> inputs) {
    // Map of module inputs to module names.
    Map<String, String> moduleInputToName = Maps.newHashMap();
    for (ModuleInfo info : moduleInfoMap.values()) {
      moduleInputToName.put(info.getInput(), info.getName());
    }

    // Pick out the JS files that correspond to modules.
    Map<String, JsInput> moduleToInputMap = Maps.newHashMap();
    List<String> modulesInInputOrder = Lists.newLinkedList();
    for (JsInput input : inputs) {
      String inputName = input.getName();
      if (!moduleInputToName.containsKey(inputName)) {
        continue;
      }

      String moduleName = moduleInputToName.get(inputName);
      if (invertedDependencyTree.containsKey(moduleName)) {
        JsInput previousInput = moduleToInputMap.put(moduleName, input);
        if (previousInput != null) {
          throw new IllegalArgumentException("More than one input file for "
              + moduleName + " module: " + input.getName() + ", "
              + previousInput.getName());
        }
        modulesInInputOrder.add(moduleName);
      }
    }

    // Make sure that the last JsInput is an input file for a module.
    JsInput lastInput = inputs.get(inputs.size() - 1);
    if (!moduleInputToName.containsKey(lastInput.getName())) {
      throw new IllegalArgumentException(
          "The last JS file in the compilation must be a module input but was"
              + ": " + lastInput.getName());
    }

    // Ensure that every module has a corresponding input file.
    Sets.SetView<String> missingModules = Sets.difference(invertedDependencyTree
        .keySet(), moduleToInputMap.keySet());
    if (!missingModules.isEmpty()) {
      throw new IllegalArgumentException("The following modules did not have "
          + "input files: " + missingModules);
    }

    // Ensure that the order of the input files is a valid topological sort
    // of the module dependency graph.
    Set<String> visitedModules = Sets.newHashSet();
    for (String module : modulesInInputOrder) {
      for (String parent : moduleInfoMap.get(module).getDeps()) {
        if (!visitedModules.contains(parent)) {
          throw new IllegalArgumentException(parent + " should appear before "
              + module + " in " + Joiner.on("\n").join(inputs));
        }
      }
      visitedModules.add(module);
    }

    // It is imperative to use a LinkedHashMap so that the iteration order of
    // the map matches module order.
    Map<String, List<JsInput>> moduleToInputList = Maps.newLinkedHashMap();
    Iterator<JsInput> inputIterator = inputs.iterator();
    for (String moduleName : modulesInInputOrder) {
      List<JsInput> inputList = Lists.newLinkedList();
      JsInput lastInputInModule = moduleToInputMap.get(moduleName);
      while (inputIterator.hasNext()) {
        JsInput input = inputIterator.next();
        inputList.add(input);
        if (input == lastInputInModule) {
          break;
        }
      }
      moduleToInputList.put(moduleName, inputList);
    }

    return moduleToInputList;
  }

  private List<JSModule> oldGetModulesImpl(Manifest manifest) throws MissingProvideException {
    List<JsInput> inputs = manifest.getInputsInCompilationOrder();
    Map<String, List<JsInput>> moduleToInputList = partitionInputsIntoModules(inputs);

    // Create modules and add the source files that make up the module.
    List<JSModule> modules = Lists.newLinkedList();
    Map<String, JSModule> modulesByName = Maps.newHashMap();
    for (Map.Entry<String, List<JsInput>> entry : moduleToInputList.entrySet()) {
      String moduleName = entry.getKey();
      List<JsInput> inputList = entry.getValue();
      JSModule module = new JSModule(moduleName);
      for (JsInput moduleInput : inputList) {
        module.add(Manifest.inputToSourceFile.apply(moduleInput));
      }
      modules.add(module);
      modulesByName.put(moduleName, module);
    }

    // Add the dependencies for each module.
    for (JSModule module : modules) {
      String moduleName = module.getName();
      for (String parent : moduleInfoMap.get(moduleName).getDeps()) {
        module.addDependency(modulesByName.get(parent));
      }
    }

    return modules;
  }

  /**
   *
   * @param inputs
   *          This list is most likely to be produced by
   *          {@link Manifest#getInputsInCompilationOrder()}.
   * @return
   * @throws MissingProvideException
   */
  List<JSModule> getModules(Manifest manifest) throws MissingProvideException {
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

    if (Math.random() > -1 /* true */) {
      return buildModulesUsingOption1(manifest);
    } else {
      // TODO(bolinfest): Remove this when Option 1 has been throughly tested.
      return oldGetModulesImpl(manifest);
    }
  }

  private List<JSModule> buildModulesUsingOption1(Manifest manifest)
      throws MissingProvideException {
    List<JsInput> inputsInOrder = manifest.getInputsInCompilationOrder();
    // Remove the first item, base.js, from the list.
    inputsInOrder = inputsInOrder.subList(1, inputsInOrder.size());

    // 1. Find the set of transitive dependencies for each module using a
    // LinkedHashSet.
    // 2. For each module, iterate over its list of inputs and keep track of
    // which inputs have already been seen. For an input that is seen for the
    // first time, find the set of modules that contain that input in its set of
    // transitive dependencies. Once that set is created, find the least common
    // ancestor module for the members of the set, and add the input to that
    // module.
    // 3. Convert each list of JsInput dependencies for each module into a
    // JSModule and return the JSModules as a topologically sorted list.

    // Step 1: Build the set of transitive dependencies for each module.
    Map<String, LinkedHashSet<JsInput>> moduleToTransitiveDependencies = Maps
        .newHashMap();
    Map<String, JsInput> provideToSource = manifest.getProvideToSource(
        ImmutableSet.copyOf(inputsInOrder));
    for (String module : topologicalSort) {
      ModuleInfo moduleInfo = moduleInfoMap.get(module);
      JsInput primaryInput = manifest.getJsInputByName(moduleInfo.getInput());
      LinkedHashSet<JsInput> deps = new LinkedHashSet<JsInput>();
      manifest.buildDependencies(provideToSource, deps, primaryInput);
      moduleToTransitiveDependencies.put(module, deps);
    }

    // Step 2: Assign inputs to modules.
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

    // Step 3: Convert each list of JsInput dependencies for each module into a
    // JSModule.
    Map<String, JSModule> modulesByName = Maps.newHashMap();
    for (String module : topologicalSort) {
      // Create the module and add the dependencies in order.
      JSModule jsModule = new JSModule(module);
      List<JsInput> deps = moduleToInputs.get(module);
      for (JsInput dep : deps) {
        jsModule.add(Manifest.inputToSourceFile.apply(dep));
      }

      // Remember to special-case base.js so it is the first file in the root
      // module.
      if (module.equals(getRootModule())) {
        jsModule.addFirst(Manifest.inputToSourceFile.apply(manifest.getBaseJs()));
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
    String leastCommonAncestor = firstModule;
    while (iter.hasNext()) {
      String module = iter.next();
      leastCommonAncestor = searcher.findCommonAncestor(
          leastCommonAncestor, module);
    }

    return leastCommonAncestor;
  }

  private class Searcher {

    private final Map<Pair<String,String>,String> cache = Maps.newHashMap();

    private Searcher() {}

    private Pair<String,String> createSortedPair(String module1, String module2) {
      int comp = module1.compareTo(module2);
      Preconditions.checkState(comp != 0, "Should not be the same");
      if (comp < 0) {
        return Pair.of(module1, module2);
      } else {
        return Pair.of(module2, module1);
      }
    }

    /**
     * Returns the module that is the least common ancestor of the two specified
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
    public String findCommonAncestor(String module1, String module2) {
      if (module1.equals(module2)) {
        return module1;
      }

      Pair<String,String> p = createSortedPair(module1, module2);
      String ancestor = cache.get(p);
      if (ancestor != null) {
        return ancestor;
      }

      // Progressively check ancestors of each node.
      Map<String, Integer> modulesToVisitFrom1 = Maps.newHashMap();
      modulesToVisitFrom1.put(module1, 0);
      Map<String, Integer> modulesToVisitFrom2 = Maps.newHashMap();
      modulesToVisitFrom2.put(module2, 0);
      Map<String, Integer> expandedFromModule1 = Maps.newHashMap();
      Map<String, Integer> expandedFromModule2 = Maps.newHashMap();
      while (true) {
        // See whether there is a common reachable module found between module1
        // and module2. Make sure that the following case works correctly:
        //
        //       A
        //     / |
        //    B  C     Make sure that when searching for the least common
        //    |  | \   ancestor of C and F that C is determined to be the least
        //    |  D  E  common ancestor instead of another module, such as A.
        //     \ |     That would preclude the ability to determine C as the
        //       F     least common ancestor of {C,E,F}. In practice, that would
        //       |     unnecessarily add code to A that could be contained in C.
        //       G
        //
        SetView<String> intersection = Sets.intersection(
            expandedFromModule1.keySet(), expandedFromModule2.keySet());
        int size = intersection.size();
        if (size > 0) {
          if (size == 1) {
            ancestor = intersection.iterator().next();
          } else {
            // TODO(bolinfest): Prove that this heuristic is optimal.

            // This would happen in the {B,F} case where both B and A are
            // reached in two iterations. In that case, the min cost for B would
            // be 0 and the min cost for A would be 1, so B would be selected as
            // the least common ancestor, as desired.
            int minCostSeen = Integer.MAX_VALUE;
            for (String candidateAncestor : intersection) {
              int cost1 = modulesToVisitFrom1.get(candidateAncestor);
              int cost2 = modulesToVisitFrom2.get(candidateAncestor);
              int cost = Math.min(cost1, cost2);
              if (cost < minCostSeen) {
                minCostSeen = cost;
                ancestor = candidateAncestor;
              }
            }
          }
          break;
        } else {
          expandModulesToVisit(modulesToVisitFrom1, expandedFromModule1);
          expandModulesToVisit(modulesToVisitFrom2, expandedFromModule2);
        }
      }

      cache.put(p, ancestor);
      return ancestor;
    }

    private void expandModulesToVisit(Map<String, Integer> modulesToVisit,
        Map<String, Integer> expandedModules) {
      // Expand each module in the pending visitors list.
      // Copy the visitors list because the underlying Map will be modified as
      // part of this for loop:
      Map<String,Integer> modules = ImmutableMap.copyOf(modulesToVisit);
      for (Map.Entry<String,Integer> entry : modules.entrySet()) {
        String module = entry.getKey();
        // Check to see whether the module has already been expanded.
        if (expandedModules.containsKey(module)) {
          continue;
        }

        // Add to visited list.
        Integer cost = entry.getValue() + 1;
        for (String dep : moduleInfoMap.get(module).getDeps()) {
          if (!modulesToVisit.containsKey(dep)) {
            modulesToVisit.put(dep, cost);
          }
        }

        // Remove from visited list and add to expanded list.
        modulesToVisit.remove(module);
        expandedModules.put(entry.getKey(), entry.getValue());
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
      buildDependencies(dependencyTree, transitiveDependencies, rootModule);

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
     * @throws BadDependencyTreeException
     */
    private static void buildDependencies(
        Map<String, List<String>> dependencies,
        LinkedHashSet<String> transitiveDependencies, String dependency)
        throws BadDependencyTreeException {
      for (String dependencyList : dependencies.get(dependency)) {
        buildDependencies(dependencies, transitiveDependencies, dependencyList);
      }
      if (transitiveDependencies.contains(dependency)) {
        throw new BadDependencyTreeException("Circular dependency involving: "
            + dependency + " depends on: " + transitiveDependencies);
      }
      transitiveDependencies.add(dependency);
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
      Map<String, ModuleInfo> moduleInfo = Maps.newHashMap();
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
        Gson gson = new Gson();
        ModuleInfo moduleEntry = gson.fromJson(entry.getValue(),
            ModuleInfo.class);
        String name = entry.getKey();
        moduleEntry.setName(name);
        moduleInfo.put(name, moduleEntry);
      }

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
      logger.info(topologicalSort.toString());

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
    // This field is transient because it is used with Gson.
    private transient String name;
    private String input;
    private List<String> deps;

    public ModuleInfo() {
    }

    private void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    // TODO: It should be possible for a module to specify multiple inputs.
    public String getInput() {
      return input;
    }

    public List<String> getDeps() {
      return deps;
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
