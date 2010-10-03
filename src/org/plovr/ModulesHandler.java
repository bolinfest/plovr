package org.plovr;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.sun.net.httpserver.HttpExchange;

public final class ModulesHandler extends AbstractGetHandler {

  public ModulesHandler(CompilationServer server) {
    super(server);
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    ModuleConfig moduleConfig = config.getModuleConfig();
    SetMultimap<Integer, String> moduleDepths =
        calculateModuleDepths(moduleConfig.getRootModule(),
            moduleConfig.getInvertedDependencyTree());
  }

  /**
   * For each node in the graph, we want to find the longest path from the
   * node to the root. The length of the longest path is the depth at which
   * the node should be drawn in the visualization of the graph.
   */
  @VisibleForTesting
  static SetMultimap<Integer, String> calculateModuleDepths(String root,
      Map<String, List<String>> graph) {

    // To determine the longest path for each node, progressively descend
    // down the inverted dependency tree. Keep track of each module found at
    // that depth and record the deepest point at which the module was seen.
    SetMultimap<Integer, String> modulesAtDepth = HashMultimap.create();
    modulesAtDepth.put(0, root);
    Map<String, Integer> moduleToDepth = Maps.newHashMap();
    moduleToDepth.put(root, 0);

    int depth = 0;
    while (true) {
      Set<String> modules = modulesAtDepth.get(depth);
      if (modules.isEmpty()) {
        break;
      }
      int newDepth = ++depth;

      // For each module at the current depth, collect of its descendants so
      // they can be inserted in modulesAtDepth at their new depth.
      Set<String> atNewDepth = Sets.newHashSet();
      for (String module : modules) {
        List<String> descendants = graph.get(module);
        for (String descendant : descendants) {
          atNewDepth.add(descendant);
        }
      }

      // A module in atNewDepth may already be in the modulesAtDepth multimap.
      // If so, then the key with which it is associated in the multimap must
      // be changed. The moduleToDepth map is used to keep track of where each
      // module is in the multimap for quick lookup, so moduleToDepth must be
      // kept up to date, as well.
      for (String module : atNewDepth) {
        if (moduleToDepth.containsKey(module)) {
          int oldDepth = moduleToDepth.remove(module);
          modulesAtDepth.remove(oldDepth, module);
        }
        moduleToDepth.put(module, newDepth);
        modulesAtDepth.put(newDepth, module);
      }
    }

    return modulesAtDepth;
  }
}
