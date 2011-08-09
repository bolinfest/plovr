package org.plovr;

import com.google.common.base.Preconditions;

/**
 * {@link PlovrCoffeeScriptCompilerException} is a wrapper for a
 * {@link CoffeeScriptCompilerException} that contains information specific to
 * plovr, such as the {@link JsInput} that was responsible for the exception.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class PlovrCoffeeScriptCompilerException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final CoffeeScriptCompilerException coffeeScriptCompilerException;
  private final JsInput input;

  public PlovrCoffeeScriptCompilerException(
      CoffeeScriptCompilerException cause, JsInput input) {
    super(cause);
    Preconditions.checkNotNull(cause);
    this.coffeeScriptCompilerException = cause;
    this.input = input;
  }

  public CoffeeScriptCompilerException getCoffeeScriptCompilerException() {
    return coffeeScriptCompilerException;
  }

  public JsInput getInput() {
    return input;
  }

  public int getLineNumber() {
    return coffeeScriptCompilerException.getLineNumber();
  }
}
