package org.plovr;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import plovr.io.Files;

public class JsonCommentStripperTest extends TestCase {

  public void testSimpleInput() throws IOException {
    String json = JsonCommentStripper.stripCommentsFromJson(
        new File("test/org/plovr/json-comment-stripper-test-input.js"));
    String expectedOutput = Files.toString(new File(
        "test/org/plovr/json-comment-stripper-test-output.js"));
    assertEquals(expectedOutput, json);
  }
}
