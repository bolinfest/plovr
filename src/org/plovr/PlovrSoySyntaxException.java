package org.plovr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.template.soy.base.SoySyntaxException;

/**
 * {@link PlovrSoySyntaxException} wraps a {@link SoySyntaxException} so that it
 * can display a plovr-specific error message.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class PlovrSoySyntaxException extends UncheckedCompilationException {

  private static final long serialVersionUID = 1L;

  @VisibleForTesting
  static final Pattern LINE_AND_CHAR_NO =
      Pattern.compile("\\[?line (\\d+), column (\\d+)\\]?");

  private final SoySyntaxException soySyntaxException;
  private final JsInput input;
  private final int lineno;
  private final int charno;

  public PlovrSoySyntaxException(SoySyntaxException e, JsInput input) {
    super(e);
    this.soySyntaxException = e;
    this.input = input;

    String soyErrorMsg = soySyntaxException.getMessage();
    Matcher matcher = PlovrSoySyntaxException.LINE_AND_CHAR_NO.matcher(
        soyErrorMsg);
    if (matcher.find()) {
      lineno = Integer.valueOf(matcher.group(1), 10);
      charno = Integer.valueOf(matcher.group(2), 10);
    } else {
      lineno = -1;
      charno = -1;
    }
  }

  @Override
  public String getMessage() {
    String soyErrorMsg = soySyntaxException.getMessage();

    // If the line number is available, format the message as Compiler errors
    // are formatted so it will get hyperlinked appropriately by
    // src/org/plovr/plovr.js
    if (lineno > 0) {
      return String.format("%s:%d: ERROR - %s",
          input.getName(),
          lineno,
          soyErrorMsg);
    } else {
      return soyErrorMsg;
    }
  }

  public int getLineNumber() {
    return lineno;
  }

  public int getCharNumber() {
    return charno;
  }

  public JsInput getInput() {
    return input;
  }

  @Override public CompilationException toCheckedException() {
    return new CheckedSoySyntaxException(this);
  }
}
