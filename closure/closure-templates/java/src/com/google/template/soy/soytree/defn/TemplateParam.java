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

import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An explicitly declared template parameter.
 *
 * <p>Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
@Immutable
public abstract class TemplateParam extends AbstractVarDefn {
  /**
   * Enum for the location of the declaration.
   */
  public static enum DeclLoc {
    // Declaration in template SoyDoc, e.g.
    //     @param foo Blah blah blah.
    SOY_DOC,
    // Declaration in template header, e.g.
    //     {@param foo: list<int>}  /** Blah blah blah. */
    HEADER,
  }

  /** Whether the param is required. */
  private final boolean isRequired;

  /** Whether the param is an injected param. */
  private final boolean isInjected;

  /** The parameter description. */
  private final String desc;

  public TemplateParam(
      String name,
      SoyType type,
      boolean isRequired,
      boolean isInjected,
      @Nullable String desc) {
    super(name, type);
    this.isRequired = isRequired;
    this.isInjected = isInjected;
    this.desc = desc;
  }

  TemplateParam(TemplateParam param) {
    super(param);
    this.isRequired = param.isRequired;
    this.isInjected = param.isInjected;
    this.desc = param.desc;
  }

  @Override public Kind kind() {
    return Kind.PARAM;
  }

  /** Returns the location of the parameter declaration. */
  public abstract DeclLoc declLoc();

  /** Returns whether the param is required. */
  public boolean isRequired() {
    return isRequired;
  }

  /** Returns whether the param is required. */
  public boolean isInjected() {
    return isInjected;
  }

  public String desc() {
    return desc;
  }

  public abstract TemplateParam copyEssential();

  // Subclasses must implement equals().
  @Override public abstract boolean equals(Object o);

  // Subclasses must implement hashCode().
  @Override public abstract int hashCode();

  protected boolean abstractEquals(Object o) {
    // Note: 'type' and 'desc' are nonessential with respect to equality.
    // Note: This is valid only if you don't try and mix parameters from
    // different templates in the same set.
    if (this == o) { return true; }
    if (o == null || this.getClass() != o.getClass()) { return false; }
    AbstractVarDefn other = (AbstractVarDefn) o;
    return this.name().equals(other.name()) && this.kind() == other.kind();
  }

  protected int abstractHashCode() {
    // Note: This is valid only if you don't try and mix parameters from
    // different templates in the same set.
    return Objects.hash(this.getClass(), name(), isRequired, isInjected);
  }
}
