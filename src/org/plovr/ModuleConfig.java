package org.plovr;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.javascript.jscomp.JSModule;

public final class ModuleConfig {

  private static final Logger logger = Logger.getLogger(
      ModuleConfig.class.getName());

  private static Pattern MODULE_INIT_FILE = Pattern.compile(
      "([^/\\\\]*)_init\\.js$");

  private final String rootModule;

  private final Map<String, List<String>> dependencyTree;

  private final Map<String, List<String>> invertedDependencyTree;

  private Map<String, File> moduleToOutputPath;

  private ModuleConfig(
      String rootModule,
      Map<String, List<String>> dependencyTree,
      Map<String, List<String>> invertedDependencyTree) {
    this.rootModule = rootModule;
    this.dependencyTree = dependencyTree;
    this.invertedDependencyTree = invertedDependencyTree;
  }

  public String getRootModule() {
    return rootModule;
  }

  public Iterable<String> getModuleNames() {
    return Iterables.unmodifiableIterable(dependencyTree.keySet());
  }

  public void setModuleToOutputPath(Map<String, File> moduleToOutputPath) {
    this.moduleToOutputPath = ImmutableMap.copyOf(moduleToOutputPath);
  }

  public Map<String, File> getModuleToOutputPath() {
    return this.moduleToOutputPath;
  }

  public Map<String, List<String>> getInvertedDependencyTree() {
    return invertedDependencyTree;
  }

  /**
   *
   * @param inputs This list is most likely to be produced by
   *     {@link Manifest#getInputsInCompilationOrder()}.
   * @return
   */
  Map<String, List<JsInput>> partitionInputsIntoModules(List<JsInput> inputs) {

    // Pick out the _init.js files that correspond to modules.
    Map<String, JsInput> moduleToInitInputMap = Maps.newHashMap();
    List<String> modulesInInputOrder = Lists.newLinkedList();
    for (JsInput input : inputs) {
      String name = input.getName();
      Matcher matcher = MODULE_INIT_FILE.matcher(name);
      if (!matcher.find()) {
        continue;
      }
      String moduleName = matcher.group(1);
      if (dependencyTree.containsKey(moduleName)) {
        JsInput previousInput = moduleToInitInputMap.put(moduleName, input);
        if (previousInput != null) {
          throw new IllegalArgumentException("More than one init file for " +
              moduleName + " module: " + input.getName() + ", " +
              previousInput.getName());
        }
        modulesInInputOrder.add(moduleName);
      }
    }

    // TODO(bolinfest): Make sure that the last input is an _init.js file.

    // Ensure that every module has a corresponding _init.js file.
    Sets.SetView<String> missingModules = Sets.difference(dependencyTree.keySet(),
        moduleToInitInputMap.keySet());
    if (!missingModules.isEmpty()) {
      throw new IllegalArgumentException("The following modules did not have " +
          "init files: " + missingModules);
    }

    // Ensure that the order of the _init.js files is a valid topological sort
    // of the module dependency graph.
    Set<String> visitedModules = Sets.newHashSet();
    for (String module : modulesInInputOrder) {
      for (String parent : invertedDependencyTree.get(module)) {
        if (!visitedModules.contains(parent)) {
          throw new IllegalArgumentException(parent + " should appear before " +
              module + " in " + Joiner.on("\n").join(inputs));
        }
      }
      visitedModules.add(module);
    }

    Map<String, List<JsInput>> moduleToInputList = Maps.newHashMap();
    Iterator<JsInput> inputIterator = inputs.iterator();
    for (String moduleName : modulesInInputOrder) {
      List<JsInput> inputList = Lists.newLinkedList();
      JsInput lastInputInModule = moduleToInitInputMap.get(moduleName);
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
      for (String parent : invertedDependencyTree.get(moduleName)) {
        module.addDependency(modulesByName.get(parent));
      }
    }

    return modules;
  }

