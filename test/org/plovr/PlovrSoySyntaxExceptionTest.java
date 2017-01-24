package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.base.SoySyntaxException;

/**
 * {@link PlovrSoySyntaxExceptionTest} is a unit test for
 * {@link PlovrSoySyntaxException}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class PlovrSoySyntaxExceptionTest {

  private final DummyJsInput input = new DummyJsInput(
      "dummy.soy",
      "", /* code */
      ImmutableList.<String>of(), /* provides */
      ImmutableList.<String>of(), /* requires */
      true, /* soyFile */
      "" /* templateCode */);

  @Test
  public void testErrorMessageWithLineInformationWithBrackets() {
    String errorMessage = "Left brace '{' not allowed within a Soy tag " +
        "delimited by single braces (consider using double braces to delimit " +
        "the Soy tag) [line 13, column 1].";
    Matcher matcher = PlovrSoySyntaxException.LINE_AND_CHAR_NO.matcher(
        errorMessage);
    assertTrue(matcher.find());
    assertEquals("13", matcher.group(1));
    assertEquals("1", matcher.group(2));
  }

  @Test
  public void testErrorMessageWithLineInformationNoBrackets() {
    String errorMessage = "template .base: Encountered \"<EOF>\" at line 55, " +
        "column 7.\nWas expecting one of...";
    Matcher matcher = PlovrSoySyntaxException.LINE_AND_CHAR_NO.matcher(
        errorMessage);
    assertTrue(matcher.find());
    assertEquals("55", matcher.group(1));
    assertEquals("7", matcher.group(2));
  }
}
