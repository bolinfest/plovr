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

package com.google.template.soy.jssrc.internal;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.template.soy.SoyFileSetParserBuilder;
import com.google.template.soy.jssrc.restricted.JsExpr;
import com.google.template.soy.shared.SharedTestUtils;
import com.google.template.soy.shared.internal.ErrorReporterModule;
import com.google.template.soy.soytree.CallNode;
import com.google.template.soy.soytree.SoyFileSetNode;

import junit.framework.TestCase;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Unit tests for GenCallCodeUtils.
 *
 */
public class GenCallCodeUtilsTest extends TestCase {


  private static final Injector INJECTOR =
      Guice.createInjector(new ErrorReporterModule(), new JsSrcModule());

  private static final Deque<Map<String, JsExpr>> LOCAL_VAR_TRANSLATIONS =
      new ArrayDeque<Map<String, JsExpr>>();


  public void testGenCallExprForBasicCalls() {
    assertThat(getCallExprTextHelper("{call some.func data=\"all\" /}"))
        .isEqualTo("some.func(opt_data)");

    assertThat(getCallExprTextHelper("{@param boo : ?}", "{call some.func data=\"$boo.foo\" /}"))
        .isEqualTo("some.func(opt_data.boo.foo)");

    assertThat(
            getCallExprTextHelper(
                "{@param moo : ?}", "{call some.func}", "  {param goo: $moo /}", "{/call}"))
        .isEqualTo("some.func({goo: opt_data.moo})");

    assertThat(
            getCallExprTextHelper(
                "{@param boo : ?}",
                "{call some.func data=\"$boo\"}",
                "  {param goo}Blah{/param}",
                "{/call}"))
        .isEqualTo("some.func(soy.$$augmentMap(opt_data.boo, {goo: 'Blah'}))");

    String callExprText =
        getCallExprTextHelper(
            "{call some.func}\n" +
            "  {param goo}\n" +
            "    {for $i in range(3)}{$i}{/for}\n" +
            "  {/param}\n" +
            "{/call}\n");
    assertThat(callExprText).matches("some[.]func[(][{]goo: param[0-9]+[}][)]");
  }


  public void testGenCallExprForBasicCallsWithTypedParamBlocks() {
    assertThat(
            getCallExprTextHelper(
                "{@param boo : ?}",
                "{call some.func data=\"$boo\"}",
                "  {param goo kind=\"html\"}Blah{/param}",
                "{/call}"))
        .isEqualTo(
            "some.func(soy.$$augmentMap(opt_data.boo, "
                + "{goo: soydata.VERY_UNSAFE.$$ordainSanitizedHtmlForInternalBlocks('Blah')}))");

    final String callExprText =
        getCallExprTextHelper(
            "{call some.func}\n" +
            "  {param goo kind=\"html\"}\n" +
            "    {for $i in range(3)}{$i}{/for}\n" +
            "  {/param}\n" +
            "{/call}\n");
    // NOTE: Soy generates a param### variable to store the output of the for loop.
    assertWithMessage("Actual result: " + callExprText)
        .that(callExprText.matches(
            "some[.]func[(][{]goo: soydata.VERY_UNSAFE.[$][$]ordainSanitizedHtmlForInternalBlocks"
            + "[(]param[0-9]+[)][}][)]"))
        .isTrue();
  }


  public void testGenCallExprForDelegateCalls() {
    assertThat(getCallExprTextHelper("{delcall myDelegate data=\"all\" /}"))
        .isEqualTo("soy.$$getDelegateFn(soy.$$getDelTemplateId('myDelegate'), '', false)(opt_data)");

    assertThat(
        getCallExprTextHelper("{delcall myDelegate data=\"all\" allowemptydefault=\"true\" /}"))
        .isEqualTo(
            "soy.$$getDelegateFn(soy.$$getDelTemplateId('myDelegate'), '', true)(opt_data)");

    assertThat(
            getCallExprTextHelper(
                "{@param moo : ?}",
                "{delcall my.other.delegate}",
                "  {param goo: $moo /}",
                "{/delcall}"))
        .isEqualTo(
            "soy.$$getDelegateFn("
                + "soy.$$getDelTemplateId('my.other.delegate'), '', false)({goo: opt_data.moo})");
  }


