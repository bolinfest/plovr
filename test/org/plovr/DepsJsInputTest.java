package org.plovr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class DepsJsInputTest {

  @Test
  public void testGetNameSimple() {
    DummyJsInput baseJs = new DummyJsInput("base.js", "",
        ImmutableList.<String>of(), ImmutableList.<String>of());
    DepsJsInput depsJs = new DepsJsInput(baseJs, "source");
    assertEquals("deps.js", depsJs.getName());
    assertEquals("source", depsJs.getCode());
  }

  @Test
  public void testGetNameRegex() {
    DummyJsInput baseJs = new DummyJsInput("/closure/goog/base.js", "",
        ImmutableList.<String>of(), ImmutableList.<String>of());
    DepsJsInput depsJs = new DepsJsInput(baseJs, "source");
    assertEquals("/closure/goog/deps.js", depsJs.getName());
    assertEquals("source", depsJs.getCode());
  }
}
