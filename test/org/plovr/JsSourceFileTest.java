package org.plovr;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

/**
 * {@link JsSourceFileTest} is a unit test for {@link JsSourceFile}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class JsSourceFileTest extends TestCase {

  public void testProvidesAndRequireParsing() {
    String path = "test/org/plovr/example.js";
    File testFile = new File(path);
    assertTrue("Test file could not be found: " + testFile, testFile.exists());

    JsInput input = new JsSourceFile(path, testFile);
    List<String> provides = input.getProvides();
    assertEquals(2, provides.size());
    assertTrue(provides.contains("example.Foo"));
    assertTrue(provides.contains("example.Bar"));

    List<String> requires = input.getRequires();
    assertEquals(1, requires.size());
    assertTrue(requires.contains("goog.events"));
  }
}
