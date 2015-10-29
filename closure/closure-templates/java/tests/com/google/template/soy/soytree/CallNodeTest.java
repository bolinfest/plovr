/*
 * Copyright 2010 Google Inc.
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

package com.google.template.soy.soytree;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.error.ExplodingErrorReporter;

import junit.framework.TestCase;

/**
 * Unit tests for CallNode.
 *
 */
public final class CallNodeTest extends TestCase {


  /** Escaping list of directive names. */
  private static final ImmutableList<String> NO_ESCAPERS = ImmutableList.of();

  public void testCommandText() {

    checkCommandText("foo");
    checkCommandText(".foo data=\"all\"");
    checkCommandText(" .baz data=\"$x\"", ".baz data=\"$x\"");

    try {
      checkCommandText(".foo.bar data=\"$x\"");
      fail();
    } catch (IllegalStateException e) {
      // Test passes.
    }
  }


  public void testSetEscapingDirectiveNames() {
    CallBasicNode callNode = new CallBasicNode.Builder(0, SourceLocation.UNKNOWN)
        .commandText(".foo")
        .build(ExplodingErrorReporter.get());
    assertThat(callNode.getEscapingDirectiveNames()).isEmpty();
    callNode.setEscapingDirectiveNames(ImmutableList.of("hello", "world"));
    assertEquals(ImmutableList.of("hello", "world"), callNode.getEscapingDirectiveNames());
    callNode.setEscapingDirectiveNames(ImmutableList.of("bye", "world"));
    assertEquals(ImmutableList.of("bye", "world"), callNode.getEscapingDirectiveNames());
  }


  private void checkCommandText(String commandText) {
    checkCommandText(commandText, commandText);
  }


  private void checkCommandText(String commandText, String expectedCommandText) {

    CallBasicNode callNode = new CallBasicNode.Builder(0, SourceLocation.UNKNOWN)
        .commandText(commandText)
        .build(ExplodingErrorReporter.get());
    if (callNode.getCalleeName() == null) {
      callNode.setCalleeName("testNamespace" + callNode.getSrcCalleeName());
    }


    CallBasicNode normCallNode = new CallBasicNode.Builder(0, SourceLocation.UNKNOWN)
        .calleeName(callNode.getCalleeName())
        .sourceCalleeName(callNode.getSrcCalleeName())
        .dataAttribute(callNode.dataAttribute())
        .userSuppliedPlaceholderName(callNode.getUserSuppliedPhName())
        .syntaxVersionBound(callNode.getSyntaxVersionUpperBound())
        .escapingDirectiveNames(NO_ESCAPERS)
        .build(ExplodingErrorReporter.get());

    assertThat(normCallNode.getCommandText()).isEqualTo(expectedCommandText);
    assertThat(normCallNode.getSyntaxVersionUpperBound()).isEqualTo(callNode.getSyntaxVersionUpperBound());
    assertThat(normCallNode.getCalleeName()).isEqualTo(callNode.getCalleeName());
    assertThat(normCallNode.dataAttribute()).isEqualTo(callNode.dataAttribute());
  }

}
