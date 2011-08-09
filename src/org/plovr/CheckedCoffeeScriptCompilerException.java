package org.plovr;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;

/**
 * {@link PlovrCoffeeScriptCompilerException} is a CoffeeScript exception that
 * can generate a {@link JSError} that can be displayed for the user.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class CheckedCoffeeScriptCompilerException extends CompilationException {

  private static final long serialVersionUID = 1L;

  private final PlovrCoffeeScriptCompilerException coffeeScriptException;

  private static final DiagnosticType COFFEE_SCRIPT_SYNTAX_EXCEPTION =
      DiagnosticType.error("COFFEE_SCRIPT_SYNTAX_EXCEPTION", "{0}");

  public CheckedCoffeeScriptCompilerException(
      PlovrCoffeeScriptCompilerException cause) {
    super(cause);
    Preconditions.checkNotNull(cause);
    this.coffeeScriptException = cause;
  }

  @Override
  public CompilationError createCompilationError() {
    int lineno = coffeeScriptException.getLineNumber();
    int charno = -1;
    JSError jsError = JSError.make(
        coffeeScriptException.getInput().getName(),
        lineno,
        charno,
        CheckLevel.ERROR,
        COFFEE_SCRIPT_SYNTAX_EXCEPTION,
        getMessage());
    return new CompilationError(jsError);
  }
}
