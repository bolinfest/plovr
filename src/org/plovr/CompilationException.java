package org.plovr;

/**
 * Base class for compilation errors. Implementations need to provide
 * a way to convert into a {@link CompilationError} so that the error
 * can be communicated to the user.
 */
public abstract class CompilationException extends Exception {

  private static final long serialVersionUID = 1L;

  public CompilationException(String message) {
    super(message);
  }

  public CompilationException(Throwable t) {
    super(t);
  }

  public abstract CompilationError createCompilationError();
}
