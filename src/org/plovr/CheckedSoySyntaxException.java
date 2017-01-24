package org.plovr;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;

public final class CheckedSoySyntaxException extends CompilationException.Single {

  private static final long serialVersionUID = 1L;

  private static final DiagnosticType SOY_SYNTAX_EXCEPTION =
    DiagnosticType.error("PLOVR_SOY_SYNTAX_EXCEPTION", "{0}");

  private final PlovrSoySyntaxException plovrSoySyntaxException;

  public CheckedSoySyntaxException(PlovrSoySyntaxException e) {
    super(e);
    this.plovrSoySyntaxException = e;
  }

  private String getInputPath() {
    return plovrSoySyntaxException.getInput().getName();
  }

  @Override
  public String getMessage() {
    return plovrSoySyntaxException.getMessage();
  }

  @Override
  public CompilationError createCompilationError() {
    int lineno;
    int charno;
    lineno = plovrSoySyntaxException.getLineNumber();
    charno = plovrSoySyntaxException.getCharNumber();
    JSError jsError = JSError.make(
        getInputPath(),
        lineno,
        charno,
        CheckLevel.ERROR, SOY_SYNTAX_EXCEPTION,
        getMessage());
    return new CompilationError(jsError);
  }
}
