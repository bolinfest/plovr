package org.plovr;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * {@code NameCollisionTest} is a regression test for
 * http://code.google.com/p/plovr/issues/detail?id=56}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */

public class NameCollisionTest {

  /**
   * Ensures that if a config contains paths with subpaths that mirror one
   * another, then there should not be any name collisions.
   */
  @Test
  public void testEqualResourceSubpaths() {
     PlovrRunner.run(ImmutableList.of("build", "testdata/name-collision/config.js"));
  }
}
