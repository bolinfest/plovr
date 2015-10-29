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

package com.google.template.soy.types.primitive;

import com.google.template.soy.data.SoyValue;
import com.google.template.soy.data.restricted.SoyString;
import com.google.template.soy.types.SoyType;

/**
 * Soy string type.
 *
 */
public final class StringType extends PrimitiveType {


  private static final StringType INSTANCE = new StringType();


  // Not constructible - use getInstance().
  private StringType() {}


  @Override public Kind getKind() {
    return Kind.STRING;
  }


  @Override public String toString() {
    return "string";
  }


  /**
   * Return the single instance of this type.
   */
  public static StringType getInstance() {
    return INSTANCE;
  }


  @Override public boolean isAssignableFrom(SoyType srcType) {
    return srcType.getKind() == Kind.STRING || srcType instanceof SanitizedType;
  }


  @Override public boolean isInstance(SoyValue value) {
    return value instanceof SoyString;
  }

  @Override public Class<? extends SoyValue> javaType() {
    return SoyString.class;
  }
}
