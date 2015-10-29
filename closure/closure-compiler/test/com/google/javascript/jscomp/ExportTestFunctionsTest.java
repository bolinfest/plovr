/*
 * Copyright 2009 The Closure Compiler Authors.
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
package com.google.javascript.jscomp;

/**
 * Tests for ExportTestFunctions.
 *
 */
public final class ExportTestFunctionsTest extends Es6CompilerTestCase {

  private static final String EXTERNS =
      "function google_exportSymbol(a, b) {}; "
      + "function google_exportProperty(a, b, c) {};";

  private static final String TEST_FUNCTIONS_WITH_NAMES =
      "function Foo(arg) {}; "
      + "function setUp(arg3) {}; "
      + "function tearDown(arg, arg2) {}; "
      + "function testBar(arg) {}; "
      + "function test$(arg) {}; "
      + "function test$foo(arg) {}";

  public ExportTestFunctionsTest() {
    super(EXTERNS);
  }

  @Override
  public void setUp() {
    super.enableLineNumberCheck(false);
  }

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new ExportTestFunctions(compiler, "google_exportSymbol",
        "google_exportProperty");
  }

  @Override
  protected int getNumRepetitions() {
    // This pass only runs once.
    return 1;
  }

  public void testFunctionsAreExported() {
    test(TEST_FUNCTIONS_WITH_NAMES,
        "function Foo(arg){}; "
        + "function setUp(arg3){} google_exportSymbol(\"setUp\",setUp);; "
        + "function tearDown(arg,arg2) {} "
        + "google_exportSymbol(\"tearDown\",tearDown);; "
        + "function testBar(arg){} google_exportSymbol(\"testBar\",testBar);; "
        + "function test$(arg){} google_exportSymbol(\"test$\",test$);; "
        + "function test$foo(arg){} google_exportSymbol(\"test$foo\",test$foo)"
    );
  }

  // Helper functions
  public void testBasicTestFunctionsAreExported() {
    testSame("function Foo() {function testA(){}}");
    test("function setUp() {}",
         "function setUp(){} google_exportSymbol('setUp',setUp)");
    test("function setUpPage() {}",
         "function setUpPage(){} google_exportSymbol('setUpPage',setUpPage)");
    test("function shouldRunTests() {}",
         "function shouldRunTests(){}"
             + "google_exportSymbol('shouldRunTests',shouldRunTests)");
    test("function tearDown() {}",
         "function tearDown(){} google_exportSymbol('tearDown',tearDown)");
    test("function tearDownPage() {}",
         "function tearDownPage(){} google_exportSymbol('tearDownPage'," +
         "tearDownPage)");
    test("function testBar() { function testB() {}}",
         "function testBar(){function testB(){}}"
             + "google_exportSymbol('testBar',testBar)");
    testSame("var testCase = {}; testCase.setUpPage = function() {}");
  }

  /**
   * Make sure this works for global functions declared as function expressions:
   * <pre>
   * var testFunctionName = function() {
   *   // Implementation
   * };
   * </pre>
   * This format should be supported in addition to function statements.
   */
  public void testFunctionExpressionsAreExported() {
    testSame("var Foo = function() {var testA = function() {}}");
    test("var setUp = function() {}",
         "var setUp = function() {}; " +
         "google_exportSymbol('setUp',setUp)");
    test("var setUpPage = function() {}",
         "var setUpPage = function() {}; " +
         "google_exportSymbol('setUpPage',setUpPage)");
    test("var shouldRunTests = function() {}",
         "var shouldRunTests = function() {}; " +
         "google_exportSymbol('shouldRunTests',shouldRunTests)");
    test("var tearDown = function() {}",
         "var tearDown = function() {}; " +
         "google_exportSymbol('tearDown',tearDown)");
    test("var tearDownPage = function() {}",
         "var tearDownPage = function() {}; " +
         "google_exportSymbol('tearDownPage', tearDownPage)");
    test("var testBar = function() { var testB = function() {}}",
         "var testBar = function(){ var testB = function() {}}; " +
         "google_exportSymbol('testBar',testBar)");
  }

  public void testFunctionExpressionsByLetAreExported() {
    testSameEs6("let Foo = function() {var testA = function() {}}");
    testEs6("let setUp = function() {}",
        LINE_JOINER.join(
            "let setUp = function() {}; ",
            "google_exportSymbol('setUp', setUp)"));
    testEs6("let testBar = function() {}",
        LINE_JOINER.join(
            "let testBar = function() {}; ",
            "google_exportSymbol('testBar', testBar)"));
    testEs6("let tearDown = function() {}",
        LINE_JOINER.join(
            "let tearDown = function() {}; ",
            "google_exportSymbol('tearDown', tearDown)"));
  }

  public void testFunctionExpressionsByConstAreExported() {
    testSameEs6("const Foo = function() {var testA = function() {}}");
    testEs6("const setUp = function() {}",
        LINE_JOINER.join(
            "const setUp = function() {}; ",
            "google_exportSymbol('setUp', setUp)"));
    testEs6("const testBar = function() {}",
        LINE_JOINER.join(
            "const testBar = function() {}; ",
            "google_exportSymbol('testBar', testBar)"));
    testEs6("const tearDown = function() {}",
        LINE_JOINER.join(
            "const tearDown = function() {}; ",
            "google_exportSymbol('tearDown', tearDown)"));
  }

  public void testArrowFunctionExpressionsAreExported() {
    testSameEs6("var Foo = ()=>{var testA = function() {}}");
    testEs6("var setUp = ()=>{}",
        LINE_JOINER.join(
            "var setUp = ()=>{}; ",
            "google_exportSymbol('setUp', setUp)"));
    testEs6("var testBar = ()=>{}",
        LINE_JOINER.join(
            "var testBar = ()=>{}; ",
            "google_exportSymbol('testBar', testBar)"));
    testEs6("var tearDown = ()=>{}",
        LINE_JOINER.join(
            "var tearDown = ()=>{}; ",
            "google_exportSymbol('tearDown', tearDown)"));
  }

  public void testFunctionAssignmentsAreExported() {
    testSame("Foo = {}; Foo.prototype.bar = function() {};");

    test("Foo = {}; Foo.prototype.setUpPage = function() {};",
         "Foo = {}; Foo.prototype.setUpPage = function() {};"
         + "google_exportProperty(Foo.prototype, 'setUpPage', "
         + "Foo.prototype.setUpPage);");

    test("Foo = {}; Foo.prototype.shouldRunTests = function() {};",
         "Foo = {}; Foo.prototype.shouldRunTests = function() {};"
         + "google_exportProperty(Foo.prototype, 'shouldRunTests', "
         + "Foo.prototype.shouldRunTests);");

    test("Foo = {}; Foo.prototype.testBar = function() {};",
         "Foo = {}; Foo.prototype.testBar = function() {};"
         + "google_exportProperty(Foo.prototype, 'testBar', "
         + "Foo.prototype.testBar);");

    test("Foo = {}; Foo.prototype.testBar = function() "
         + "{ var testBaz = function() {}};",
         "Foo = {}; Foo.prototype.testBar = function() "
         + "{ var testBaz = function() {}};"
         + "google_exportProperty(Foo.prototype, 'testBar', "
         + "Foo.prototype.testBar);");

    test("Foo = {}; Foo.baz.prototype.testBar = function() "
         + "{ var testBaz = function() {}};",
         "Foo = {}; Foo.baz.prototype.testBar = function() "
         + "{ var testBaz = function() {}};"
         + "google_exportProperty(Foo.baz.prototype, 'testBar', "
         + "Foo.baz.prototype.testBar);");
  }

  public void testExportTestSuite() {
    testSame("goog.testing.testSuite({'a': function() {}, 'b': function() {}});");
    test(
        "goog.testing.testSuite({a: function() {}, b: function() {}});",
        "goog.testing.testSuite({'a': function() {}, 'b': function() {}});");
  }

  public void testMemberDefInObjLit() {
    testEs6(
        "goog.testing.testSuite({a() {}, b() {}});",
        "goog.testing.testSuite({'a': function() {}, 'b': function() {}});");
  }
}
