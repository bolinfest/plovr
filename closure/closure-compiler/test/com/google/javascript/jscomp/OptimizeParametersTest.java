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
 * Tests for {@link OptimizeParameters}
 *
 */
public final class OptimizeParametersTest extends CompilerTestCase {
  @Override
  public CompilerPass getProcessor(Compiler compiler) {
    return new OptimizeParameters(compiler);
  }

  @Override
  public void setUp() {
    enableNormalize();
  }

  public void testNoRemoval() {
    testSame("function foo(p1) { } foo(1); foo(2)");
    testSame("function foo(p1) { } foo(1,2); foo(3,4)");
  }

  public void testSimpleRemoval() {
    test("function foo(p1) { } foo(); foo()",
         "function foo() {var p1;} foo(); foo()");
    test("function foo(p1) { } foo(1); foo(1)",
         "function foo() {var p1 = 1;} foo(); foo()");
    test("function foo(p1) { } foo(1,2); foo(1,4)",
         "function foo() {var p1 = 1;} foo(2); foo(4)");
  }

  public void testNotAFunction() {
    testSame("var x = 1; x; x = 2");
  }

  public void testRemoveOneOptionalNamedFunction() {
    test("function foo(p1) { } foo()", "function foo() {var p1} foo()");
  }

  public void testDifferentScopes() {
    test("function f(a, b) {} f(1, 2); f(1, 3); " +
        "function h() {function g(a) {} g(4); g(5);} f(1, 2);",
        "function f(b) {var a = 1} f(2); f(3); " +
        "function h() {function g(a) {} g(4); g(5);} f(2);");
  }

  public void testOptimizeOnlyImmutableValues() {
    test("function foo(a) {}; foo(undefined);",
         "function foo() {var a = undefined}; foo()");
    test("function foo(a) {}; foo(null);",
        "function foo() {var a = null}; foo()");
    test("function foo(a) {}; foo(1);",
         "function foo() {var a = 1}; foo()");
    test("function foo(a) {}; foo('abc');",
        "function foo() {var a = 'abc'}; foo()");

    test("var foo = function(a) {}; foo(undefined);",
         "var foo = function() {var a = undefined}; foo()");
    test("var foo = function(a) {}; foo(null);",
         "var foo = function() {var a = null}; foo()");
    test("var foo = function(a) {}; foo(1);",
         "var foo = function() {var a = 1}; foo()");
    test("var foo = function(a) {}; foo('abc');",
         "var foo = function() {var a = 'abc'}; foo()");
  }

  public void testRemoveOneOptionalVarAssignment() {
    test("var foo = function (p1) { }; foo()",
        "var foo = function () {var p1}; foo()");
  }

  public void testDoOptimizeCall() {
    testSame("var foo = function () {}; foo(); foo.call();");
    // TODO(johnlenz): support foo.call
    testSame("var foo = function () {}; foo(); foo.call(this);");
    testSame("var foo = function (a, b) {}; foo(1); foo.call(this, 1);");
    testSame("var foo = function () {}; foo(); foo.call(null);");
    testSame("var foo = function (a, b) {}; foo(1); foo.call(null, 1);");

    testSame("var foo = function () {}; foo.call();");
    // TODO(johnlenz): support foo.call
    testSame("var foo = function () {}; foo.call(this);");
    testSame("var foo = function (a, b) {}; foo.call(this, 1);");
    testSame("var foo = function () {}; foo.call(null);");
    testSame("var foo = function (a, b) {}; foo.call(null, 1);");
  }

  public void testDoOptimizeApply() {
    testSame("var foo = function () {}; foo(); foo.apply();");
    testSame("var foo = function () {}; foo(); foo.apply(this);");
    testSame("var foo = function (a, b) {}; foo(1); foo.apply(this, 1);");
    testSame("var foo = function () {}; foo(); foo.apply(null);");
    testSame("var foo = function (a, b) {}; foo(1); foo.apply(null, []);");

    testSame("var foo = function () {}; foo.apply();");
    testSame("var foo = function () {}; foo.apply(this);");
    testSame("var foo = function (a, b) {}; foo.apply(this, 1);");
    testSame("var foo = function () {}; foo.apply(null);");
    testSame("var foo = function (a, b) {}; foo.apply(null, []);");
  }