  /**
   *
   * @param json
   * @return
   * @throws BadDependencyTreeException
   */
  public static ModuleConfig create(JsonObject json) throws
      BadDependencyTreeException {
    JsonElement deps = json.get("deps");
    Preconditions.checkNotNull(deps, "modules must have a property named deps");
    Preconditions.checkArgument(deps.isJsonObject(), "deps must be a map");

    // The dependency tree is "inverted" because children point to their
    // parents instead of the other way around.
    final Map<String, List<String>> invertedDependencyTree = extractDependencies(
        deps.getAsJsonObject());

    // In dependencyTree, modules point to the modules that depend on them.
    final Map<String, List<String>> dependencyTree = invertDependencyTree(
        invertedDependencyTree);

    String rootModule = findRootModule(invertedDependencyTree);
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

    ModuleConfig moduleConfig = new ModuleConfig(rootModule, dependencyTree,
        invertedDependencyTree);
    return moduleConfig;
  }

  private static Map<String, List<String>> extractDependencies(JsonObject deps) {
    Map<String, List<String>> dependencies = Maps.newHashMap();
    for (Map.Entry<String,JsonElement> entry : deps.entrySet()) {
      JsonElement element = entry.getValue();
      if (element.isJsonArray()) {
        List<String> depList = Lists.newLinkedList();
        JsonArray array = element.getAsJsonArray();
        for (JsonElement dependency : array) {
          if (dependency.isJsonPrimitive()) {
            JsonPrimitive primitive = dependency.getAsJsonPrimitive();
            if (primitive.isString()) {
              depList.add(primitive.getAsString());
            }
          }
        }
        dependencies.put(entry.getKey(), depList);
      }
    }
    return dependencies;
  }

  /**
   * @param dependencies
   * @return the name of the root module
   * @throws BadDependencyTreeException if the dependencies are not well-formed.
   */
  private static String findRootModule(Map<String, List<String>> dependencies)
      throws BadDependencyTreeException {
    String moduleWithNoDependencies = null;
    for (Map.Entry<String, List<String>> entry : dependencies.entrySet()) {
      List<String> moduleDeps = entry.getValue();
      if (moduleDeps.size() == 0) {
        if (moduleWithNoDependencies == null) {
          moduleWithNoDependencies = entry.getKey();
        } else {
          throw new BadDependencyTreeException("Both " + moduleWithNoDependencies +
              " and " + entry.getKey() + " have no dependencies, so this" +
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
      Map<String, List<String>> invertedDependencyTree)
      throws BadDependencyTreeException {
    // dependencies maps a module to the modules that depend on it. A module
    // must be loaded before any of its dependencies.
    Map<String, List<String>> dependencies = Maps.newHashMap();

    // Populate the keys of dependencies with the keys of the "deps" object
    // literal from the config file.
    for (String module : invertedDependencyTree.keySet()) {
      dependencies.put(module, Lists.<String>newLinkedList());
    }

    for (Map.Entry<String, List<String>> entry : invertedDependencyTree.entrySet()) {
      String dependentModule = entry.getKey();
      for (String ancestorModule : entry.getValue()) {
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

  private static List<String> buildDependencies(
      Map<String, List<String>> dependencyTree,
      String dependency)
      throws BadDependencyTreeException {
    LinkedHashSet<String> transitiveDependencies = new LinkedHashSet<String>();
    buildDependencies(dependencyTree, transitiveDependencies, dependency);

    // Because the dependencies were built up in reverse order, add the
    // results of the iterator in reverse order to create a new list.
    LinkedList<String> dependencies = Lists.newLinkedList();
    for (String module : transitiveDependencies) {
      dependencies.addFirst(module);
    }
    return dependencies;
  }

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
          dependency);
    }
    transitiveDependencies.add(dependency);
  }

  public static class BadDependencyTreeException extends Exception {

    private static final long serialVersionUID = -6236569978296669755L;

    BadDependencyTreeException(String message) {
      super(message);
    }
  }
}
