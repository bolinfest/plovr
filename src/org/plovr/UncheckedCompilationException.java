package org.plovr;

abstract class UncheckedCompilationException extends RuntimeException {
  UncheckedCompilationException(String message) { super(message); }
  UncheckedCompilationException(Exception cause) { super(cause); }

  abstract CompilationException toCheckedException();

  public CompilationError createCompilationError() {
    return toCheckedException().createCompilationError();
  }
}
