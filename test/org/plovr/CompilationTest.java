package org.plovr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * {@link CompilationTest} is a unit test for {@Compilation}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class CompilationTest {

  @Test
  public void testInsertFingerprintIntoName() {
    assertEquals("foo/bar_2XBD4C.js",
        Compilation.insertFingerprintIntoName("foo/bar_.js", "2XBD4C"));
    assertEquals("foo/bar2XBD4C",
        Compilation.insertFingerprintIntoName("foo/bar", "2XBD4C"));
  }

  @Test(expected=NullPointerException.class)
  public void testBadFilePathToInsertFingerprintIntoName() {
    Compilation.insertFingerprintIntoName(null, "2XBD4C");
  }

  @Test(expected=NullPointerException.class)
  public void testBadFingerprintToInsertFingerprintIntoName() {
    Compilation.insertFingerprintIntoName("foo/bar_.js", null);
  }

}