  public void testRemoveOneOptionalExpressionAssign() {
    // TODO(johnlenz): There are two definitions of "foo" here, ignore the
    // one that can't be called.
    testSame("var foo; foo = function (p1) { }; foo()");
  }

  public void testRemoveOneOptionalOneRequired() {
    test("function foo(p1, p2) { } foo(1); foo(2)",
        "function foo(p1) {var p2} foo(1); foo(2)");
  }

  public void testRemoveOneOptionalMultipleCalls() {
    test( "function foo(p1, p2) { } foo(1); foo(2); foo()",
        "function foo(p1) {var p2} foo(1); foo(2); foo()");
  }

  public void testRemoveOneOptionalMultiplePossibleDefinition() {
    // TODO(johnlenz): Support multiple valid definitions.
    String src = "var goog = {};" +
        "goog.foo = function (p1, p2) { };" +
        "goog.foo = function (q1, q2) { };" +
        "goog.foo = function (r1, r2) { };" +
        "goog.foo(1); goog.foo(2); goog.foo()";
    testSame(src);
  }

  public void testRemoveTwoOptionalMultiplePossibleDefinition() {
    // TODO(johnlenz): Support multiple valid definitions.
    String src = "var goog = {};" +
        "goog.foo = function (p1, p2, p3, p4) { };" +
        "goog.foo = function (q1, q2, q3, q4) { };" +
        "goog.foo = function (r1, r2, r3, r4) { };" +
        "goog.foo(1,0); goog.foo(2,1); goog.foo()";
    testSame(src);
  }

  public void testConstructorOptArgsNotRemoved() {
    String src =
        "/** @constructor */" +
        "var goog = function(){};" +
        "goog.prototype.foo = function(a,b) {};" +
        "goog.prototype.bar = function(a) {};" +
        "goog.bar.inherits(goog.foo);" +
        "new goog.foo(2,3);" +
        "new goog.foo(1,2);";
    testSame(src);
  }

  public void testMultipleUnknown() {
    // TODO(johnlenz): Support multiple definitions.
    String src = "var goog1 = {};" +
        "goog1.foo = function () { };" +
        "var goog2 = {};" +
        "goog2.foo = function (p1) { };" +
        "var x = getGoog();" +
        "x.foo()";
    testSame(src);
  }

  public void testSingleUnknown() {
    String src =
        "var goog2 = {};" +
        "goog2.foo = function (p1) { };" +
        "var x = getGoog();" +
        "x.foo()";

    String expected =
        "var goog2 = {};" +
        "goog2.foo = function () { var p1 };" +
        "var x = getGoog();" +
        "x.foo()";
    test(src, expected);
  }

  public void testRemoveVarArg() {
    test("function foo(p1, var_args) { } foo(1); foo(2)",
        "function foo(p1) { var var_args } foo(1); foo(2)");
  }

  public void testAliasMethodsDontGetOptimize() {
    String src =
        "var foo = function(a, b) {};" +
        "var goog = {};" +
        "goog.foo = foo;" +
        "goog.prototype.bar = goog.foo;" +
        "new goog().bar(1,2);" +
        "foo(2);";
    testSame(src);
  }

  public void testAliasMethodsDontGetOptimize2() {
    String src =
        "var foo = function(a, b) {};" +
        "var bar = foo;" +
        "foo(1);" +
        "bar(2,3);";
    testSame(src);
  }

  public void testAliasMethodsDontGetOptimize3() {
    String src =
        "var array = {};" +
        "array[0] = function(a, b) {};" +
        "var foo = array[0];" + // foo should be marked as aliased.
        "foo(1);";
    testSame(src);
  }

  public void testAliasMethodsDontGetOptimize4() {
    // Don't change the call to baz as it has been aliased.

    test(
      "function foo(bar) {};" +
      "baz = function(a) {};" +
      "baz(1);" +
      "foo(baz);",
      "function foo() {var bar = baz};" +
      "baz = function(a) {};" +
      "baz(1);" +
      "foo();");
  }

  public void testMethodsDefinedInArraysDontGetOptimized() {
    String src =
        "var array = [true, function (a) {}];" +
        "array[1](1)";
    testSame(src);
  }

