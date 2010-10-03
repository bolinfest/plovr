package org.plovr;

import java.awt.Point;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

/**
 * {@link ModulesHandler} provides a visualization of the modules as an SVG.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class ModulesHandler extends AbstractGetHandler {

  private static final int X_OFFSET = 10;
  private static final int Y_OFFSET = 10;

  private static final int X_BOX_SPACING = 50;
  private static final int Y_BOX_SPACING = 75;

  private static final int BOX_HEIGHT = 100;
  private static final int BOX_WIDTH = 150;
  private static final int LINE_HEIGHT = 20;

  private static final int TEXT_X_OFFSET = 10;
  private static final int TEXT_Y_OFFSET = (BOX_HEIGHT + LINE_HEIGHT) / 2;

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
    String svg = generateSvg(moduleDepths,
        moduleConfig.getInvertedDependencyTree());

    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "image/svg+xml");
    exchange.sendResponseHeaders(200, svg.length());

    Writer responseBody = new OutputStreamWriter(exchange.getResponseBody());
    responseBody.write(svg);
    responseBody.close();
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

  @VisibleForTesting
  static String generateSvg(SetMultimap<Integer, String> moduleDepths,
      Map<String, List<String>> invertedDependencyTree) {
    // Calculate the maximum number of modules that should be displayed at the
    // same depth in the SVG.
    int maxModulesPerRow = -1;
    for (Collection<String> modules : moduleDepths.asMap().values()) {
      maxModulesPerRow = Math.max(maxModulesPerRow, modules.size());
    }

    // Create a rectangle for each of the modules in the graph.
    List<String> rects = Lists.newLinkedList();
    Map<String, Point> boxTops = Maps.newHashMap();
    Map<String, Point> boxBottoms = Maps.newHashMap();
    for (Map.Entry<Integer, Collection<String>> entry : moduleDepths.asMap().entrySet()) {
      int depth = entry.getKey();
      int y = Y_OFFSET + depth * (BOX_HEIGHT + Y_BOX_SPACING);
      int numModules = entry.getValue().size();
      int blankSpace = (maxModulesPerRow - numModules) * (BOX_WIDTH + X_BOX_SPACING) / 2;
      int x = blankSpace + X_OFFSET;

      for (String module : entry.getValue()) {
        String rect = String.format(
            "  <rect id='%s' x='%d' y='%d'" +
            " stroke='#000' fill='#FFF'" +
            " width='%d' height='%d'/>\n" +
            "  <text style='font-family: Arial'" +
            " x='%d' y='%d'>%s</text>",
            module, x, y, BOX_WIDTH, BOX_HEIGHT, x + TEXT_X_OFFSET, y + TEXT_Y_OFFSET, module);
        rects.add(rect);

        // Add the connection points for boxes.
        int boxMiddle = x + BOX_WIDTH / 2;
        boxTops.put(module, new Point(boxMiddle, y));
        boxBottoms.put(module, new Point(boxMiddle, y + BOX_HEIGHT));

        x += BOX_WIDTH + X_BOX_SPACING;
      }
    }

    // Create lines to connect modules.
    List<String> lines = Lists.newLinkedList();
    for (Map.Entry<String, List<String>> deps :
        invertedDependencyTree.entrySet()) {
      String module = deps.getKey();
      Point sink = boxBottoms.get(module);
      for (String descendant : deps.getValue()) {
        Point source = boxTops.get(descendant);
        lines.add(String.format(
            "  <line x1='%d' y1='%d' x2='%d' y2='%d' style='stroke:#006600;'/>",
            source.x, source.y, sink.x, sink.y));
      }
    }

    String svg = "<svg xmlns='http://www.w3.org/2000/svg' " +
        "xmlns:xlink='http://www.w3.org/1999/xlink'>\n" +
        Joiner.on("\n").join(rects) +
        "\n" +
        Joiner.on("\n").join(lines) +
        "\n</svg>\n";

    return svg;
  }
}
