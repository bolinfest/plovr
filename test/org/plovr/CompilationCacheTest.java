package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CompilationCacheTest {
  @Test
  public void testNeedsRecompile() throws Exception {
    DummyJsInput input = new DummyJsInput("input1.js", "console.log('hello!')");
    Config config = Config.builderForTesting()
        .setId("recompileTest")
        .addInput(input)
        .build();

    CompilationCache cache = new CompilationCache();
    assertFalse(cache.haveInputsChangedSince(config, 100));

    input.lastModified = 50;
    assertFalse(cache.haveInputsChangedSince(config, 100));

    input.lastModified = 200;
    assertTrue(cache.haveInputsChangedSince(config, 100));
  }

  @Test
  public void testCacheKeyChange() throws Exception {
    DummyJsInput input = new DummyJsInput("input1.js", "c");
    Config config1 = Config.builderForTesting()
        .setId("cacheKeyChangeTest")
        .addInput(input)
        .build();
    Config config2 = Config.builder(config1)
        .setCompilationMode(CompilationMode.ADVANCED)
        .build();

    CompilationCache cache = new CompilationCache();
    cache.put(config1, "console.log('config1')", 100);
    cache.put(config2, "console.log('config2')", 200);

    assertEquals("console.log('config1')", cache.getIfUpToDate(config1));
    assertEquals("console.log('config2')", cache.getIfUpToDate(config2));
  }

  @Test
  public void testInputChange() throws Exception {
    DummyJsInput input = new DummyJsInput("input1.js", "c");
    Config config = Config.builderForTesting()
        .setId("inputChangeTest")
        .addInput(input)
        .build();

    CompilationCache cache = new CompilationCache();
    cache.put(config, "console.log('config')", 100);

    assertEquals("console.log('config')", cache.getIfUpToDate(config));
    input.lastModified = 200;
    assertEquals(null, cache.getIfUpToDate(config));
  }
}
