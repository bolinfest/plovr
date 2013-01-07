package org.plovr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;

@SuppressWarnings("serial")
class TypeScriptCompilerException extends Exception {

  @VisibleForTesting
  static final Pattern LINE_AND_CHAR_NO =
      Pattern.compile("([^\\(]*)\\((\\d+),(\\d+)\\): (.*)\\n");

  private final String sourceFile;
  private final int lineno;
  private final int charno;
  private final String errorMessage;

  TypeScriptCompilerException(String message) {
    super(message);

    Matcher matcher = LINE_AND_CHAR_NO.matcher(message);
    if (matcher.matches()) {
      this.sourceFile = matcher.group(1);
      this.lineno = Integer.valueOf(matcher.group(2), 10);
      this.charno = Integer.valueOf(matcher.group(3), 10);
      this.errorMessage = matcher.group(4);
    } else {
      this.sourceFile = null;
      this.lineno = -1;
      this.charno = -1;
      this.errorMessage = null;
    }
  }

  public String getSourceFile() {
    return sourceFile;
  }

  public int getLineNumber() {
    return lineno;
  }

  public int getCharOffset() {
    return charno;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
