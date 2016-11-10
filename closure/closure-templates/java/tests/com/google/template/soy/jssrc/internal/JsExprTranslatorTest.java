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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.template.soy.SoyModule;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.error.ExplodingErrorReporter;
import com.google.template.soy.exprtree.FunctionNode;
import com.google.template.soy.exprtree.IntegerNode;
import com.google.template.soy.exprtree.NullNode;
import com.google.template.soy.exprtree.OperatorNodes.TimesOpNode;
import com.google.template.soy.jssrc.restricted.JsExpr;
import com.google.template.soy.passes.ResolveFunctionsVisitor;
import com.google.template.soy.shared.restricted.SoyFunction;

import junit.framework.TestCase;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Unit tests for JsExprTranslator.
 *
 */
public final class JsExprTranslatorTest extends TestCase {
  private static final Injector INJECTOR = Guice.createInjector(new SoyModule());

  private static final ImmutableMap<String, ? extends SoyFunction> SOY_FUNCTIONS =
      INJECTOR.getInstance(new Key<ImmutableMap<String, ? extends SoyFunction>>() {});

  public void testTranslateToJsExpr() {
    JsSrcTestUtils.simulateNewApiCall(INJECTOR);
    JsExprTranslator jsExprTranslator = INJECTOR.getInstance(JsExprTranslator.class);

    TimesOpNode expr = new TimesOpNode(SourceLocation.UNKNOWN);
    expr.addChild(new IntegerNode(3, SourceLocation.UNKNOWN));
    // will be replaced with one of the functions below
    expr.addChild(new NullNode(SourceLocation.UNKNOWN));

    FunctionNode userFnNode = new FunctionNode("userFn", SourceLocation.UNKNOWN);
    userFnNode.addChild(new IntegerNode(5, SourceLocation.UNKNOWN));

    FunctionNode randomIntFnNode = new FunctionNode("randomInt", SourceLocation.UNKNOWN);
    randomIntFnNode.addChild(new IntegerNode(4, SourceLocation.UNKNOWN));

    Deque<Map<String, JsExpr>> localVarTranslations = new ArrayDeque<>();

    // Test unsupported function (Soy V1 syntax).
    expr.replaceChild(1, userFnNode);
    new ResolveFunctionsVisitor(SOY_FUNCTIONS).exec(expr);
    String exprText = "3   *   userFn(5)";
    assertThat(
            jsExprTranslator
                .translateToJsExpr(
                    expr, exprText, localVarTranslations, ExplodingErrorReporter.get())
                .getText())
        .isEqualTo("3 * userFn(5)");

    // Test supported function.
    expr.replaceChild(1, randomIntFnNode);
    new ResolveFunctionsVisitor(SOY_FUNCTIONS).exec(expr);
    exprText = "3   *   randomInt(4)";
    assertThat(
            jsExprTranslator
                .translateToJsExpr(
                    expr, exprText, localVarTranslations, ExplodingErrorReporter.get())
                .getText())
        .isEqualTo("3 * Math.floor(Math.random() * 4)");
  }
}
