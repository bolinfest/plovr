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
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.javascript.jscomp.JSModule;

public final class ModuleConfig {

  private static final Logger logger = Logger.getLogger(
      ModuleConfig.class.getName());

  private final String rootModule;

  private final String productionUri;

  private final Map<String, List<String>> dependencyTree;

  private final Map<String, ModuleInfo> moduleInfo;

  private final Map<String, File> moduleToOutputPath;

  private final File moduleInfoPath;

  private ModuleConfig(
      String rootModule,
      Map<String, List<String>> dependencyTree,
      Map<String, ModuleInfo> moduleInfo,
      Map<String, File> moduleToOutputPath,
      File moduleInfoPath,
      String productionUri) {
    this.rootModule = rootModule;
    this.dependencyTree = dependencyTree;
    this.moduleInfo = moduleInfo;
    this.moduleToOutputPath = moduleToOutputPath;
    this.moduleInfoPath = moduleInfoPath;
    this.productionUri = productionUri;
  }

  public String getRootModule() {
    return rootModule;
  }

  public Iterable<String> getModuleNames() {
    return Iterables.unmodifiableIterable(dependencyTree.keySet());
  }

  public Map<String, File> getModuleToOutputPath() {
    return this.moduleToOutputPath;
  }

  public Map<String, ModuleInfo> getInvertedDependencyTree() {
    return moduleInfo;
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

  public Function<String,String> createModuleNameToUriFunction() {
    final String productionUri = getProductionUri();
    Function<String, String> moduleNameToUri = new Function<String, String>() {
      @Override
      public String apply(String moduleName) {
        return productionUri.replace("%s", moduleName);
      }
    };
    return moduleNameToUri;
  }

  /**
   *
   * @param inputs This list is most likely to be produced by
   *     {@link Manifest#getInputsInCompilationOrder()}.
   * @return
   */
  Map<String, List<JsInput>> partitionInputsIntoModules(List<JsInput> inputs) {
    // Map of module inputs to module names.
    Map<String, String> moduleInputToName = Maps.newHashMap();
    for (ModuleInfo info : moduleInfo.values()) {
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
      if (dependencyTree.containsKey(moduleName)) {
        JsInput previousInput = moduleToInputMap.put(moduleName, input);
        if (previousInput != null) {
          throw new IllegalArgumentException("More than one input file for " +
              moduleName + " module: " + input.getName() + ", " +
              previousInput.getName());
        }
        modulesInInputOrder.add(moduleName);
      }
    }

    // Make sure that the last JsInput is an input file for a module.
    JsInput lastInput = inputs.get(inputs.size() - 1);
    if (!moduleInputToName.containsKey(lastInput.getName())) {
      throw new IllegalArgumentException(
          "The last JS file in the compilation must be a module input but was" +
          ": " + lastInput.getName());
    }

    // Ensure that every module has a corresponding input file.
    Sets.SetView<String> missingModules = Sets.difference(dependencyTree.keySet(),
        moduleToInputMap.keySet());
    if (!missingModules.isEmpty()) {
      throw new IllegalArgumentException("The following modules did not have " +
          "input files: " + missingModules);
    }

    // Ensure that the order of the input files is a valid topological sort
    // of the module dependency graph.
    Set<String> visitedModules = Sets.newHashSet();
    for (String module : modulesInInputOrder) {
      for (String parent : moduleInfo.get(module).getDeps()) {
        if (!visitedModules.contains(parent)) {
          throw new IllegalArgumentException(parent + " should appear before " +
              module + " in " + Joiner.on("\n").join(inputs));
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

  /**
   *
   * @param inputs This list is most likely to be produced by
   *     {@link Manifest#getInputsInCompilationOrder()}.
   * @return
   */
  List<JSModule> getModules(List<JsInput> inputs) {
    Map<String, List<JsInput>> moduleToInputList = partitionInputsIntoModules(
        inputs);

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
      for (String parent : moduleInfo.get(moduleName).getDeps()) {
        module.addDependency(modulesByName.get(parent));
      }
    }

    return modules;
  }

  /**
   * @param dependencies
   * @return the name of the root module
   * @throws BadDependencyTreeException if the dependencies are not well-formed.
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
          throw new BadDependencyTreeException("Both " + moduleWithNoDependencies +
              " and " + info.getName() + " have no dependencies, so this" +
              " dependency graph does not form a tree");
        }
      }
    }

    if (moduleWithNoDependencies == null) {
      throw new BadDependencyTreeException("There was no module with zero" +
          " dependencies, so this tree has no root.");
    }

    return moduleWithNoDependencies;
  }

  private static Map<String, List<String>> invertDependencyTree(
      Collection<ModuleInfo> moduleInfo)
      throws BadDependencyTreeException {
    // dependencies maps a module to the modules that depend on it. A module
    // must be loaded before any of its dependencies.
    Map<String, List<String>> dependencies = Maps.newHashMap();

    // Populate the keys of dependencies with the keys of the "deps" object
    // literal from the config file.
    for (ModuleInfo info : moduleInfo) {
      dependencies.put(info.getName(), Lists.<String>newLinkedList());
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
          throw new BadDependencyTreeException(ancestorModule +
              " is not a key in the \"deps\" map");
        }
      }
    }

    return dependencies;
  }

  /**
   *
   * @param dependencyTree map of module to modules that depend on it
   * @param rootModule the root module of the tree
   * @return
   * @throws BadDependencyTreeException
   */
  private static List<String> buildDependencies(
      Map<String, List<String>> dependencyTree,
      String rootModule)
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
   * @param transitiveDependencies the Iterator of this set returns the
   *     dependencies in reverse order
   * @param dependency
   * @throws BadDependencyTreeException
   */
  private static void buildDependencies(
      Map<String, List<String>> dependencies,
      LinkedHashSet<String> transitiveDependencies,
      String dependency)
      throws BadDependencyTreeException {
    for (String dependencyList : dependencies.get(dependency)) {
      buildDependencies(dependencies, transitiveDependencies, dependencyList);
    }
    if (transitiveDependencies.contains(dependency)) {
      throw new BadDependencyTreeException("Circular dependency involving: " +
          dependency + " depends on: " + transitiveDependencies);
    }
    transitiveDependencies.add(dependency);
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

      this.dependencyTree = moduleConfig.dependencyTree;
      this.moduleInfo = moduleConfig.moduleInfo;
    }

   /**
    *
    * @param json
    * @return
    * @throws BadDependencyTreeException
    */
   public void setModuleInfo(JsonObject json) throws
       BadDependencyTreeException {

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
     final Map<String, List<String>> dependencyTree = invertDependencyTree(
         moduleInfo.values());

     String rootModule = findRootModule(moduleInfo.values());
     // Calling buildDependencies() confirms that the module dependencies are
     // well-formed by producing a topological sort of the modules.
     // For example, if the config file contained:
     //
     // "deps" : {
     //   "A": [],                          A
     //   "B": ["A"],                     /   \
     //   "C": ["A"],                   B       C
     //   "D": ["B","C"],                 \   /   \
     //   "E": ["C"]                        D       E
     // }
     //
     // Then the iteration of transitiveDependencies would produce either:
     //
     // ["A", "B", "C", "D", "E"] OR ["A", "B", "C", "E", "D"]
     //
     // Both are valid results because the topological sort of the dependency
     // graph is not unique.
     List<String> topologicalSort = buildDependencies(dependencyTree, rootModule);
     logger.info(topologicalSort.toString());

     this.rootModule = rootModule;
     this.dependencyTree = dependencyTree;
     this.moduleInfo = moduleInfo;
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
          String partialPath = outputPath.replace("%s", moduleName);
          File moduleFile = new File(ConfigOption.maybeResolvePath(
              partialPath, relativePathBase));
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
          moduleInfoPath = new File(
              ConfigOption.maybeResolvePath(infoPath, relativePathBase));
        }
      } else {
        moduleInfoPath = this.moduleInfoPath;
      }

      return new ModuleConfig(
          rootModule,
          dependencyTree,
          moduleInfo,
          moduleToOutputPath,
          moduleInfoPath,
          productionUri);
    }
  }

  public static class ModuleInfo {
    private transient String name;
    private String input;
    private List<String> deps;

    public ModuleInfo() {}

    private void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

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