  public void testMethodsDefinedInObjectDontGetOptimized() {
    String src =
      "var object = { foo: function bar() {} };" +
      "object.foo(1)";
    testSame(src);
    src =
      "var object = { foo: function bar() {} };" +
      "object['foo'](1)";
    testSame(src);
  }

  public void testRemoveConstantArgument() {
    // Remove only one parameter
    test("function foo(p1, p2) {}; foo(1,2); foo(2,2);",
         "function foo(p1) {var p2 = 2}; foo(1); foo(2)");

    // Remove nothing
    testSame("function foo(p1, p2) {}; foo(1); foo(2,3);");

    // Remove middle parameter
    test("function foo(a,b,c){}; foo(1, 2, 3); foo(1, 2, 4); foo(2, 2, 3)",
         "function foo(a,c){var b=2}; foo(1, 3); foo(1, 4); foo(2, 3)");

    // Number are equals
    test("function foo(a) {}; foo(1); foo(1.0);",
         "function foo() {var a = 1;}; foo(); foo();");

    // A more OO test
    test(
        LINE_JOINER.join(
            "/** @constructor */",
            "function Person() {}; Person.prototype.run = function(a, b) {};",
            "Person.run(1, 'a'); Person.run(2, 'a');"),
        LINE_JOINER.join(
            "/** @constructor */",
            "function Person() {}; Person.prototype.run = function(a) {var b = 'a'};",
            "Person.run(1); Person.run(2);"));
  }

  public void testCanDeleteArgumentsAtAnyPosition() {
    // Argument removed in middle and end
    String src =
        "function foo(a,b,c,d,e) {};" +
        "foo(1,2,3,4,5);" +
        "foo(2,2,4,4,5);";
    String expected =
        "function foo(a,c) {var b=2; var d=4; var e=5;};" +
        "foo(1,3);" +
        "foo(2,4);";
    test(src, expected);
  }

  public void testNoOptimizationForExternsFunctions() {
    testSame("function _foo(x, y, z){}; _foo(1);");
  }

  public void testNoOptimizationForGoogExportSymbol() {
    testSame("goog.exportSymbol('foo', foo);" +
             "function foo(x, y, z){}; foo(1);");
  }

  public void testNoArgumentRemovalNonEqualNodes() {
    testSame("function foo(a){}; foo('bar'); foo('baz');");
    testSame("function foo(a){}; foo(1.0); foo(2.0);");
    testSame("function foo(a){}; foo(true); foo(false);");
    testSame("var a = 1, b = 2; function foo(a){}; foo(a); foo(b);");
    testSame("function foo(a){}; foo(/&/g); foo(/</g);");
  }

  public void testFunctionPassedAsParam() {
    test(
        LINE_JOINER.join(
            "/** @constructor */ function person() {};",
            "person.prototype.run = function(a, b) {};",
            "person.prototype.walk = function() {};",
            "person.prototype.foo = function() { this.run(this.walk, 0.1); };",
            "person.foo();"),
        LINE_JOINER.join(
            "/** @constructor */ function person() {};",
            "person.prototype.run = function(a) { var b = 0.1; };",
            "person.prototype.walk = function() {};",
            "person.prototype.foo = function() { this.run(this.walk); };",
            "person.foo();"));
  }

  public void testCallIsIgnore() {
    testSame("var goog;" +
        "goog.foo = function(a, opt) {};" +
        "var bar = function(){goog.foo.call(this, 1)};" +
        "goog.foo(1);");
  }

  public void testApplyIsIgnore() {
    testSame("var goog;" +
        "goog.foo = function(a, opt) {};" +
        "var bar = function(){goog.foo.apply(this, 1)};" +
        "goog.foo(1);");
  }

  public void testFunctionWithReferenceToArgumentsShouldNotBeOptimize() {
    testSame("function foo(a,b,c) { return arguments.size; };" +
             "foo(1);");
    testSame("var foo = function(a,b,c) { return arguments.size }; foo(1);");
    testSame("var foo = function bar(a,b,c) { return arguments.size }; " +
             "foo(2); bar(2);");
  }

