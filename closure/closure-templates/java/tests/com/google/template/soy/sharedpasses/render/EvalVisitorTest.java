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

package com.google.template.soy.sharedpasses.render;

import static com.google.common.truth.Truth.assertThat;
import static com.google.template.soy.shared.SharedTestUtils.untypedTemplateBodyForExpression;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.template.soy.SoyFileSetParserBuilder;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.basicdirectives.BasicDirectivesModule;
import com.google.template.soy.basicfunctions.BasicFunctionsModule;
import com.google.template.soy.data.SoyDataException;
import com.google.template.soy.data.SoyDict;
import com.google.template.soy.data.SoyList;
import com.google.template.soy.data.SoyRecord;
import com.google.template.soy.data.SoyValue;
import com.google.template.soy.data.SoyValueHelper;
import com.google.template.soy.data.SoyValueProvider;
import com.google.template.soy.data.restricted.FloatData;
import com.google.template.soy.data.restricted.NullData;
import com.google.template.soy.data.restricted.StringData;
import com.google.template.soy.data.restricted.UndefinedData;
import com.google.template.soy.error.ExplodingErrorReporter;
import com.google.template.soy.exprparse.ExpressionParser;
import com.google.template.soy.exprtree.ExprNode;
import com.google.template.soy.exprtree.FunctionNode;
import com.google.template.soy.passes.SharedPassesModule;
import com.google.template.soy.shared.SharedTestUtils;
import com.google.template.soy.shared.internal.ErrorReporterModule;
import com.google.template.soy.shared.internal.SharedModule;
import com.google.template.soy.shared.restricted.SoyFunction;
import com.google.template.soy.sharedpasses.render.EvalVisitor.EvalVisitorFactory;
import com.google.template.soy.soytree.PrintNode;

import junit.framework.TestCase;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Unit tests for EvalVisitor.
 *
 */
public class EvalVisitorTest extends TestCase {

  private static final Injector INJECTOR = Guice.createInjector(
      new ErrorReporterModule(),
      new SharedModule(),
      new SharedPassesModule(),
      new BasicDirectivesModule(),
      new BasicFunctionsModule());

  protected static final SoyValueHelper VALUE_HELPER = INJECTOR.getInstance(SoyValueHelper.class);

  private SoyRecord testData;
  private static final SoyRecord TEST_IJ_DATA =
      VALUE_HELPER.newEasyDict("ijBool", true, "ijInt", 26, "ijStr", "injected");

  private final Map<String, SoyValueProvider> locals = Maps.newHashMap(
      ImmutableMap.<String, SoyValueProvider>of(
          "zoo", StringData.forValue("loo"),
          "woo", FloatData.forValue(-1.618)));


  @Override protected void setUp() {
    testData = createTestData();
    SharedTestUtils.simulateNewApiCall(INJECTOR);
  }

  protected SoyRecord createTestData() {
    SoyList tri = VALUE_HELPER.newEasyList(1, 3, 6, 10, 15, 21);
    return VALUE_HELPER.newEasyDict(
        "boo", 8, "foo.bar", "baz", "foo.goo2", tri, "goo", tri,
        "moo", 3.14, "t", true, "f", false, "n", null,
        "map0", VALUE_HELPER.newEasyDict(), "list0", VALUE_HELPER.newEasyList(),
        "longNumber", 1000000000000000001L,
        "floatNumber", 1.5);
  }


  /**
   * Evaluates the given expression and returns the result.
   * @param expression The expression to evaluate.
   * @return The expression result.
   * @throws Exception If there's an error.
   */
  private SoyValue eval(String expression) throws Exception {
    PrintNode code =
        (PrintNode)
            SoyFileSetParserBuilder.forTemplateContents(
                    // wrap in a function so we don't run into the 'can't print bools' error message
                    untypedTemplateBodyForExpression("fakeFunction(" + expression + ")"))
                .addSoyFunction(
                    new SoyFunction() {
                      @Override
                      public String getName() {
                        return "fakeFunction";
                      }

                      @Override
                      public Set<Integer> getValidArgsSizes() {
                        return ImmutableSet.of(1);
                      }
                    })
                .parse()
                .fileSet()
                .getChild(0)
                .getChild(0)
                .getChild(0);
    ExprNode expr = ((FunctionNode) code.getExprUnion().getExpr().getChild(0)).getChild(0);

    EvalVisitor evalVisitor =
        INJECTOR
            .getInstance(EvalVisitorFactory.class)
            .create(TEST_IJ_DATA, TestingEnvironment.createForTest(testData, locals));
    return evalVisitor.exec(expr);
  }


