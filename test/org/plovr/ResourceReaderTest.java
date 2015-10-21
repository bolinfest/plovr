package org.plovr;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * {@link ResourceReaderTest} is a unit test for {@link ResourceReader}.
 */
public class ResourceReaderTest {
  @Test
  public void testClosureLibrarySources() throws Exception {
      List<JsInput> srcs = ResourceReader.getClosureLibrarySources();
      for (JsInput src : srcs) {
          assertTrue(!src.getCode().isEmpty());
      }
  }
}
