package org.plovr;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;

public final class MissingProvideException extends CompilationException {

  private static final long serialVersionUID = 1L;

  private static final DiagnosticType MISSING_PROVIDE =
    DiagnosticType.error("PLOVR_MISSING_PROVIDE", "Missing provide for {0} in {1}");

  private final JsInput input;

  private final String missingProvide;

  // TODO(bolinfest): Get the line number and character offset where the
  // error occurred so it is possible to link to it.

  /**
   * @param input the file that contains the goog.require() call.
   * @param missingProvide the thing that was required.
   */
  MissingProvideException(JsInput input, String missingProvide) {
    super(String.format("Missing provide for %s in %s", missingProvide,
        input.getName()));
    this.input = input;
    this.missingProvide = missingProvide;
  }

  @Override
  public CompilationError createCompilationError() {
    final int lineno = -1;
    final int charno = -1;
    JSError jsError = JSError.make(getInput().getName(), lineno, charno,
        CheckLevel.ERROR, MISSING_PROVIDE, getMissingProvide(),
        getInput().getName());
    return new CompilationError(jsError);
  }

  public JsInput getInput() {
    return input;
  }

  public String getMissingProvide() {
    return missingProvide;
  }
}
