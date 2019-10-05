package org.plovr;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.DiagnosticGroups;
import java.io.PrintStream;

/**
 * {@link PlovrClosureCompiler} subclasses {@link Compiler} so that its
 * {@link #getDiagnosticGroups()} method can be overridden to return a
 * {@link PlovrDiagnosticGroups}, which is a {@link DiagnosticGroups} that can
 * be modified from outside the com.google.javascript.jscomp package.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class PlovrClosureCompiler extends Compiler {

  private PlovrDiagnosticGroups diagnosticGroups = new PlovrDiagnosticGroups();

  public PlovrClosureCompiler(PrintStream errorStream) { super(errorStream); }

  @Override
  public PlovrDiagnosticGroups getDiagnosticGroups() {
    return diagnosticGroups;
  }
}
