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

package com.google.template.soy.soytree.defn;

import com.google.template.soy.types.primitive.UnknownType;

/**
 * A reference to an undeclared variable, used in legacy templates.
 *
 */
public final class UndeclaredVar extends AbstractVarDefn {

  /**
   * @param name The variable name.
   */
  public UndeclaredVar(String name) {
    super(name, UnknownType.getInstance());
  }

  private UndeclaredVar(UndeclaredVar var) {
    super(var);
  }

  @Override public Kind kind() {
    return Kind.UNDECLARED;
  }

  @Override public UndeclaredVar clone() {
    return new UndeclaredVar(this);
  }

  @Override
  public boolean isInjected() {
    return false;
  }
}
