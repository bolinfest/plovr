package org.plovr;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ConfigPathTest {

  @Test
  public void testNormalize() {
    assertEquals("foo", ConfigPath.normalize("foo", false /*isDirectory*/));
    assertEquals("foo/", ConfigPath.normalize("foo", true /*isDirectory*/));
    assertEquals("foo/", ConfigPath.normalize("foo/", true /*isDirectory*/));
    assertEquals("foo/", ConfigPath.normalize("foo\\", true /*isDirectory*/));
    assertEquals("foo\\bar/", ConfigPath.normalize("foo\\bar", true /*isDirectory*/));
  }
}