  /**
   * Asserts that the given expression evaluates to the given result.
   * @param expression The expression to evaluate.
   * @param result The expected expression result.
   * @throws Exception If the assertion is not true or if there's an error.
   */
  private void assertEval(String expression, boolean result) throws Exception {
    assertThat(eval(expression).booleanValue()).isEqualTo(result);
  }


  /**
   * Asserts that the given expression evaluates to the given result.
   * @param expression The expression to evaluate.
   * @param result The expected result.
   * @throws Exception If the assertion is not true or if there's an error.
   */
  private void assertEval(String expression, long result) throws Exception {
    assertThat(eval(expression).longValue()).isEqualTo(result);
  }


  /**
   * Asserts that the given expression evaluates to the given result.
   * @param expression The expression to evaluate.
   * @param result The expected result.
   * @throws Exception If the assertion is not true or if there's an error.
   */
  private void assertEval(String expression, double result) throws Exception {
    assertThat(eval(expression).floatValue()).isEqualTo(result);
  }


  /**
   * Asserts that the given expression evaluates to the given result.
   * @param expression The expression to evaluate.
   * @param result The expected result.
   * @throws Exception If the assertion is not true or if there's an error.
   */
  private void assertEval(String expression, String result) throws Exception {
    assertThat(eval(expression).stringValue()).isEqualTo(result);
  }


  /**
   * Asserts that evaluating the given expression causes a ParseException.
   * @param expression The expression to evaluate.
   */
  private void assertParseError(String expression) {
    try {
      new ExpressionParser(expression, SourceLocation.UNKNOWN, ExplodingErrorReporter.get())
          .parseExpression();
    } catch (IllegalStateException e) {
      return; // passes
    }
    fail("expected parse error, got none");
  }

