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
import com.google.template.soy.types.SoyType;
import com.google.template.soy.types.primitive.BoolType;

/**
 * Node representing a boolean value.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
public final class BooleanNode extends AbstractPrimitiveNode {


  /** The boolean value. */
  private final boolean value;


  /**
   * @param value The boolean value.
   * @param sourceLocation The node's source location.
   */
  public BooleanNode(boolean value, SourceLocation sourceLocation) {
    super(sourceLocation);
    this.value = value;
  }


  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  private BooleanNode(BooleanNode orig, CopyState copyState) {
    super(orig, copyState);
    this.value = orig.value;
  }


  @Override public Kind getKind() {
    return Kind.BOOLEAN_NODE;
  }


  @Override public SoyType getType() {
    return BoolType.getInstance();
  }


  /** Returns the boolean value. */
  public boolean getValue() {
    return value;
  }


  @Override public String toSourceString() {
    return value ? "true" : "false";
  }


  @Override public BooleanNode copy(CopyState copyState) {
    return new BooleanNode(this, copyState);
  }

}
