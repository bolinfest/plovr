package org.plovr;

import com.google.common.collect.ImmutableList;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

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

  public abstract List<CompilationError> createCompilationErrors();

  public final void print(PrintStream out) {
    for (CompilationError error : createCompilationErrors()) {
      out.println(error.getMessage());
    }
  }

  public abstract static class Single extends CompilationException {
    public Single(String message) {
      super(message);
    }

    public Single(Throwable t) {
      super(t);
    }

    public List<CompilationError> createCompilationErrors() {
      return ImmutableList.of(createCompilationError());
    }

    public abstract CompilationError createCompilationError();
  }

  public static class Multi extends CompilationException {
    private final List<CompilationException> exceptions;

    public Multi(List<CompilationException> exceptions) {
      super(exceptions.get(0));
      this.exceptions = exceptions;
    }

    @Override
    public List<CompilationError> createCompilationErrors() {
      ArrayList<CompilationError> result = new ArrayList<>();
      for (CompilationException e : exceptions) {
        result.addAll(e.createCompilationErrors());
      }
      return result;
    }
  }
}
