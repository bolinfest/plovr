package org.plovr;

import java.util.List;

abstract class UncheckedCompilationException extends RuntimeException {
  UncheckedCompilationException(String message) { super(message); }
  UncheckedCompilationException(Exception cause) { super(cause); }

  abstract CompilationException toCheckedException();

  public List<CompilationError> createCompilationErrors() {
    return toCheckedException().createCompilationErrors();
  }
}
