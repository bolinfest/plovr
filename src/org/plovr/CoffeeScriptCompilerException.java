package org.plovr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;

/**
 * An exception thrown by the CoffeeScript compiler.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class CoffeeScriptCompilerException extends Exception {

  private static final long serialVersionUID = 1L;

  @VisibleForTesting
  static final Pattern LINE_AND_CHAR_NO =
      Pattern.compile("Parse error on line (\\d+):");

  private final int lineno;

  /**
   * @param message should be of the form:
   *     In INPUT, Parse error on line LINE_NUMBER: PARSE_ERROR.
   */
  public CoffeeScriptCompilerException(String message) {
    super(message);

    Matcher matcher = LINE_AND_CHAR_NO.matcher(message);
    if (matcher.find()) {
      lineno = Integer.valueOf(matcher.group(1), 10);
    } else {
      lineno = -1;
    }
  }

  public int getLineNumber() {
    return lineno;
  }
}
