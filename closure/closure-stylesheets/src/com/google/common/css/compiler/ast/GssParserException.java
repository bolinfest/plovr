/*
 * Copyright 2009 Google Inc.
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

package com.google.common.css.compiler.ast;

import com.google.common.css.SourceCodeLocation;

public class GssParserException extends Exception {
  private static final long serialVersionUID = 1L;

  private GssError gssError;

  public GssParserException(SourceCodeLocation location, Throwable cause) {
    super("Parse error", cause);
    this.gssError = new GssError("Parse error", location);
  }

  public GssParserException(SourceCodeLocation location) {
    this(location, null);
  }

  public GssError getGssError() {
    return gssError;
  }

  @Override
  public String getMessage() {
    return gssError.format();
  }
}
