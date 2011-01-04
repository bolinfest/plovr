package org.plovr;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.template.soy.base.SoySyntaxException;

public final class CheckedSoySyntaxException extends CompilationException {

  private static final long serialVersionUID = 1L;

  private static final DiagnosticType SOY_SYNTAX_EXCEPTION =
    DiagnosticType.error("PLOVR_SOY_SYNTAX_EXCEPTION", "{0}");

  private final SoySyntaxException soySyntaxException;
  private final PlovrSoySyntaxException plovrSoySyntaxException;

  public CheckedSoySyntaxException(SoySyntaxException e) {
    super(e);
    this.soySyntaxException = e;
    this.plovrSoySyntaxException = null;
  }

  public CheckedSoySyntaxException(PlovrSoySyntaxException e) {
    super(e);
    this.soySyntaxException = null;
    this.plovrSoySyntaxException = e;
  }

  private String getInputPath() {
    if (soySyntaxException != null) {
      return soySyntaxException.filePath;
    } else {
      return plovrSoySyntaxException.getInput().getName();
    }
  }

  @Override
  public String getMessage() {
    if (soySyntaxException != null) {
      return soySyntaxException.getMessage();
    } else {
      return plovrSoySyntaxException.getMessage();
    }    
  }

  @Override
  public CompilationError createCompilationError() {
    int lineno;
    int charno;
    if (soySyntaxException != null) {
      lineno = -1;
      charno = -1;
    } else {
      lineno = plovrSoySyntaxException.getLineNumber();
      charno = plovrSoySyntaxException.getCharNumber();
    }
    JSError jsError = JSError.make(
        getInputPath(),
        lineno,
        charno,
        CheckLevel.ERROR, SOY_SYNTAX_EXCEPTION,
        getMessage());
    return new CompilationError(jsError);
  }
}
