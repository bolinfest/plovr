package com.google.javascript.jscomp;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.NodeTraversal.AbstractPostOrderCallback;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.logging.Logger;

/**
 * Converts named function declarations into anonymous function expressions,
 * i.e. the following:
 *
 * <pre>function f()</pre>
 *
 * becomes:
 *
 * <pre>
 * var f = function()
 * <pre>
 *
 * Note: This breaks function hoisting, so this assumes that either
 * (1) function hoisting isn't used, or (2) a previous pass has
 * already rearranged the function order so that named functions occur
 * before their uses.
 */
class AnonymizeNamedFunctions implements CompilerPass {
  static final Logger logger =
      Logger.getLogger(AnonymizeNamedFunctions.class.getName());

  private final AbstractCompiler compiler;

  public AnonymizeNamedFunctions(AbstractCompiler compiler) {
    Preconditions.checkArgument(compiler.getLifeCycleStage().isNormalized());
    this.compiler = compiler;
  }

  @Override
  public void process(Node externs, Node root) {
    NodeTraversal.traverse(compiler, root, new Callback());
  }

  private class Callback extends AbstractPostOrderCallback {
    @Override
    public void visit(NodeTraversal t, Node n, Node parent) {
      if (n.getType() != Token.FUNCTION) {
        return;
      }

      if (parent.getType() != Token.SCRIPT) {
        return;
      }

      // Change it into a VAR
      // BEFORE: function f(args) {}
      //   function
      //     name
      //     ...
      // AFTER: var f = function(args) {}
      //   var name
      //     function
      //       ""
      //       ...

      Preconditions.checkState(n.hasMoreThanOneChild());

      Node fnName = n.getFirstChild();
      if (fnName != null &&
          fnName.getType() == Token.NAME) {
        if (fnName.getString().isEmpty()) {
          return;
        }

        Node varNameNode = fnName.cloneNode();
        Node var = new Node(
            Token.VAR, varNameNode, n.getLineno(), n.getCharno());
        var.copyInformationFrom(n);

        // Clear out the function name
        fnName.setString("");

        // Swap the function and the var decl
        parent.replaceChild(n, var);

        // Move the nameless function under the var
        varNameNode.addChildToFront(n);

        compiler.reportCodeChange();
      }
    }
  }
}
