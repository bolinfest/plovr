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

/**
 * Node representing a variable.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 * <p>
 * Important: This type of node never appears in expression parse trees. It is only created if you
 * explicitly parse an input as a variable using ExpressionParser.parseVariable().
 *
 */
public final class VarNode extends AbstractExprNode {

  public static final VarNode ERROR = new VarNode("error", SourceLocation.UNKNOWN);


  /** The variable name (without the dollar sign). */
  private final String name;


  /**
   * @param name The variable name (without the dollar sign).
   * @param sourceLocation The node's source location.
   */
  public VarNode(String name, SourceLocation sourceLocation) {
    super(sourceLocation);
    this.name = name;
  }


  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  private VarNode(VarNode orig, CopyState copyState) {
    super(orig, copyState);
    this.name = orig.name;
  }


  @Override public Kind getKind() {
    return Kind.VAR_NODE;
  }


  @Override public SoyType getType() {
    throw new UnsupportedOperationException();
  }


  /** Returns the variable name (without the dollar sign). */
  public String getName() {
    return name;
  }


  @Override public String toSourceString() {
    return "$" + name;
  }


  @Override public VarNode copy(CopyState copyState) {
    return new VarNode(this, copyState);
  }

}
