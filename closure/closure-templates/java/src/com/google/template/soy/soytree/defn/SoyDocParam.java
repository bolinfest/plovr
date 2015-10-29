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

import com.google.template.soy.types.SoyType;
import com.google.template.soy.types.primitive.UnknownType;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A parameter declared in the template SoyDoc.
 *
 * <p>Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
@Immutable
public final class SoyDocParam extends TemplateParam {

  public SoyDocParam(String name, boolean isRequired, @Nullable String desc) {
    super(name, UnknownType.getInstance(), isRequired, false, desc);
  }

  private SoyDocParam(SoyDocParam soyDocParam) {
    super(soyDocParam);
  }

  @Override public DeclLoc declLoc() {
    return DeclLoc.SOY_DOC;
  }

  @Override public SoyType type() {
    return UnknownType.getInstance();
  }

  @Override public SoyDocParam copyEssential() {
    // Note: 'desc' is nonessential.
    SoyDocParam soyDocParam = new SoyDocParam(name(), isRequired(), null);
    soyDocParam.setLocalVariableIndex(localVariableIndex());
    return soyDocParam;
  }

  @SuppressWarnings("SimplifiableIfStatement")  // for IntelliJ
  @Override public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || this.getClass() != o.getClass()) { return false; }
    return super.abstractEquals(o);
  }

  @Override public int hashCode() {
    return super.abstractHashCode();
  }

  public SoyDocParam clone() {
    return new SoyDocParam(this);
  }
}
