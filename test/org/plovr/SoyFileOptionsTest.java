package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class SoyFileOptionsTest {

  @Test
  public void testEmptyConstructor() {
    SoyFileOptions defaultOptions = new SoyFileOptions();
    assertTrue(defaultOptions.useClosureLibrary);
    assertEquals(ImmutableList.of(), defaultOptions.pluginModuleNames);
  }

  @Test
  public void testConstructor() {
    List<String> pluginModuleNames = ImmutableList.of("one", "two");
    SoyFileOptions options = new SoyFileOptions(pluginModuleNames, false);
    assertFalse(options.useClosureLibrary);
    assertEquals(ImmutableList.of("one", "two"), options.pluginModuleNames);
  }

  @Test
  public void testEqualsAndHashcode() {
    SoyFileOptions defaultOptions = new SoyFileOptions();
    SoyFileOptions defaultOptions2 = new SoyFileOptions();
    assertEquals(defaultOptions, defaultOptions2);
    assertEquals(defaultOptions.hashCode(), defaultOptions2.hashCode());

    List<String> pluginModuleNames = ImmutableList.of("one", "two");
    SoyFileOptions options = new SoyFileOptions(pluginModuleNames, false);
    assertFalse(defaultOptions.equals(options));
    assertTrue(defaultOptions.hashCode() != options.hashCode());

    SoyFileOptions options2 = new SoyFileOptions(pluginModuleNames, false);
    assertTrue(options.equals(options2));
    assertEquals(options.hashCode(), options2.hashCode());
  }
}
