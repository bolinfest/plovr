package org.plovr;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class JsonCommentStripperTest extends TestCase {

  public void testSimpleInput() throws IOException {
    String json = JsonCommentStripper.stripCommentsFromJson(
        new File("test/org/plovr/json-comment-stripper-test-input.js"));
    String expectedOutput = Files.toString(new File(
        "test/org/plovr/json-comment-stripper-test-output.js"), Charsets.UTF_8);
    assertEquals(expectedOutput, json);
  }
}
