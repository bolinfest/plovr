/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.passes;

import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.SoyError;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.exprtree.FunctionNode;
import com.google.template.soy.soytree.AbstractSoyNodeVisitor;
import com.google.template.soy.soytree.MsgPluralNode;
import com.google.template.soy.soytree.PrintNode;
import com.google.template.soy.soytree.SoyNode;
import com.google.template.soy.soytree.SoyNode.ParentSoyNode;

/**
 * Visitor for finding {@code print} nodes that are actually {@code remainder} nodes,
 * and replacing them with the appropriate expression.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 * <p> {@link #exec} should be called on a full parse tree. There is no return value.
 *
 */
public final class RewriteRemaindersVisitor extends AbstractSoyNodeVisitor<Void> {

  private static final SoyError REMAINDER_ARITY_MISMATCH =
      SoyError.of("''remainder'' called with {0} arguments, expected 1.");
  private static final SoyError REMAINDER_OUTSIDE_PLURAL =
      SoyError.of("Special function ''remainder'' is for use in plural messages only.");
  private static final SoyError REMAINDER_PLURAL_EXPR_MISMATCH =
      SoyError.of("Argument to ''remainder'' must be the same as the ''plural'' variable");
  private static final SoyError REMAINDER_UNNECESSARY_AT_OFFSET_0 =
      SoyError.of("''remainder'' is unnecessary since offset=0.");
  private static final SoyError REMAINDER_WITH_PHNAME =
      SoyError.of("Special function ''remainder'' cannot be used with ''phname''.");

  /** The MsgPluralNode most recently visited. */
  private MsgPluralNode currPluralNode;

  private final ErrorReporter errorReporter;

  public RewriteRemaindersVisitor(ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
  }

  // -----------------------------------------------------------------------------------------------
  // Implementations for specific nodes.


  @Override protected void visitPrintNode(PrintNode node) {

    ExprRootNode exprRootNode = node.getExprUnion().getExpr();
    if (exprRootNode == null) {
      return;
    }

    // Check for the function node with the function "remainder()".
    if (exprRootNode.getRoot() instanceof FunctionNode) {
      FunctionNode functionNode = (FunctionNode) exprRootNode.getRoot();
      if (functionNode.getFunctionName().equals("remainder")) {

        // 'remainder' outside 'plural'. Bad!
        if (currPluralNode == null) {
          errorReporter.report(functionNode.getSourceLocation(), REMAINDER_OUTSIDE_PLURAL);
          return; // to prevent NPE later in the method
        }

        // 'remainder' with no parameters or more than one parameter. Bad!
        if (functionNode.numChildren() != 1) {
          errorReporter.report(
              functionNode.getSourceLocation(),
              REMAINDER_ARITY_MISMATCH,
              functionNode.numChildren());
        }

        // 'remainder' with a different expression than the enclosing 'plural'. Bad!
        if (!functionNode.getChild(0).toSourceString().equals(
                  currPluralNode.getExpr().toSourceString())) {
          errorReporter.report(functionNode.getSourceLocation(), REMAINDER_PLURAL_EXPR_MISMATCH);
        }

        // 'remainder' with a 0 offset. Bad!
        if (currPluralNode.getOffset() == 0) {
          errorReporter.report(functionNode.getSourceLocation(), REMAINDER_UNNECESSARY_AT_OFFSET_0);
        }

        // 'remainder' with 'phname' attribute. Bad!
        if (node.getUserSuppliedPhName() != null) {
          errorReporter.report(functionNode.getSourceLocation(), REMAINDER_WITH_PHNAME);
        }

        // Now rewrite the PrintNode (reusing the old node id).
        String newExprText =
            "(" + currPluralNode.getExpr().toSourceString() + ") - " + currPluralNode.getOffset();
        PrintNode newPrintNode
            = new PrintNode.Builder(node.getId(), node.isImplicit(), SourceLocation.UNKNOWN)
                .exprText(newExprText)
                .build(errorReporter);
        newPrintNode.addChildren(node.getChildren());
        node.getParent().replaceChild(node, newPrintNode);
      }
    }
  }


  @Override protected void visitMsgPluralNode(MsgPluralNode node) {
    currPluralNode = node;
    visitChildren(node);
    currPluralNode = null;
  }


  // -----------------------------------------------------------------------------------------------
  // Fallback implementation.


  @Override protected void visitSoyNode(SoyNode node) {
    if (node instanceof ParentSoyNode<?>) {
      visitChildrenAllowingConcurrentModification((ParentSoyNode<?>) node);
    }
  }

}