  public void testGenCallExprForDelegateVariantCalls() {
    assertThat(getCallExprTextHelper("{delcall myDelegate variant=\"'voo'\" data=\"all\" /}"))
        .isEqualTo(
            "soy.$$getDelegateFn(soy.$$getDelTemplateId('myDelegate'), 'voo', false)(opt_data)");

    assertThat(
            getCallExprTextHelper(
                "{@param voo : ?}",
                "{delcall myDelegate variant=\"$voo\" data=\"all\" allowemptydefault=\"true\" /}"))
        .isEqualTo(
            "soy.$$getDelegateFn(soy.$$getDelTemplateId('myDelegate'), opt_data.voo, true)"
                + "(opt_data)");

    assertThat(
            getCallExprTextHelper(
                "{@param moo : ?}",
                "{delcall my.other.delegate variant=\"'voo' + $ij.voo\"}",
                "  {param goo: $moo /}",
                "{/delcall}"))
        .isEqualTo(
            "soy.$$getDelegateFn("
                + "soy.$$getDelTemplateId('my.other.delegate'), 'voo' + opt_ijData.voo, false)"
                + "({goo: opt_data.moo})");
  }


  public void testGenCallExprForDelegateCallsWithTypedParamBlocks() {
    assertThat(
            getCallExprTextHelper(
                "{delcall my.other.delegate}",
                "  {param goo kind=\"html\"}Blah{/param}",
                "{/delcall}"))
        .isEqualTo(
            "soy.$$getDelegateFn(soy.$$getDelTemplateId('my.other.delegate'), '', false)("
                + "{goo: soydata.VERY_UNSAFE.$$ordainSanitizedHtmlForInternalBlocks('Blah')})");

    {
      final String callExprText =
          getCallExprTextHelper(
              "{delcall my.other.delegate}",
              "  {param goo kind=\"html\"}",
              "    {for $i in range(3)}{$i}{/for}",
              "  {/param}",
              "{/delcall}");
      assertWithMessage("Actual text:" + callExprText)
          .that(
              callExprText.matches(
                  "soy.\\$\\$getDelegateFn\\("
                      + "soy.\\$\\$getDelTemplateId\\('my.other.delegate'\\), '', false\\)"
                      + "[(][{]goo: soydata.VERY_UNSAFE.[$][$]ordainSanitizedHtmlForInternalBlocks"
                      + "[(]param[0-9]+[)][}][)]"))
          .isTrue();
    }
  }


  public void testGenCallExprForStrictCall() {
    assertThat(getCallExprTextHelper("{call some.func /}\n", ImmutableList.of("|escapeHtml")))
        .isEqualTo("soy.$$escapeHtml(some.func(null))");
  }


  private String getCallExprTextHelper(String... callSourceLines) {
    return getCallExprTextHelper(Joiner.on('\n').join(callSourceLines), ImmutableList.<String>of());
  }


  private String getCallExprTextHelper(
      String callSource, ImmutableList<String> escapingDirectives) {

    SoyFileSetNode soyTree =
        SoyFileSetParserBuilder.forTemplateContents(callSource).parse().fileSet();
    CallNode callNode = (CallNode) SharedTestUtils.getNode(soyTree, 0);
    // Manually setting the escaping directives.
    callNode.setEscapingDirectiveNames(escapingDirectives);

    JsSrcTestUtils.simulateNewApiCall(INJECTOR);
    GenCallCodeUtils genCallCodeUtils = INJECTOR.getInstance(GenCallCodeUtils.class);
    JsExpr callExpr =
        genCallCodeUtils.genCallExpr(callNode, LOCAL_VAR_TRANSLATIONS, AliasUtils.IDENTITY_ALIASES);
    return callExpr.getText();
  }

}
