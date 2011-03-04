package com.google.javascript.jscomp;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.NodeTraversal.AbstractPostOrderCallback;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.logging.Logger;

/**
 * Adds scope to all global assignments, with a fixed scope variable:
 *
 * <pre>var a, b = c = 5;</pre>
 *
 * becomes:
 *
 * <pre>
 * $.a = undefined, $.b = $.c = 5;
 * <pre>
 *
 * This allows one to put all of the code inside a with ($) { } block
 * and have all the variables actually live in the $ scope.
 *
 */
class AddScopeToGlobals implements CompilerPass {
  static final Logger logger =
      Logger.getLogger(AddScopeToGlobals.class.getName());

  private final AbstractCompiler compiler;
  private final String scope;

  public AddScopeToGlobals(AbstractCompiler compiler, String scope) {
    this.compiler = compiler;
    this.scope = scope;
  }

  @Override
  public void process(Node externs, Node root) {
    NodeTraversal.traverse(compiler, root, new RemoveVar());
  }

  private class RemoveVar extends AbstractPostOrderCallback {
    @Override
    public void visit(NodeTraversal t, Node n, Node parent) {
      if (n.getType() != Token.VAR) {
        return;
      }

      if (parent.getType() != Token.SCRIPT) {
        return;
      }

      // Change it into an EXPR_RESULT
      // BEFORE: var a = ...
      //   var
      //     name a
      //       ...
      //     name b
      //       ...
      // AFTER: $.a = ..., $.b = ...
      //   exprstmt
      //     comma
      //       assign
      //         getprop
      //           name $
      //           string a
      //         ...
      //       assign
      //         getprop
      //           name $
      //           string b
      //         ...

      Node expr = new Node(Token.EXPR_RESULT);
      Node top = null;
      for (Node name : n.children()) {
        Preconditions.checkState(name.getType() == Token.NAME);

        // Set up the GETPROP $.a
        Node prop = new Node(Token.GETPROP);
        prop.addChildToBack(
          Node.newString(Token.NAME, scope).copyInformationFrom(name));
        prop.addChildToBack(
          Node.newString(name.getString()).copyInformationFrom(name));
        prop.copyInformationFrom(name);

        // Set up the ASSIGN (GETPROP ...) = (...)
        Node assign = new Node(Token.ASSIGN);
        assign.addChildToBack(prop);

        // This is the value of the var assignment above.
        Node value = name.removeChildren();
        if (value != null) {
          assign.addChildToBack(value);
          // Unfortunately we have to watch out -- while things like
          //   var x = x || {};
          // are perfectly legitimate, things like
          //   $.x = x || {};
          // are not since we'll get a reference error if x isn't
          // already defined. So in this case only, try to replace x
          // with $.x
          updateNameWithProp(value, assign, name.getString());
        } else {
          // TODO: Can we do something clever here?
          assign.addChildToBack(NodeUtil.newUndefinedNode(name));
        }
        assign.copyInformationFrom(name);

        // There might have been multiple assignments in the var, they
        // need to be split up by a COMMA node.
        if (top == null) {
          top = assign;
        } else {
          Node comma = new Node(Token.COMMA);
          comma.addChildToBack(top);
          comma.addChildToBack(assign);
          comma.copyInformationFrom(top);
          top = comma;
        }
      }
      expr.addChildToBack(top);
      expr.copyInformationFrom(n);
      parent.replaceChild(n, expr);

      compiler.reportCodeChange();
    }

    /**
     * Updates a statement's references to a particular variable with
     * lookups in the scope being prefixed to all globals. Also update
     * variables that are on the left side of an assign, since they
     * need to behave the same way as though they were being
     * instantiated from scratch.
     */
    private void updateNameWithProp(Node n, Node parent, String varname) {
      if (n.getType() == Token.NAME && (
            n.getString().equals(varname) ||
            parent.getType() == Token.ASSIGN && n == parent.getFirstChild())
            ) {
        // BEFORE
        //   name a
        // AFTER
        //   getprop
        //     name $
        //     string a
        Node prop = new Node(Token.GETPROP);
        prop.addChildToBack(
          Node.newString(Token.NAME, scope).copyInformationFrom(n));
        prop.addChildToBack(Node.newString(n.getString()).copyInformationFrom(n));
        prop.copyInformationFrom(n);
        parent.replaceChild(n, prop);
      } else if (n.getType() == Token.FUNCTION) {
        // Functions don't need to be updated, as their execution will
        // be deferred. (Caveat: This isn't necessarily true, the
        // function might be executed immediately. But this really
        // shouldn't happen at global scope.)
      } else {
        for (Node c : n.children()) {
          updateNameWithProp(c, n, varname);
        }
      }
    }
  }
}
