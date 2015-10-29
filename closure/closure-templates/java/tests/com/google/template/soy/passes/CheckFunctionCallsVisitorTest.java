/*
 * Copyright 2011 Google Inc.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.template.soy.SoyFileSetParserBuilder;
import com.google.template.soy.basetree.SyntaxVersion;
import com.google.template.soy.error.FormattingErrorReporter;

import junit.framework.TestCase;

/**
 */
public final class CheckFunctionCallsVisitorTest extends TestCase {

  public void testPureFunctionOk() {
    assertSuccess(
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " * @param x",
        " * @param y",
        " */",
        "{template .foo}",
        "  {print min($x, $y)}",
        "{/template}");
  }

  public void testIncorrectArity() {
    assertFunctionCallsInvalid(
        "Function 'min' called with 1 arguments (expected 2).",
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " * @param x",
        " */",
        "{template .foo}",
        "  {print min($x)}",
        "{/template}");
    assertFunctionCallsInvalid(
        "Function 'index' called with 0 arguments (expected 1).",
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " */",
        "{template .foo}",
        "  {print index()}",
        "{/template}");
  }

  public void testNestedFunctionCall() {
    assertFunctionCallsInvalid(
        "Function 'min' called with 1 arguments (expected 2).",
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " * @param x",
        " * @param y",
        " */",
        "{template .foo}",
        "  {print min(min($x), min($x, $y))}",
        "{/template}");
  }

  public void testNotALoopVariable1() {
    assertFunctionCallsInvalid(
        "Function 'index' must have a foreach loop variable as its argument",
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " * @param x",
        " */",
        "{template .foo}",
        "  {print index($x)}",
        "{/template}");
  }

  public void testNotALoopVariable2() {
    assertFunctionCallsInvalid(
        "Function 'index' must have a foreach loop variable as its argument",
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " * @param x",
        " */",
        "{template .foo}",
        "  {print index($x.y)}",
        "{/template}");
  }

  public void testNotALoopVariable3() {
    assertFunctionCallsInvalid(
        "Function 'index' must have a foreach loop variable as its argument",
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " */",
        "{template .foo}",
        "  {print index($ij.data)}",
        "{/template}");
  }

  public void testNotALoopVariable4() {
    assertFunctionCallsInvalid(
        "Function 'index' must have a foreach loop variable as its argument",
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " * @param x",
        " */",
        "{template .foo}",
        "  {print index($x + 1)}",
        "{/template}");
  }

  public void testLoopVariableOk() {
    assertSuccess(
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " * @param elements",
        " */",
        "{template .foo}",
        "  {foreach $z in $elements}",
        "    {if isLast($z)}Lorem Ipsum{/if}",
        "  {/foreach}",
        "{/template}");
  }

  public void testLoopVariableNotInScopeWhenEmpty() {
    assertFunctionCallsInvalid(
        "Function 'index' must have a foreach loop variable as its argument",
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " * @param elements",
        " */",
        "{template .foo}",
        "  {foreach $z in $elements}",
        "    Lorem Ipsum...",
        "  {ifempty}",
        "    {print index($elements)}", // Loop variable not in scope when empty.
        "  {/foreach}",
        "{/template}");
  }

  public void testQuoteKeysIfJsFunction() {
    assertSuccess(
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/***/",
        "{template .foo}",
        "  {let $m: quoteKeysIfJs(['a': 1, 'b': 'blah']) /}",
        "{/template}");

    assertFunctionCallsInvalid(
        "Function 'quoteKeysIfJs' called with argument of type string (expected map literal).",
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/***/",
        "{template .foo}",
        "  {let $m: quoteKeysIfJs('blah') /}",
        "{/template}");
  }

  public void testUnrecognizedFunction() {
    assertFunctionCallsInvalid(
        "Unknown function 'bogus'.",
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "/**",
        " */",
        "{template .foo}",
        "  {print bogus()}",
        "{/template}");
  }

  public void testUnrecognizedFunctionOkInV1() {
    assertPasses(
        SyntaxVersion.V1_0,
        "{namespace ns autoescape=\"deprecated-noncontextual\"}\n",
        "{template .foo}",
        "  {print bogus()}",
        "{/template}");
  }

  private void assertSuccess(String... lines) {
    assertPasses(SyntaxVersion.V2_0, lines);
  }

  private void assertPasses(SyntaxVersion declaredSyntaxVersion, String... lines) {
    SoyFileSetParserBuilder.forFileContents(Joiner.on('\n').join(lines))
        .declaredSyntaxVersion(declaredSyntaxVersion)
        .parse()
        .fileSet();
  }

  private void assertFunctionCallsInvalid(String errorMessage, String... lines) {
    FormattingErrorReporter errorReporter = new FormattingErrorReporter();
    SoyFileSetParserBuilder.forFileContents(Joiner.on('\n').join(lines))
        .errorReporter(errorReporter)
        .parse();
    assertThat(errorReporter.getErrorMessages()).hasSize(1);
    assertThat(Iterables.getOnlyElement(errorReporter.getErrorMessages())).contains(errorMessage);
  }
}
