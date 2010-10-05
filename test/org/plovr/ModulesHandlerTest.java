package org.plovr;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.plovr.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

/**
 * {@link ModulesHandlerTest} is a unit test for {@link ModulesHandler}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class ModulesHandlerTest {

  @Test
  public void testCalculateModuleDepths() {
    // This is a test for the following graph:
    //
    //       A      0
    //     / |
    //    B  C      1
    //    |  | \
    //    |  D  E   2
    //     \ |
    //       F      3
    //       |
    //       G      4
    //
    // Note how F should be considered to be at depth 3 rather than depth 2.
    // Similarly, G should be considered to be at depth 4 rather than depth 3.
    String root = "A";
    Map<String, List<String>> graph = ImmutableMap.<String, List<String>>builder()
        .put("A", ImmutableList.of("B", "C"))
        .put("B", ImmutableList.of("F"))
        .put("C", ImmutableList.of("D", "E"))
        .put("D", ImmutableList.of("F"))
        .put("E", ImmutableList.<String>of())
        .put("F", ImmutableList.of("G"))
        .put("G", ImmutableList.<String>of())
        .build();
    SetMultimap<Integer, String> depths = ModulesHandler.calculateModuleDepths(
        root, graph);
    assertEquals(ImmutableSet.of("A"), depths.get(0));
    assertEquals(ImmutableSet.of("B", "C"), depths.get(1));
    assertEquals(ImmutableSet.of("D", "E"), depths.get(2));
    assertEquals(ImmutableSet.of("F"), depths.get(3));
    assertEquals(ImmutableSet.of("G"), depths.get(4));

    Map<String, Pair<Integer,Integer>> moduleSizes =
        ImmutableMap.<String, Pair<Integer,Integer>>builder()
        .put("A", Pair.of(10000, 500))
        .put("B", Pair.of(10000, 500))
        .put("C", Pair.of(20000, 1000))
        .put("D", Pair.of(20000, 1000))
        .put("E", Pair.of(30000, 2500))
        .put("F", Pair.of(40000, 3000))
        .put("G", Pair.of(40000, 3000))
        .build();

    Map<String, List<JsInput>> moduleToInputs = ImmutableMap.<String, List<JsInput>>builder()
        .put("A", ImmutableList.<JsInput>of())
        .put("B", ImmutableList.<JsInput>of())
        .put("C", ImmutableList.<JsInput>of())
        .put("D", ImmutableList.<JsInput>of())
        .put("E", ImmutableList.<JsInput>of())
        .put("F", ImmutableList.<JsInput>of())
        .put("G", ImmutableList.<JsInput>of())
        .build();

    // For now, just call this to make sure it does not throw an exception.
    Pair<String, Dimension> svg = ModulesHandler.generateSvg(
        "fakeConfigId", depths, graph, moduleSizes, moduleToInputs);
    System.out.println(svg.getFirst());
  }
}
