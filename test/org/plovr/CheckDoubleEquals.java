package org.plovr;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.jscomp.NodeTraversal.AbstractPostOrderCallback;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

/**
 * Checks for the presence of the == and != operators, as they are frowned upon
 * according to Appendix B of JavaScript: The Good Parts.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class CheckDoubleEquals implements CompilerPass, DiagnosticGroupRegistrar {

  // Both of these DiagnosticTypes are disabled by default to demonstrate how
  // they can be enabled via the command line.

  /** Error to display when == is used. */
  private static final DiagnosticType NO_EQ_OPERATOR =
      DiagnosticType.disabled("JSC_NO_EQ_OPERATOR",
        "Use the === operator instead of the == operator.");

  /** Error to display when != is used. */
  private static final DiagnosticType NO_NE_OPERATOR =
      DiagnosticType.disabled("JSC_NO_NE_OPERATOR",
          "Use the !== operator instead of the != operator.");

  public static final String DIAGNOSTIC_GROUP_NAME = "checkDoubleEquals";

  private static final DiagnosticGroup DIAGNOSTIC_GROUP = new DiagnosticGroup(
      NO_EQ_OPERATOR, NO_NE_OPERATOR);

  private final AbstractCompiler compiler;

  public CheckDoubleEquals(AbstractCompiler compiler) {
    this.compiler = compiler;
  }

  @Override
  public void process(Node externs, Node root) {
    NodeTraversal.traverse(compiler, root, new FindDoubleEquals());
  }

  @Override
  public void registerDiagnosticGroupsWith(PlovrDiagnosticGroups groups) {
    groups.registerGroup(DIAGNOSTIC_GROUP_NAME, DIAGNOSTIC_GROUP);
  }

  /**
   * Traverses the AST looking for uses of == or !=. Upon finding one, it will
   * report an error unless {@code @suppress {double-equals}} is present.
   */
  private class FindDoubleEquals extends AbstractPostOrderCallback {

    @Override
    public void visit(NodeTraversal t, Node n, Node parent) {
      final int type = n.getType();
      if (type == Token.EQ || type == Token.NE) {
        JSDocInfo info = n.getJSDocInfo();
        if (info != null && info.getSuppressions().contains("double-equals")) {
          return;
        }

        DiagnosticType diagnosticType =
            (type == Token.EQ) ? NO_EQ_OPERATOR : NO_NE_OPERATOR;
        JSError error = JSError.make(t.getSourceName(), n, diagnosticType);
        compiler.report(error);
      }
    }
  }
}
