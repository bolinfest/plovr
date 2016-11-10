/*
 * Copyright 2015 Google Inc.
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

package com.google.template.soy.error;

import com.google.template.soy.base.SourceLocation;

/**
 * {@link ErrorReporter} implementation that throws an {@link AssertionError} whenever an error
 * is reported to it. This should only be used when no errors are expected.  This is seldom
 * desirable in production code, but often desirable in tests, which should fail in the presence
 * of any errors that are not specifically checked for.
 *
 * <p>To write a test that does not have this exploding behavior (for example, a test that needs
 * to check the full list of errors encountered during compilation), pass a non-exploding
 * ErrorReporter instance to
 * {@link com.google.template.soy.SoyFileSetParserBuilder#errorReporter}.
 *
 * @author brndn@google.com (Brendan Linn)
 */
public final class ExplodingErrorReporter extends AbstractErrorReporter {

  private static final ErrorReporter INSTANCE = new ExplodingErrorReporter();
  
  public static ErrorReporter get() {
    return INSTANCE;
  }

  private ExplodingErrorReporter() {}

  @Override
  public void report(SourceLocation sourceLocation, SoyErrorKind error, Object... args) {
    throw new AssertionError(
        String.format("Unexpected SoyError: %s at %s", error.format(args), sourceLocation));
  }

  @Override
  protected int getCurrentNumberOfErrors() {
    return 0;
  }
}
