/*
 * Copyright 2008 Google Inc.
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

package com.google.template.soy.exprtree;

import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.basetree.MixinParentNode;
import com.google.template.soy.exprtree.ExprNode.ParentExprNode;
import com.google.template.soy.types.SoyType;

import java.util.List;

/**
 * Abstract implementation of a ParentExprNode.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
public abstract class AbstractParentExprNode extends AbstractExprNode implements ParentExprNode {


  /** The mixin object that implements the ParentNode functionality. */
  private final MixinParentNode<ExprNode> parentMixin;


  /**
   * Data type of this expression. If null, it indicates that the type has not yet been
   * filled in (and should be).
   */
  private SoyType type;


  protected AbstractParentExprNode(SourceLocation sourceLocation) {
    this(null /* type */, sourceLocation);
  }


  protected AbstractParentExprNode(SoyType type, SourceLocation sourceLocation) {
    super(sourceLocation);
    parentMixin = new MixinParentNode<>(this);
    this.type = type;
  }


  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  protected AbstractParentExprNode(AbstractParentExprNode orig, CopyState copyState) {
    super(orig, copyState);
    this.parentMixin = new MixinParentNode<>(orig.parentMixin, this, copyState);
    this.type = orig.type;
  }


  @Override public SoyType getType() {
    return type;
  }


  public void setType(SoyType type) {
    this.type = type;
  }


  @Override public int numChildren() {
    return parentMixin.numChildren();
  }

  @Override public ExprNode getChild(int index) {
    return parentMixin.getChild(index);
  }

  @Override public int getChildIndex(ExprNode child) {
    return parentMixin.getChildIndex(child);
  }

  @Override public List<ExprNode> getChildren() {
    return parentMixin.getChildren();
  }

  @Override public void addChild(ExprNode child) {
    parentMixin.addChild(child);
  }

  @Override public void addChild(int index, ExprNode child) {
    parentMixin.addChild(index, child);
  }

  @Override public void removeChild(int index) {
    parentMixin.removeChild(index);
  }

  @Override public void removeChild(ExprNode child) {
    parentMixin.removeChild(child);
  }

  @Override public void replaceChild(int index, ExprNode newChild) {
    parentMixin.replaceChild(index, newChild);
  }

  @Override public void replaceChild(ExprNode currChild, ExprNode newChild) {
    parentMixin.replaceChild(currChild, newChild);
  }

  @Override public void clearChildren() {
    parentMixin.clearChildren();
  }

  @Override public void addChildren(List<? extends ExprNode> children) {
    parentMixin.addChildren(children);
  }

  @Override public void addChildren(int index, List<? extends ExprNode> children) {
    parentMixin.addChildren(index, children);
  }

  @Override public void appendSourceStringForChildren(StringBuilder sb) {
    parentMixin.appendSourceStringForChildren(sb);
  }

}
