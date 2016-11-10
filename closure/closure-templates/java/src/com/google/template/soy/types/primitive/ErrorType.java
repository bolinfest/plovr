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
import com.google.template.soy.types.SoyType;

/**
 * A placeholder for errors during parsing.
 */
public final class ErrorType implements SoyType {

  private static final ErrorType INSTANCE = new ErrorType();

  private ErrorType() {}

  public static ErrorType getInstance() {
    return INSTANCE;
  }

  @Override public Kind getKind() {
    return Kind.ERROR;
  }


  @Override public boolean isAssignableFrom(SoyType srcType) {
    return false;
  }

  @Override public boolean isInstance(SoyValue value) {
    // TODO(lukes): have this throw an exception? while it is true that nothing is equal to the
    // error type... this comparison should probably never happen in the first place.
    return false;
  }
}
