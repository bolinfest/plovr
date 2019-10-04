package org.plovr;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Regression tests that run plovr and make sure the process exits successfully.
 */
public class IntegrationTest {

  /**
   * Ensures that if a config contains paths with subpaths that mirror one
   * another, then there should not be any name collisions.
   */
  @Test
  public void testEqualResourceSubpaths() {
     PlovrRunner.run(ImmutableList.of("build", "testdata/name-collision/config.js"));
  }
}
