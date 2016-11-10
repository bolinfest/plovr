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

package com.google.common.css.compiler.passes;

import com.google.common.css.compiler.ast.CssConditionalBlockNode;
import com.google.common.css.compiler.ast.CssConditionalRuleNode;
import com.google.common.css.compiler.ast.CssDeclarationBlockNode;
import com.google.common.css.compiler.ast.CssNode;
import com.google.common.css.compiler.ast.CssRulesetNode;
import com.google.common.css.compiler.ast.testing.NewFunctionalTestBase;

/**
 * Unit tests for {@link CreateConditionalNodes}.
 *
 */
public class CreateConditionalNodesTest extends NewFunctionalTestBase {

  @Override
  protected void runPass() {
    CreateConditionalNodes pass = new CreateConditionalNodes(
        tree.getMutatingVisitController(), errorManager);
    pass.runPass();
  }

  public void testCreateSimpleConditionalBlockNode() throws Exception {
    parseAndRun("@if (!X){ a {b: c} } @else { d {e: f} }");
    assertTrue(getFirstActualNode() instanceof CssConditionalBlockNode);
    CssConditionalBlockNode condBlock =
        (CssConditionalBlockNode) getFirstActualNode();
    CssConditionalRuleNode condRuleIf = condBlock.getChildren().get(0);
    CssConditionalRuleNode condRuleElse = condBlock.getChildren().get(1);
    assertEquals("if", condRuleIf.getName().getValue());
    assertEquals(1, condRuleIf.getParametersCount());
    assertEquals("[[a]{[b:[c]]}]", condRuleIf.getBlock().toString());
    assertEquals("else", condRuleElse.getName().getValue());
    assertEquals(0, condRuleElse.getParametersCount());
    assertEquals("[[d]{[e:[f]]}]", condRuleElse.getBlock().toString());
  }

  public void testCreateNestedConditionalBlockNode() throws Exception {
    parseAndRun("@if X {a {b: c} } @else { @if (Y) {d {e: f} } }");
    assertTrue(getFirstActualNode() instanceof CssConditionalBlockNode);
    CssConditionalBlockNode condBlock = (CssConditionalBlockNode) getFirstActualNode();
    assertEquals(2, condBlock.getChildren().size());
    CssConditionalRuleNode condRuleIf = condBlock.getChildren().get(0);
    CssConditionalRuleNode condRuleElse = condBlock.getChildren().get(1);
    assertEquals("if", condRuleIf.getName().getValue());
    assertEquals(1, condRuleIf.getParametersCount());
    assertEquals("[[a]{[b:[c]]}]", condRuleIf.getBlock().toString());
    assertEquals("else", condRuleElse.getName().getValue());
    assertEquals(0, condRuleElse.getParametersCount());
    assertEquals(1, condRuleElse.getBlock().getChildren().size());
    CssNode child = condRuleElse.getBlock().getChildren().get(0);
    assertTrue(child instanceof CssConditionalBlockNode);
    CssConditionalBlockNode elseCondBlock = (CssConditionalBlockNode) child;
    assertEquals(1, elseCondBlock.getChildren().size());
    CssConditionalRuleNode elseCondRuleIf = elseCondBlock.getChildren().get(0);
    assertEquals("if", elseCondRuleIf.getName().getValue());
    assertEquals(1, elseCondRuleIf.getParametersCount());
    assertEquals("[[d]{[e:[f]]}]", elseCondRuleIf.getBlock().toString());
  }

  public void testCreateConditionalBlockNodeInRuleset() throws Exception {
    parseAndRun("a {@if X {b: c} @else {d: e} }");
    assertTrue(getFirstActualNode() instanceof CssRulesetNode);
    CssRulesetNode ruleset = (CssRulesetNode) getFirstActualNode();
    assertEquals("[a]{[[@if[X]{[b:[c]]}, @else[]{[d:[e]]}]]}", ruleset.toString());
    CssDeclarationBlockNode declarationBlock = ruleset.getDeclarations();
    assertEquals(1, declarationBlock.getChildren().size());
    assertTrue(declarationBlock.getChildAt(0) instanceof CssConditionalBlockNode);
    CssConditionalBlockNode condBlock = (CssConditionalBlockNode) declarationBlock.getChildAt(0);
    assertEquals(2, condBlock.getChildren().size());
    CssConditionalRuleNode condRuleIf = condBlock.getChildren().get(0);
    CssConditionalRuleNode condRuleElse = condBlock.getChildren().get(1);
    assertEquals("if", condRuleIf.getName().getValue());
    assertEquals(1, condRuleIf.getParametersCount());
    assertEquals("[b:[c]]", condRuleIf.getBlock().toString());
    assertEquals("else", condRuleElse.getName().getValue());
    assertEquals(0, condRuleElse.getParametersCount());
    assertEquals("[d:[e]]", condRuleElse.getBlock().toString());
  }

  public void testIfWithoutBlockError() throws Exception {
    parseAndRun("@if (X) ;", "@if without block");
  }

  public void testIfWithoutConditionError() throws Exception {
    parseAndRun("@if {a {b: c} }", "@if without condition");
  }

  public void testIfWithTooManyParametersError() throws Exception {
    parseAndRun("@if X Y {a {b: c}}", "@if with too many parameters");
  }

  public void testElseTooManyParametersError() throws Exception {
    parseAndRun("@if (X) {a {b: c}} @else (Y) {a {b: c}}", "@else with too many parameters");
  }

  public void testElseWithoutIfError() throws Exception {
    parseAndRun("@else {a {b: c}}", "@else without previous @if");
  }

  public void testElseIfAfterElseError() throws Exception {
    parseAndRun("@if (X) {a {b: c}} @else {a {b: c}} @elseif (Y) {a {b: c}}",
                "@elseif without previous @if");
  }

  public void testElseAfterRuleError() throws Exception {
    parseAndRun("@if (X && Y) {a {b: c}} a {b: c} @else {a {b: c}}",
                "@else without previous @if");
  }

  public void testNestedElseWithoutIfError() throws Exception {
    parseAndRun("@if X { @else {a {b: c}} }",
                "@else without previous @if");
  }
}