  /**
   * Asserts that evaluating the given expression causes a ParseException.
   * @param expression The expression to evaluate.
   */
  private void assertParseError(String expression, String errorMsgSubstring) {
    try {
      new ExpressionParser(expression, SourceLocation.UNKNOWN, ExplodingErrorReporter.get())
          .parseExpression();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).contains(errorMsgSubstring);
      return;
    }
    fail("expected parse error, got none");
  }


  /**
   * Asserts that evaluating the given expression causes a RenderException.
   * @param expression The expression to evaluate.
   * @throws Exception If the assertion is not true or if there's an error.
   */
  private void assertRenderException(String expression, @Nullable String errorMsgSubstring)
      throws Exception {

    try {
      eval(expression);
      fail();

    } catch (RenderException re) {
      if (errorMsgSubstring != null) {
        assertThat(re.getMessage()).contains(errorMsgSubstring);
      }
      // Test passes.
    }
  }


  /**
   * Asserts that evaluating the given expression causes a SoyDataException.
   * @param expression The expression to evaluate.
   * @throws Exception If the assertion is not true or if there's an error.
   */
  private void assertDataException(String expression, @Nullable String errorMsgSubstring)
      throws Exception {

    try {
      eval(expression);
      fail();

    } catch (SoyDataException e) {
      if (errorMsgSubstring != null) {
        assertThat(e.getMessage()).contains(errorMsgSubstring);
      }
      // Test passes.
    }
  }


  // -----------------------------------------------------------------------------------------------
  // Tests begin here.


  public void testEvalPrimitives() throws Exception {
    assertThat(eval("null")).isInstanceOf(NullData.class);
    assertEval("true", true);
    assertEval("false", false);
    assertEval("26", 26);
    assertEval("8.27", 8.27);
    assertEval("'boo'", "boo");
  }


  public void testEvalListLiteral() throws Exception {

    SoyList result = (SoyList) eval("['blah', 123, $boo]");
    assertThat(result.length()).isEqualTo(3);
    assertThat(result.get(0).stringValue()).isEqualTo("blah");
    assertThat(result.get(1).integerValue()).isEqualTo(123);
    assertThat(result.get(2).integerValue()).isEqualTo(8);

    result = (SoyList) eval("['blah', 123, $boo,]");  // trailing comma
    assertThat(result.length()).isEqualTo(3);
    assertThat(result.get(0).stringValue()).isEqualTo("blah");
    assertThat(result.get(1).integerValue()).isEqualTo(123);
    assertThat(result.get(2).integerValue()).isEqualTo(8);

    result = (SoyList) eval("[]");
    assertThat(result.length()).isEqualTo(0);

    assertParseError("[,]");
  }


  public void testEvalMapLiteral() throws Exception {

    SoyDict result = (SoyDict) eval("[:]");
    assertThat(result.getItemKeys()).isEmpty();

    result = (SoyDict) eval("['aaa': 'blah', 'bbb': 123, $foo.bar: $boo]");
    assertThat(result.getItemKeys()).hasSize(3);
    assertThat(result.getField("aaa").stringValue()).isEqualTo("blah");
    assertThat(result.getField("bbb").integerValue()).isEqualTo(123);
    assertThat(result.getField("baz").integerValue()).isEqualTo(8);

    result = (SoyDict) eval("['aaa': 'blah', 'bbb': 123, $foo.bar: $boo,]");  // trailing comma
    assertThat(result.getItemKeys()).hasSize(3);
    assertThat(result.getField("aaa").stringValue()).isEqualTo("blah");
    assertThat(result.getField("bbb").integerValue()).isEqualTo(123);
    assertThat(result.getField("baz").integerValue()).isEqualTo(8);

    result = (SoyDict) eval("quoteKeysIfJs([:])");
    assertThat(result.getItemKeys()).isEmpty();

    result = (SoyDict) eval("quoteKeysIfJs( ['aaa': 'blah', 'bbb': 123, $foo.bar: $boo] )");
    assertThat(result.getItemKeys()).hasSize(3);
    assertThat(result.getField("aaa").stringValue()).isEqualTo("blah");
    assertThat(result.getField("bbb").integerValue()).isEqualTo(123);
    assertThat(result.getField("baz").integerValue()).isEqualTo(8);

    assertParseError("[:,]");
    assertParseError("[,:]");

    // Test error on single-identifier key.
    assertParseError(
        "[aaa: 'blah',]",
        "Disallowed single-identifier key \"aaa\" in map literal");
    assertParseError(
        "['aaa': 'blah', bbb: 123]",
        "Disallowed single-identifier key \"bbb\" in map literal");

    // Test last value overwrites earlier value for the same key.
    result = (SoyDict) eval("['baz': 'blah', $foo.bar: 'bluh']");
    assertThat(result.getField("baz").stringValue()).isEqualTo("bluh");
  }


  public void testEvalDataRefBasic() throws Exception {

    assertEval("$zoo", "loo");
    assertEval("$woo", -1.618);

    assertEval("$boo", 8);
    assertEval("$foo.bar", "baz");
    assertEval("$goo[2]", 6);

    assertEval("$ij.ijBool", true);
    assertEval("$ij.ijInt", 26);
    assertEval("$ij.ijStr", "injected");

    assertThat(eval("$too")).isInstanceOf(UndefinedData.class);
    assertThat(eval("$foo.too")).isInstanceOf(UndefinedData.class);
    assertThat(eval("$foo.goo2[22]")).isInstanceOf(UndefinedData.class);
    assertThat(eval("$ij.boo")).isInstanceOf(UndefinedData.class);

    // TODO: If enabling exception for undefined LHS (see EvalVisitor), uncomment tests below.
    //assertRenderException(
    //    "$foo.bar.moo.tar", "encountered undefined LHS just before accessing \".tar\"");
    assertThat(eval("$foo.bar.moo.tar")).isInstanceOf(UndefinedData.class);
    //assertRenderException(
    //    "$foo.baz.moo.tar", "encountered undefined LHS just before accessing \".moo\"");
    assertThat(eval("$foo.baz.moo.tar")).isInstanceOf(UndefinedData.class);
    assertRenderException("$boo?[2]", "encountered non-map/list just before accessing \"?[2]\"");
    assertRenderException(
        "$boo?['xyz']", "encountered non-map/list just before accessing \"?['xyz']\"");
    assertDataException(
        "$foo[2]",
        "SoyDict accessed with non-string key (got key type" +
            " com.google.template.soy.data.restricted.IntegerData).");
    assertThat(eval("$moo.too")).isInstanceOf(UndefinedData.class);
    //assertRenderException(
    //    "$roo.too", "encountered undefined LHS just before accessing \".too\"");
    assertThat(eval("$roo.too")).isInstanceOf(UndefinedData.class);
    //assertRenderException("$roo[2]", "encountered undefined LHS just before accessing \"[2]\"");
    assertThat(eval("$roo[2]")).isInstanceOf(UndefinedData.class);
    assertThat(eval("$ij.ijInt.boo")).isInstanceOf(UndefinedData.class);
    //assertRenderException(
    //    "$ij.ijZoo.boo", "encountered undefined LHS just before accessing \".boo\"");
    assertThat(eval("$ij.ijZoo.boo")).isInstanceOf(UndefinedData.class);
  }


  public void testEvalDataRefWithNullSafeAccess() throws Exception {

    // Note: Null-safe access only helps when left side is undefined or null, not when it's the
    // wrong type.
    assertRenderException(
        "$foo?.bar?.moo.tar", "encountered non-record just before accessing \"?.moo\"");
    assertThat(eval("$foo?.baz?.moo.tar")).isInstanceOf(NullData.class);
    assertDataException(
        "$foo[2]",
        "SoyDict accessed with non-string key (got key type" +
            " com.google.template.soy.data.restricted.IntegerData).");
    assertRenderException(
        "$moo?.too", "encountered non-record just before accessing \"?.too\"");
    assertThat(eval("$roo?.too")).isInstanceOf(NullData.class);
    assertThat(eval("$roo?[2]")).isInstanceOf(NullData.class);
    assertRenderException(
        "$ij.ijInt?.boo", "encountered non-record just before accessing \"?.boo\"");
    assertThat(eval("$ij.ijZoo?.boo")).isInstanceOf(NullData.class);
  }


  public void testEvalNumericalOperators() throws Exception {

    assertEval("-$boo", -8);

    assertEval("$goo[3]*3", 30);
    assertEval("2 * $moo", 6.28);

    assertEval("$goo[0] / 4", 0.25);
    assertEval("$woo/-0.8090", 2.0);

    assertEval("$boo % 3", 2);

    assertEval("-99+-111", -210);
    assertEval("$moo + $goo[5]", 24.14);
    assertEval("$ij.ijInt + $boo", 34);
    assertEval("'boo'+'hoo'", "boohoo");  // string concatenation
    assertEval("$foo.bar + $ij.ijStr", "bazinjected");  // string concatenation
    assertEval("8 + $zoo + 8.0", "8loo8");  // coercion to string type

    assertEval("$goo[4] - $boo", 7);
    assertEval("1.002- $woo", 2.62);

    // Ensure longs work.
    assertEval("$longNumber + $longNumber", 2000000000000000002L);
    assertEval("$longNumber * 4 - $longNumber", 3000000000000000003L);
    assertEval("$longNumber / $longNumber", 1.0);  // NOTE: Division is on floats.
    assertEval("$longNumber < ($longNumber + 1)", true);
    assertEval("$longNumber < ($longNumber - 1)", false);
  }


  public void testEvalDataRefWithExpressions() throws Exception {

    assertEval("$foo['bar']", "baz");
    assertEval("$goo[2]", 6);
    assertEval("$foo['goo' + 2][2+2]", 15);
    assertEval("$foo['goo'+2][4]", 15);
    assertEval("$foo.goo2[2 + 2]", 15);
  }


  public void testEvalBooleanOperators() throws Exception {

    assertEval("not $t", false);
    assertEval("not null", true);
    assertEval("not $boo", false);
    assertEval("not $ij.ijBool", false);
    assertEval("not 0.0", true);
    assertEval("not $foo.bar", false);
    assertEval("not ''", true);
    assertEval("not $foo", false);
    assertEval("not $map0", false);
    assertEval("not $goo", false);
    assertEval("not $list0", false);

    assertEval("false and $undefinedName", false);  // short-circuit evaluation
    assertEval("$t and -1 and $goo and $foo.bar", true);

    assertEval("true or $undefinedName", true);  // short-circuit evaluation
    assertEval("$f or 0.0 or ''", false);
  }


  public void testEvalComparisonOperators() throws Exception {

    assertEval("1<1", false);
    assertEval("$woo < 0", true);

    assertEval("$goo[0]>0", true);
    assertEval("$moo> 11.1111", false);

    assertEval("0 <= 0", true);
    assertEval("$moo <= -$woo", false);

    assertEval("2 >= $goo[2]", false);
    assertEval("4 >=$moo", true);

    assertEval("15==$goo[4]", true);
    assertEval("$woo == 1.61", false);
    assertEval("4.0 ==4", true);
    assertEval("$f == true", false);
    assertEval("null== null", true);
    assertEval("'$foo.bar' == $foo.bar", false);
    assertEval("$foo.bar == 'b' + 'a'+'z'", true);
    assertEval("$foo == $map0", false);
    assertEval("$foo.goo2 == $goo", true);
    assertEval("'22' == 22", true);
    assertEval("'22' == '' + 22", true);

    assertEval("$goo[4]!=15", false);
    assertEval("1.61 != $woo", true);
    assertEval("4 !=4.0", false);
    assertEval("true != $f", true);
    assertEval("null!= null", false);
    assertEval("$foo.bar != '$foo.bar'", true);
    assertEval("'b' + 'a'+'z' != $foo.bar", false);
    assertEval("$map0 != $foo", true);
    assertEval("$goo != $foo.goo2", false);
    assertEval("22 != '22'", false);
    assertEval("'' + 22 != '22'", false);

    assertEval("$longNumber < $longNumber", false);
    assertEval("$longNumber < ($longNumber - 1)", false);
    assertEval("($longNumber - 1) < $longNumber", true);

    assertEval("$longNumber <= $longNumber", true);
    assertEval("$longNumber <= ($longNumber - 1)", false);
    assertEval("($longNumber - 1) <= $longNumber", true);

    assertEval("$longNumber > $longNumber", false);
    assertEval("$longNumber > ($longNumber - 1)", true);
    assertEval("($longNumber - 1) > $longNumber", false);

    assertEval("$longNumber >= $longNumber", true);
    assertEval("$longNumber >= ($longNumber - 1)", true);
    assertEval("($longNumber - 1) >= $longNumber", false);

    assertEval("$floatNumber < $floatNumber", false);
    assertEval("$floatNumber < ($floatNumber - 1)", false);
    assertEval("($floatNumber - 1) < $floatNumber", true);

    assertEval("$floatNumber <= $floatNumber", true);
    assertEval("$floatNumber <= ($floatNumber - 1)", false);
    assertEval("($floatNumber - 1) <= $floatNumber", true);

    assertEval("$floatNumber > $floatNumber", false);
    assertEval("$floatNumber > ($floatNumber - 1)", true);
    assertEval("($floatNumber - 1) > $floatNumber", false);

    assertEval("$floatNumber >= $floatNumber", true);
    assertEval("$floatNumber >= ($floatNumber - 1)", true);
    assertEval("($floatNumber - 1) >= $floatNumber", false);
  }


  public void testEvalConditionalOperator() throws Exception {

    assertEval("($f and 0)?4 : '4'", "4");
    assertEval("$goo ? $goo[1]:1", 3);
  }


  public void testEvalFunctions() throws Exception {

    assertEval("isNonnull(null)", false);
    assertEval("isNonnull(0)", true);
    assertEval("isNonnull(1)", true);
    assertEval("isNonnull(false)", true);
    assertEval("isNonnull(true)", true);
    assertEval("isNonnull('')", true);
    assertEval("isNonnull($undefined)", false);
    assertEval("isNonnull($n)", false);
    assertEval("isNonnull($boo)", true);
    assertEval("isNonnull($foo.goo2)", true);
    assertEval("isNonnull($map0)", true);
  }

}
