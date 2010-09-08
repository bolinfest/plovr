package org.plovr.soy.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

/**
 * {@link FileUtilTest} is a unit test for {@link File}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class FileUtilTest {

  /**
   * Make sure that the parent of a root directory is null, not itself.
   */
  @Test
  public void testIsOwnParent() {
    File[] roots = File.listRoots();
    assertNotNull(roots);
    assertTrue(roots.length > 0);
    File root = roots[0];
    assertTrue(root.isDirectory());
    assertEquals(null, root.getParentFile());
  }

  @Test
  public void testDirectoryContainsItself() {
    File root = File.listRoots()[0];
    assertTrue(FileUtil.contains(root, root));
  }

  @Test
  public void testDirectoryContainsChild() {
    File parent = File.listRoots()[0];
    File child = parent.listFiles()[0];
    assertTrue(FileUtil.contains(parent, child));
    assertFalse(FileUtil.contains(child, parent));
  }
}
