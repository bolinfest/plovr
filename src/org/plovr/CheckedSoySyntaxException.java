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

  public CheckedSoySyntaxException(SoySyntaxException e) {
    super(e);
    this.soySyntaxException = e;
  }

  public SoySyntaxException getSoySyntaxException() {
    return soySyntaxException;
  }

  @Override
  public CompilationError createCompilationError() {
    final int lineno = -1;
    final int charno = -1;
    // TODO(bolinfest): Get the name of the JsInput that caused the exception
    // and use that instead of soySyntaxException.filePath.
    JSError jsError = JSError.make(soySyntaxException.filePath, lineno, charno,
        CheckLevel.ERROR, SOY_SYNTAX_EXCEPTION, soySyntaxException.getMessage());
    return new CompilationError(jsError);
  }
}