  public void testFunctionWithTwoNames() {
    testSame("var foo = function bar(a,b) {};");
    testSame("var foo = function bar(a,b) {}; foo(1)");
    testSame("var foo = function bar(a,b) {}; bar(1);");
    testSame("var foo = function bar(a,b) {}; foo(1); foo(2)");
    testSame("var foo = function bar(a,b) {}; foo(1); bar(1)");
    testSame("var foo = function bar(a,b) {}; foo(1); bar(2)");
    testSame("var foo = function bar(a,b) {}; foo(1,2); bar(2,1)");
  }

  public void testRecursion() {
    test("var foo = function (a,b) {foo(1, b)}; foo(1, 2)",
         "var foo = function (b) {var a=1; foo(b)}; foo(2)");
  }

  public void testConstantArgumentsToConstructorCanBeOptimized() {
    String src = "function foo(a) {};" +
        "var bar = new foo(1);";
    String expected = "function foo() {var a=1;};" +
        "var bar = new foo();";
    test(src, expected);
  }

  public void testOptionalArgumentsToConstructorCanBeOptimized() {
    String src = "function foo(a) {};" +
        "var bar = new foo();";
    String expected = "function foo() {var a;};" +
        "var bar = new foo();";
    test(src, expected);
  }

  public void testRegexesCanBeInlined() {
    test("function foo(a) {}; foo(/abc/);",
         "function foo() {var a = /abc/}; foo();");
  }

  public void testConstructorUsedAsFunctionCanBeOptimized() {
    String src = "function foo(a) {};" +
        "var bar = new foo(1);" +
        "foo(1);";
    String expected = "function foo() {var a=1;};" +
        "var bar = new foo();" +
        "foo();";
    test(src, expected);
  }

  public void testDoNotOptimizeConstructorWhenArgumentsAreNotEqual() {
    testSame("function Foo(a) {};" +
        "var bar = new Foo(1);" +
        "var baz = new Foo(2);");
  }

  public void testDoNotOptimizeArrayElements() {
    testSame("var array = [function (a, b) {}];");
    testSame("var array = [function f(a, b) {}]");

    testSame("var array = [function (a, b) {}];" +
        "array[0](1, 2);" +
        "array[0](1);");

    testSame("var array = [];" +
        "function foo(a, b) {};" +
        "array[0] = foo;");
  }

  public void testOptimizeThis() {
    String src = "function foo() {" +
        "var bar = function (a, b) {};" +
        "this.bar = function (a, b) {};" +
        "this.bar(3);" +
        "bar(2);}";
    String expected = "function foo() {" +
        "var bar = function () {var b; var a = 2;};" +
        "this.bar = function () {var b; var a = 3;};" +
        "this.bar();" +
        "bar();}";
    test(src, expected);
  }

  public void testDoNotOptimizeWhenArgumentsPassedAsParameter() {
    testSame("function foo(a) {}; foo(arguments)");
    testSame("function foo(a) {}; foo(arguments[0])");

    test("function foo(a, b) {}; foo(arguments, 1)",
         "function foo(a) {var b = 1}; foo(arguments)");

    test("function foo(a, b) {}; foo(arguments)",
         "function foo(a) {var b}; foo(arguments)");
  }

  public void testDoNotOptimizeGoogExportFunctions() {
    testSame("function foo(a, b) {}; foo(); goog.export_function(foo);");
  }

  public void testDoNotOptimizeJSCompiler_renameProperty() {
    testSame("function JSCompiler_renameProperty(a) {return a};" +
             "JSCompiler_renameProperty('a');");
  }

  public void testDoNotOptimizeJSCompiler_ObjectPropertyString() {
    testSame("function JSCompiler_ObjectPropertyString(a, b) {return a[b]};" +
             "JSCompiler_renameProperty(window,'b');");
  }

