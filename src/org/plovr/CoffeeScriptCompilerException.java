package org.plovr;

/**
 * An exception thrown by the CoffeeScript compiler.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class CoffeeScriptCompilerException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * @param message should be of the form:
   *     In INPUT, Parse error on line LINE_NUMBER: PARSE_ERROR.
   */
  public CoffeeScriptCompilerException(String message) {
    super(message);
  }
}
