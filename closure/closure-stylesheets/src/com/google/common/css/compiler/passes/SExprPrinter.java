/*
 * Copyright 2012 Google Inc.
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

package com.google.common.css.compiler.passes;

import com.google.common.css.SourceCodeLocation;
import com.google.common.css.compiler.ast.CssCompositeValueNode;
import com.google.common.css.compiler.ast.CssNode;
import com.google.common.css.compiler.ast.CssNodesListNode;
import com.google.common.css.compiler.ast.CssTree;
import com.google.common.css.compiler.ast.CssValueNode;

/**
 * Build up an s-expression corresponding to the AST for debugging purposes.
 */
public class SExprPrinter extends CodePrinter {
  private boolean includeHashCodes = false;
  private boolean withLocationAnnotation = false;

  /**
   * A S-Expr printer for {@link CssNode} instances.
   */
  public SExprPrinter(CssNode subtree) {
    super(subtree);
  }

  /**
   * A S-Expr printer for {@link CssTree} instances.
   */
  public SExprPrinter(CssTree tree) {
    super(tree);
  }

  /**
   * A S-Expr printer for {@link CssNode} instances.
   * @param includeHashCodes boolean switch to include hash code for node or not.
   * @param withLocationAnnotation boolean switch to include source code location or not.
   */
  public SExprPrinter(CssNode subtree, boolean includeHashCodes, boolean withLocationAnnotation) {
    super(subtree);
    this.includeHashCodes = includeHashCodes;
    this.withLocationAnnotation = withLocationAnnotation;
  }

  /**
   * A S-Expr printer for {@link CssTree} instances.
   * @param includeHashCodes boolean switch to include hash code for node or not.
   * @param withLocationAnnotation boolean switch to include source code location or not.
   */
  public SExprPrinter(CssTree tree, boolean includeHashCodes, boolean withLocationAnnotation) {
    super(tree);
    this.includeHashCodes = includeHashCodes;
    this.withLocationAnnotation = withLocationAnnotation;
  }

  @Override
  public void enter(CssNode node) {
    if (includeHashCodes) {
      buffer.append(String.format("(%s@%d ", node.getClass().getName(), node.hashCode()));
    } else {
      buffer.append(String.format("(%s ", node.getClass().getName()));
    }

    if (withLocationAnnotation) {
      SourceCodeLocation loc = node.getSourceCodeLocation();
      if (loc == null) {
        loc = SourceCodeLocation.getUnknownLocation();
      }
      buffer.append(String.format(
        ":scl-unknown %s ", loc.isUnknown()));
    }
  }

  @Override
  public void leave(CssNode node) {
    buffer.deleteLastCharIfCharIs(' ');
    buffer.append(')');
  }

  /** Called between adjacent nodes in a media type list */
  @Override
  public boolean enterMediaTypeListDelimiter(CssNodesListNode<? extends CssNode> node) {
    super.enterMediaTypeListDelimiter(node);
    // this very special state does not represent a node
    buffer.append("(MediaTypeListDelimiter");
    return true;
  }

  /** Called between adjacent nodes in a media type list */
  @Override
  public void leaveMediaTypeListDelimiter(CssNodesListNode<? extends CssNode> node) {
    // this very special state does not represent a node
    buffer.append(')');
    super.leaveMediaTypeListDelimiter(node);
  }

  /** Called between values in a {@code CssCompositeValueNode} */
  @Override
  public boolean enterCompositeValueNodeOperator(CssCompositeValueNode parent) {
    super.enterCompositeValueNodeOperator(parent);
    // this very special state does not represent a node
    buffer.append("(CompositeValueNodeOperator ");
    buffer.append(parent.getOperator().name());
    return true;
  }

  /** Called between values in a {@code CssCompositeValueNode} */
  @Override
  public void leaveCompositeValueNodeOperator(CssCompositeValueNode parent) {
    buffer.append(')');
    super.leaveCompositeValueNodeOperator(parent);
  }

  @Override
  public boolean enterValueNode(CssValueNode n) {
    super.enterValueNode(n);
    buffer.append(n + " ");
    return true;
  }

  @Override
  public void leaveValueNode(CssValueNode n) {
    super.leaveValueNode(n);
  }

  public static String print(CssTree t) {
    SExprPrinter printer = new SExprPrinter(t);
    t.getVisitController().startVisit(printer);
    return printer.getOutputBuffer();
  }

  public static String print(boolean includeHashCodes, boolean withLocationAnnotation, CssTree t) {
    SExprPrinter printer = new SExprPrinter(t, includeHashCodes, withLocationAnnotation);
    t.getVisitController().startVisit(printer);
    return printer.getOutputBuffer();
  }

  public static String print(CssNode n) {
    SExprPrinter printer = new SExprPrinter(n);
    n.getVisitController().startVisit(printer);
    return printer.getOutputBuffer();
  }

  public static String print(boolean includeHashCodes, boolean withLocationAnnotation, CssNode n) {
    SExprPrinter printer = new SExprPrinter(n, includeHashCodes, withLocationAnnotation);
    n.getVisitController().startVisit(printer);
    return printer.getOutputBuffer();
  }
}
