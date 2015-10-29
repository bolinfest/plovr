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

package com.google.template.soy.exprtree;

import com.google.template.soy.basetree.CopyState;

import java.util.Objects;

/**
 * Represents the bracket [] operation, which can either be used to access an
 * array element by index, or a map value by key. Following the naming convention
 * of Python, both array elements and map values are here referred to as "items".
 *
 */
public final class ItemAccessNode extends DataAccessNode {

  /**
   * @param base The base expression, that is a reference to the object
   *     containing the item.
   * @param key An expression representing either an array index or a map
   *     key.
   * @param isNullSafe If true, checks during evaluation whether the base expression is null
   *     and returns null instead of causing an invalid dereference.
   */
  public ItemAccessNode(ExprNode base, ExprNode key, boolean isNullSafe) {
    // This source location is wrong
    super(base, base.getSourceLocation(), isNullSafe);
    addChild(key); // Key is child 1, Base is child 0.
  }

  private ItemAccessNode(ItemAccessNode orig, CopyState copyState) {
    super(orig, copyState);
  }

  @Override public Kind getKind() {
    return Kind.ITEM_ACCESS_NODE;
  }


  /** Returns the key expression. */
  public ExprNode getKeyExprChild() {
    return this.getChild(1);
  }


  /**
   * Returns the source string for the part of the expression that accesses
   * the item - in other words, not including the base expression. This is
   * intended for use in reporting errors.
   */
  @Override public String getSourceStringSuffix() {
    return (isNullSafe ? "?[" : "[") + getChild(1).toSourceString() + "]";
  }


  @Override public ItemAccessNode copy(CopyState copyState) {
    return new ItemAccessNode(this, copyState);
  }


  @Override public boolean equals(Object other) {
    if (other == null || other.getClass() != this.getClass()) { return false; }
    ItemAccessNode otherItemRef = (ItemAccessNode) other;
    return getChild(0).equals(otherItemRef.getChild(0))
        && getChild(1).equals(otherItemRef.getChild(1))
        && isNullSafe == otherItemRef.isNullSafe;
  }


  @Override public int hashCode() {
    return Objects.hash(this.getClass(), getChild(0), getChild(1));
  }
}
