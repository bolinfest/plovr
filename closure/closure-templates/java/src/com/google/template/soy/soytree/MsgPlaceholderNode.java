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

package com.google.template.soy.soytree;

import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.soytree.SoyNode.MsgSubstUnitNode;

/**
 * A node that is the direct child of a MsgBlockNode and will turn into a placeholder.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
public final class MsgPlaceholderNode extends AbstractBlockNode implements MsgSubstUnitNode {

  /** The base placeholder name (what the translator sees). */
  private final String basePhName;

  /** The initial child's SoyNode kind. */
  private final SoyNode.Kind initialNodeKind;

  /** Key object for determining whether this node and another node should be represented by the
   *  same placeholder. */
  private final Object samenessKey;


  /**
   * @param id The id for this node.
   * @param initialNode Will be set as the only child of this node being created. The child is
   *     the node that represents the content of this placeholder. It is called the initial node
   *     because it may be modified/replaced later by compiler passes.
   */
  public MsgPlaceholderNode(int id, MsgPlaceholderInitialNode initialNode) {
    super(id, initialNode.getSourceLocation());
    this.basePhName = initialNode.genBasePhName();
    this.initialNodeKind = initialNode.getKind();
    this.samenessKey = initialNode.genSamenessKey();
    this.addChild(initialNode);
  }


  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  private MsgPlaceholderNode(MsgPlaceholderNode orig, CopyState copyState) {
    super(orig, copyState);
    this.basePhName = orig.basePhName;
    this.initialNodeKind = orig.initialNodeKind;
    this.samenessKey = orig.samenessKey;
  }


  @Override public Kind getKind() {
    return Kind.MSG_PLACEHOLDER_NODE;
  }


  /** Returns the base placeholder name (what the translator sees). */
  @Override public String getBaseVarName() {
    return basePhName;
  }


  /**
   * Returns whether this node and the given other node are the same, such that they should be
   * represented by the same placeholder.
   */
  @Override public boolean shouldUseSameVarNameAs(MsgSubstUnitNode other) {
    return (other instanceof  MsgPlaceholderNode) &&
        this.initialNodeKind == ((MsgPlaceholderNode) other).initialNodeKind &&
        this.samenessKey.equals(((MsgPlaceholderNode) other).samenessKey);
  }


  @Override public String toSourceString() {
    return getChild(0).toSourceString();
  }


  @Override public MsgBlockNode getParent() {
    return (MsgBlockNode) super.getParent();
  }


  @Override public MsgPlaceholderNode copy(CopyState copyState) {
    return new MsgPlaceholderNode(this, copyState);
  }

}
