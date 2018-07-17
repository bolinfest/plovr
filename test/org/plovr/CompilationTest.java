package org.plovr;

import com.google.common.collect.Lists;
import com.google.javascript.jscomp.JsMessage;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

  @Test
  public void testExtractMessages() throws Exception {
    Config config = ConfigParser.parseFile(new File("testdata/issue131/plovr.json"));
    Compilation compilation = Compilation.create(config);
    ArrayList<JsMessage> messages = Lists.newArrayList(compilation.extractMessages());
    assertEquals(1, messages.size());
  }

  @Test
  public void testTypeCheck() throws Exception {
    Config config = ConfigParser.parseFile(new File("testdata/issue154/plovr.json"));
    Compilation compilation = Compilation.create(config);
    compilation.compile();

    List<CompilationError> errors = compilation.getCompilationErrors();
    assertEquals(1, errors.size());
    assertTrue(errors.get(0).getMessage().contains("inconsistent return type"));
  }

}
