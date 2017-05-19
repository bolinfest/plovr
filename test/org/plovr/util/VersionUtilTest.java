package org.plovr.util;

import com.google.common.base.Strings;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link VersionUtilTest} is a unit test for {@link VersionUtil}.
 */
public class VersionUtilTest {
  @Test
  public void testRead() {
    assertFalse(Strings.isNullOrEmpty(VersionUtil.getRevision("closure-library")));
    assertFalse(Strings.isNullOrEmpty(VersionUtil.getRevision("closure-compiler")));
    assertFalse(Strings.isNullOrEmpty(VersionUtil.getRevision("closure-templates")));
    assertFalse(Strings.isNullOrEmpty(VersionUtil.getRevision("plovr")));
  }
}
