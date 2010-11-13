package org.plovr;

import java.util.Collection;

import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;

public final class CircularDependencyException extends CompilationException {

  private static final long serialVersionUID = 1L;

  private static final DiagnosticType CIRCULAR_DEPDENCIES =
    DiagnosticType.error("PLOVR_CIRCULAR_DEPENDENCIES",
      "Circular dependency in the chain {0} -> {1}");

  private final JsInput input;

  private final Collection<JsInput> circularDependency;

  // TODO(bolinfest): Get the line number and character offset where the
  // error occurred so it is possible to link to it.

  /**
   * @param input the file that contains the goog.require() call.
   * @param circularDependency the require chain, in order, that led to the file
   */
  CircularDependencyException(JsInput input,
      Collection<JsInput> circularDependency) {
    super(String.format("Require loop: %s -> %s", circularDependency, input));
    this.input = input;
    this.circularDependency = circularDependency;
  }

  @Override
  public CompilationError createCompilationError() {
    final int lineno = -1;
    final int charno = -1;
    JSError jsError = JSError.make(getInput().getName(), lineno, charno,
        CheckLevel.ERROR, CIRCULAR_DEPDENCIES,
        getCircularDependency().toString(), getInput().getName());
    return new CompilationError(jsError);
  }

  public JsInput getInput() {
    return input;
  }

  /**
   * @return An in-order collection of the dependency path that led to
   *     the loop.
   */
  public Collection<JsInput> getCircularDependency() {
    return circularDependency;
  }
}
