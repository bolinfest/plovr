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

import com.google.common.base.Preconditions;
import com.google.template.soy.basetree.CopyState;

import java.util.Objects;

/**
 * Reference to a named field.
 *
 */
public final class FieldAccessNode extends DataAccessNode {

  private final String fieldName;

  /**
   * @param base The base expression, that is a reference to the object
   *     containing the named field.
   * @param fieldName The name of the field.
   * @param isNullSafe If true, checks during evaluation whether the base expression is null
   *     and returns null instead of causing an invalid dereference.
   */
  public FieldAccessNode(ExprNode base, String fieldName, boolean isNullSafe) {
    super(base, base.getSourceLocation(), isNullSafe);
    Preconditions.checkArgument(fieldName != null);
    this.fieldName = fieldName;
  }

  private FieldAccessNode(FieldAccessNode orig, CopyState copyState) {
    super(orig, copyState);
    this.fieldName = orig.fieldName;
  }

  @Override public Kind getKind() {
    return Kind.FIELD_ACCESS_NODE;
  }


  /** Returns the field name. */
  public String getFieldName() {
    return fieldName;
  }


  /**
   * Returns the source string for the part of the expression that accesses
   * the item - in other words, not including the base expression. This is
   * intended for use in reporting errors.
   */
  @Override public String getSourceStringSuffix() {
    return (isNullSafe ? "?." : ".") + fieldName;
  }


  @Override public FieldAccessNode copy(CopyState copyState) {
    return new FieldAccessNode(this, copyState);
  }


  @Override public boolean equals(Object other) {
    if (other == null || other.getClass() != this.getClass()) { return false; }
    FieldAccessNode otherFieldRef = (FieldAccessNode) other;
    return getChild(0).equals(otherFieldRef.getChild(0)) &&
        fieldName.equals(otherFieldRef.fieldName) &&
        isNullSafe == otherFieldRef.isNullSafe;
  }


  @Override public int hashCode() {
    return Objects.hash(this.getClass(), getChild(0), fieldName, isNullSafe);
  }
}
