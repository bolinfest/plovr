package com.google.javascript.jscomp;


/**
 * Tests for {@link AnonymizeNamedFunctions}
 *
 */
public class AnonymizeNamedFunctionsTest extends CompilerTestCase {
  public AnonymizeNamedFunctionsTest() {
    this.enableNormalize();
  }

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new AnonymizeNamedFunctions(compiler);
  }

  public void testGlobalScope() {
    test("function f(){}", "var f = function(){}");
  }

  public void testLocalScope1() {
    test("function f(){ function x(){} x() }",
         "var f = function(){ function x(){} x() }");
  }

  public void testLocalScope2() {
    test("function f(){ function x(){} return x }",
         "var f = function (){ function x(){} return x }");
  }

  public void testVarNotImmediatelyBelowScriptOrBlock1() {
    testSame("if (x) var f = function(){}");
  }

  public void testVarNotImmediatelyBelowScriptOrBlock2() {
    testSame("var x = 1;" +
             "if (x == 1) {" +
             "  var f = function () { alert('b')}" +
             "} else {" +
             "  f = function() { alert('c')}" +
             "}" +
             "f();");
  }

  public void testVarNotImmediatelyBelowScriptOrBlock3() {
    testSame("var x = 1; if (x) {var f = function(){return x}; f(); x--;}");
  }

  public void testMultipleVar() {
    test("function f(){} var g = f", "var f = function(){}; var g = f");
  }

  public void testMultipleVar2() {
    test("function f(){}var g = f;function h(){}",
         "var f = function(){}; var g = f; var h = function(){}");
  }

  public void testBothScopes() {
    test("function x() { function y(){} }",
         "var x = function() { function y(){} }");
  }

  public void testLocalScopeOnly1() {
    test("if (x) var f = function(){ function g(){} }",
         "if (x) var f = function(){ function g(){} }");
  }

  public void testReturn() {
    test("function f(x){return 2*x} var g = f(2)",
         "var f = function(x){return 2*x}; var g = f(2);");
  }

  public void testAlert() {
    test("var x = 1; function f(){alert(x)}",
         "var x = 1; var f = function(){alert(x)}");
  }

  public void testRecursiveInternal1() {
    testSame("var f = function foo() { foo() }");
  }

  public void testRecursiveInternal2() {
    testSame("var f = function foo() { function g(){foo()} g() }");
  }

  public void testRecursiveExternal1() {
    test("function f() { f() }",
         "var f = function() { f() }");
  }

  public void testRecursiveExternal2() {
    test("function f() { function g(){f()} g() }",
         "var f = function() { function g(){f()} g() }");
  }

  public void testConstantFunction1() {
    test("function FOO(){}FOO()",
         "var FOO = function(){};FOO()");
  }

  public void testInnerFunction1() {
    // We only uncollapse at global scope.
    test("function f() { " +
         "  function y() { return 4; } var x = 3; return x + y();" +
         "}",
         "var f = function() { " +
         "  function y() { return 4; } var x = 3; return x + y();" +
         "}"
      );
  }
}
