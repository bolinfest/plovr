/*
 * Copyright 2013 Google Inc.
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

package com.google.template.soy.soytree;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.base.internal.IdGenerator;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.exprparse.SoyParsingContext;
import com.google.template.soy.exprtree.VarDefn;
import com.google.template.soy.exprtree.VarRefNode;
import com.google.template.soy.soytree.SoyNode.SplitLevelTopNode;
import com.google.template.soy.soytree.SoyNode.StandaloneNode;
import com.google.template.soy.soytree.SoyNode.StatementNode;

import javax.annotation.Nullable;

/**
 * Represents one message or a pair of message and fallback message.
 *
 * <p>Only one {@code fallbackmsg} is allowed by the parser.
 * {@link com.google.template.soy.soyparse.TemplateParserTest.java#testRecognizeCommands}
 * TODO(user): fix the grammar.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 * <p> All children are {@code MsgNode}s. And conversely, all {@code MsgNode}s must be children of
 * {@code MsgFallbackGroupNode}s through parsing and middleend passes (backends may have their own
 * special structure for messages).
 *
 */
public final class MsgFallbackGroupNode extends AbstractParentSoyNode<MsgNode>
    implements StandaloneNode, SplitLevelTopNode<MsgNode>, StatementNode {

  /**
   * Escaping directives names (including the vertical bar) to apply to the return value. With
   * strict autoescape, the result of each call site is escaped, which is potentially a no-op if
   * the template's return value is the correct SanitizedContent object.
   */
  private ImmutableList<String> escapingDirectiveNames = ImmutableList.of();
  @Nullable private HtmlContext htmlContext;

  /**
   * @param id The id for this node.
   * @param sourceLocation The node's source location.
   */
  public MsgFallbackGroupNode(int id, SourceLocation sourceLocation) {
    super(id, sourceLocation);
  }


  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  private MsgFallbackGroupNode(MsgFallbackGroupNode orig, CopyState copyState) {
    super(orig, copyState);
    this.escapingDirectiveNames = orig.escapingDirectiveNames;
    this.htmlContext = orig.htmlContext;
  }

  /**
   * Gets the HTML context (typically tag, attribute value, HTML PCDATA, or plain text) which
   * this node appears in. This affects how the node is escaped (for traditional backends)
   * or how it's passed to incremental DOM APIs.
   */
  public HtmlContext getHtmlContext() {
    return Preconditions.checkNotNull(htmlContext,
        "Cannot access HtmlContext before HtmlTransformVisitor");
  }

  public void setHtmlContext(HtmlContext value) {
    this.htmlContext = value;
  }

  /** Creates a print node that corresponds to this node, for tree rewriting. */
  public PrintNode makePrintNode(IdGenerator nodeIdGen, VarDefn var) {
    PrintNode printNode = new PrintNode.Builder(nodeIdGen.genId(),
        true /* implicit */, getSourceLocation())
        .exprUnion(new ExprUnion(
            new VarRefNode(var.name(), getSourceLocation(), false /* not ij */, var)))
        .build(SoyParsingContext.exploding());
    printNode.setHtmlContext(htmlContext);

    for (String escapingDirective : getEscapingDirectiveNames()) {
      printNode.addChild(new PrintDirectiveNode.Builder(nodeIdGen.genId(),
              escapingDirective, "" /* argsText */, getSourceLocation())
          .build(SoyParsingContext.exploding()));
    }
    return printNode;
  }


  @Override public Kind getKind() {
    return Kind.MSG_FALLBACK_GROUP_NODE;
  }


  @Override public String toSourceString() {
    StringBuilder sb = new StringBuilder();
    // Note: The first MsgNode takes care of generating the 'msg' tag.
    appendSourceStringForChildren(sb);
    sb.append("{/msg}");
    return sb.toString();
  }


  @Override public BlockNode getParent() {
    return (BlockNode) super.getParent();
  }

  public boolean hasFallbackMsg() {
    return numChildren() > 1;
  }

  public MsgNode getMsg() {
    return getChild(0);
  }

  public MsgNode getFallbackMsg() {
    checkState(hasFallbackMsg(), "This node doesn't have a {fallbackmsg}");
    return getChild(1);
  }

  @Override public MsgFallbackGroupNode copy(CopyState copyState) {
    return new MsgFallbackGroupNode(this, copyState);
  }

  /**
   * Sets the inferred escaping directives from the contextual engine.
   */
  public void setEscapingDirectiveNames(ImmutableList<String> escapingDirectiveNames) {
    this.escapingDirectiveNames = escapingDirectiveNames;
  }

  /**
   * Returns the escaping directives, applied from left to right.
   *
   * <p>It is an error to call this before the contextual rewriter has been run.
   */
  public ImmutableList<String> getEscapingDirectiveNames() {
    return escapingDirectiveNames;
  }
}
