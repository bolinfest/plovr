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

package com.google.template.soy.passes;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.SoyFileSetParserBuilder;
import com.google.template.soy.error.FormattingErrorReporter;
import com.google.template.soy.passes.AssertStrictAutoescapingVisitor;
import com.google.template.soy.soytree.SoyFileSetNode;

import junit.framework.TestCase;

/**
 * Unit tests for AssertStrictAutoescapingVisitor.
 *
 */
public final class AssertStrictAutoescapingVisitorTest extends TestCase {

  public void testStrictTemplate() {
    String soyCode =
        "{namespace foo.bar autoescape=\"strict\"}\n"
            + "{template .foo autoescape=\"strict\"}\n"
            + "{@param boo : ?}\n"
            + "  {$boo}\n"
            + "{/template}\n";
    doesntCauseStrictException(soyCode);

    soyCode =
        "{namespace foo.bar}\n"
            + "{template .foo}\n"
            + "{@param boo : ?}\n"
            + "  {$boo}\n"
            + "{/template}\n";
    doesntCauseStrictException(soyCode);
  }

  public void testNonStrictNamespace() {
    String soyCode =
        "{namespace foo.bar autoescape=\"deprecated-contextual\"}\n"
            + "{template .foo autoescape=\"strict\"}\n"
            + "{@param boo : ?}\n"
            + "  {$boo}\n"
            + "{/template}\n";
    causesStrictException(soyCode);
  }

  public void testNonStrictTemplate() {
    String soyCode =
        "{namespace foo.bar autoescape=\"strict\"}\n"
            + "{template .foo autoescape=\"deprecated-contextual\"}\n"
            + "{@param boo : ?}\n"
            + "  {$boo}\n"
            + "{/template}\n";
    causesStrictException(soyCode);
  }

  public void testNonDeclaredTemplate() {
    String soyCode =
        "{namespace foo.bar autoescape=\"deprecated-noncontextual\"}\n"
            + "{template .foo}\n"
            + "{@param boo : ?}\n"
            + "  {$boo}\n"
            + "{/template}\n";
    causesStrictException(soyCode);
  }

  /**
   * Parse soyCode and execute the AssertStrictAutoescapingVisitor check on the output.
   *
   * @param soyCode The input code.
   */
  private void doesntCauseStrictException(String soyCode) {
    ImmutableList<String> errors = parseAndGetErrors(soyCode);
    if (!errors.isEmpty()) {
      throw new AssertionError(
          "Expected:\n" + soyCode + "\n to parse successfully, but got: " + errors);
    }
  }

  /**
   * Parse soyCode and execute the AssertStrictAutoescapingVisitor check on the output.
   *
   * @param soyCode The input code.
   */
  private void causesStrictException(String soyCode) {
    ImmutableList<String> errors = parseAndGetErrors(soyCode);
    for (String error : errors) {
      if (!error.equals("Invalid use of non-strict when strict autoescaping is required.")) {
        throw new AssertionError("Found unexpected error message: " + error);
      }
    }
    if (errors.isEmpty()) {
      throw new AssertionError("Expected:\n" + soyCode + "\n to have a strict escaping error");
    }
  }

  private ImmutableList<String> parseAndGetErrors(String soyCode) {
    FormattingErrorReporter errorReporter = new FormattingErrorReporter();
    SoyFileSetNode soyTree =
        SoyFileSetParserBuilder.forFileContents(soyCode)
            .errorReporter(errorReporter)
            .parse()
            .fileSet();
    new AssertStrictAutoescapingVisitor(errorReporter).exec(soyTree);
    return errorReporter.getErrorMessages();
  }
}
