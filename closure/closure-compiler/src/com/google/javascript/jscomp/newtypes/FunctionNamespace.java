/*
 * Copyright 2016 The Closure Compiler Authors.
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

package com.google.javascript.jscomp.newtypes;

import com.google.common.base.Preconditions;
import com.google.javascript.rhino.Node;

/**
 *
 * @author dimvar@google.com (Dimitris Vardoulakis)
 */
public final class FunctionNamespace extends Namespace {
  private DeclaredTypeRegistry scope;

  public FunctionNamespace(
      JSTypes commonTypes, String name, DeclaredTypeRegistry scope, Node defSite) {
    super(commonTypes, name, defSite);
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(scope);
    this.scope = scope;
  }

  @Override
  protected JSType computeJSType() {
    Preconditions.checkState(this.namespaceType == null);
    return JSType.fromObjectType(ObjectType.makeObjectType(
        this.commonTypes,
        this.commonTypes.getFunctionType(),
        null,
        this.scope.getDeclaredFunctionType().toFunctionType(),
        this,
        false,
        ObjectKind.UNRESTRICTED));
  }

  public DeclaredTypeRegistry getScope() {
    return this.scope;
  }
}