  public void testMutableValues1() {
    test("function foo(p1) {} foo()",
         "function foo() {var p1} foo()");
    test("function foo(p1) {} foo(1)",
         "function foo() {var p1=1} foo()");
    test("function foo(p1) {} foo([])",
         "function foo() {var p1=[]} foo()");
    test("function foo(p1) {} foo({})",
         "function foo() {var p1={}} foo()");
    test("var x;function foo(p1) {} foo(x)",
         "var x;function foo() {var p1=x} foo()");
    test("var x;function foo(p1) {} foo(x())",
         "var x;function foo() {var p1=x()} foo()");
    test("var x;function foo(p1) {} foo(new x())",
         "var x;function foo() {var p1=new x()} foo()");
    test("var x;function foo(p1) {} foo('' + x)",
         "var x;function foo() {var p1='' + x} foo()");

    testSame("function foo(p1) {} foo(this)");
    testSame("function foo(p1) {} foo(arguments)");
    testSame("function foo(p1) {} foo(function(){})");
    testSame("function foo(p1) {} (function () {var x;foo(x)})()");
  }

  public void testMutableValues2() {
    test("function foo(p1, p2) {} foo(1, 2)",
         "function foo() {var p1=1; var p2 = 2} foo()");
    test("var x; var y; function foo(p1, p2) {} foo(x(), y())",
         "var x; var y; function foo() {var p1=x(); var p2 = y()} foo()");
  }

  public void testMutableValues3() {
    test(
        "var x; var y; var z;" +
        "function foo(p1, p2) {}" +
        "foo(x(), y()); foo(x(),y())",
        "var x; var y; var z;" +
        "function foo() {var p1=x(); var p2=y()}" +
        "foo(); foo()");
  }

  public void testMutableValues4() {
    // Preserve the ordering of side-effects.
    // If z(), can't be moved into the function then z() may change the value
    // of x and y.
    testSame(
        "var x; var y; var z;" +
        "function foo(p1, p2, p3) {}" +
        "foo(x(), y(), z()); foo(x(),y(),3)");

    // If z(), can't be moved into the function then z() may change the value
    // of x and y.
    testSame(
        "var x; var y; var z;" +
        "function foo(p1, p2, p3) {}" +
        "foo(x, y(), z()); foo(x,y(),3)");

    // Mutable object that can not be effect by side-effects are movable,
    // however.
    test(
        "var x; var y; var z;" +
        "function foo(p1, p2, p3) {}" +
        "foo([], y(), z()); foo([],y(),3)",
        "var x; var y; var z;" +
        "function foo(p2, p3) {var p1=[]}" +
        "foo(y(), z()); foo(y(),3)");
  }

  public void testMutableValues5() {
    test(
        "var x; var y; var z;" +
        "function foo(p1, p2) {}" +
        "new foo(new x(), y()); new foo(new x(),y())",
        "var x; var y; var z;" +
        "function foo() {var p1=new x(); var p2=y()}" +
        "new foo(); new foo()");

    test(
        "var x; var y; var z;" +
        "function foo(p1, p2) {}" +
        "new foo(x(), y()); new foo(x(),y())",
        "var x; var y; var z;" +
        "function foo() {var p1=x(); var p2=y()}" +
        "new foo(); new foo()");

    testSame(
        "var x; var y; var z;" +
        "function foo(p1, p2, p3) {}" +
        "new foo(x(), y(), z()); new foo(x(),y(),3)");

    testSame(
        "var x; var y; var z;" +
        "function foo(p1, p2, p3) {}" +
        "new foo(x, y(), z()); new foo(x,y(),3)");

    test(
        "var x; var y; var z;" +
        "function foo(p1, p2, p3) {}" +
        "new foo([], y(), z()); new foo([],y(),3)",
        "var x; var y; var z;" +
        "function foo(p2, p3) {var p1=[]}" +
        "new foo(y(), z()); new foo(y(),3)");
  }

  public void testShadows() {
    testSame("function foo(a) {}" +
             "var x;" +
             "function f() {" +
             "  var x;" +
             "  function g() {" +
             "    foo(x());" +
             "  }" +
             "};" +
             "foo(x())");
  }

  public void testCrash() {
    test(
        "function foo(a) {}" +
        "foo({o:1});" +
        "foo({o:1})",
        "function foo() {var a = {o:1}}" +
        "foo();" +
        "foo()");
  }

  public void testGlobalCatch() {
    testSame("function foo(a) {} try {} catch (e) {foo(e)}");
  }

  public void testNamelessParameter1() {
    test("f(g()); function f(){}",
         "f(); function f(){g()}");
  }

  public void testNamelessParameter2() {
    test("f(g(),h()); function f(){}",
         "f(); function f(){g();h()}");
  }
}
