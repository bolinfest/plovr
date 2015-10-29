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

package com.google.template.soy.pysrc.internal;

import static com.google.common.truth.Truth.assertThat;

import com.google.template.soy.SoyFileSetParserBuilder;
import com.google.template.soy.shared.SharedTestUtils;
import com.google.template.soy.soytree.SoyFileSetNode;
import com.google.template.soy.soytree.SoyNode;

import junit.framework.TestCase;

/**
 * Unit tests for IsComputableAsPyExprVisitor.
 *
 */
public class IsComputableAsPyExprVisitorTest extends TestCase {

  public void testAlwaysTrueNodes() {
    runTestHelper("Blah blah.", true);
    runTestHelper("{@param boo:?}\n{$boo.foo}", true);
    // TODO(dcphillips): Add tests for other nodes (such as messages) when support is available.
  }

  public void testAlwaysFalseNodes() {
    runTestHelper("{let $data: 'foo'/}", false);
    runTestHelper(
        "{@param boo:?}\n{switch $boo}{case 0}Blah{case 1}Bleh{default}Bluh{/switch}", false);
    runTestHelper("{@param booze:?}\n{foreach $boo in $booze}{$boo}{/foreach}", false);
    runTestHelper("{for $i in range(4)}{$i + 1}{/for}", false);
  }

  public void testIfNode() {
    runTestHelper(
        "{@param boo:?}\n{@param foo:?}\n{if $boo}Blah{elseif $foo}Bleh{else}Bluh{/if}", true);
    runTestHelper(
        "{@param goo:?}\n{@param moose:?}\n{if $goo}{foreach $moo in $moose}{$moo}{/foreach}{/if}",
        false);
  }

  public void testCallNode() {
    runTestHelper("{call .foo data=\"all\" /}", true);
    runTestHelper(
        "{@param boo:?}\n{@param moo:?}\n{call .foo data=\"$boo\"}{param goo : $moo /}{/call}",
        true);
    runTestHelper("{@param boo:?}\n{call .foo data=\"$boo\"}{param goo}Blah{/param}{/call}", true);
    runTestHelper(
        "{@param boo:?}\n"
            + "{@param moose:?}\n"
            + "{call .foo data=\"$boo\"}"
            + "  {param goo}"
            + "    {foreach $moo in $moose}"
            + "      {$moo}"
            + "    {/foreach}"
            + "  {/param}"
            + "{/call}",
        false);
  }

  private static void runTestHelper(String soyNodeCode, boolean expectedResult) {
    SoyFileSetNode soyTree =
        SoyFileSetParserBuilder.forTemplateContents(soyNodeCode).parse().fileSet();
    SoyNode node = SharedTestUtils.getNode(soyTree, 0);
    assertThat(new IsComputableAsPyExprVisitor().exec(node)).isEqualTo(expectedResult);
  }
}
