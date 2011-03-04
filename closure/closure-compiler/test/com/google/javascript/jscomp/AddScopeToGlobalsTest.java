package com.google.javascript.jscomp;


/**
 * Tests for {@link AddScopeToGlobals}
 *
 */
public class AddScopeToGlobalsTest extends CompilerTestCase {
  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new AddScopeToGlobals(compiler, "$");
  }

  public void testGlobalVar() {
    test("var x", "$.x = void 0");
    test("var x, y", "$.x = void 0, $.y = void 0");
    test("var x = 5, y", "$.x = 5, $.y = void 0");
    test("var x, y = 5, z", "$.x = void 0, $.y = 5, $.z = void 0");

    test("var x, y = z = 5", "$.x = void 0, $.y = $.z = 5");
    test("var x, y = z = a", "$.x = void 0, $.y = $.z = a");
    test("var x, y = z = a = b", "$.x = void 0, $.y = $.z = $.a = b");
  }

  public void testLocalVar() {
    testSame("function x() { var a; }");
    testSame("function x() { var a, b = 5, c; }");
  }

  public void testGlobalInFunc() {
    test("var x = 5; var y = function() { x = 6; }",
         "$.x = 5; $.y = function() { x = 6; }");
  }

  public void testGlobalAliasInFunc() {
    test("var x = 5; var y = function() { var x = 6; }",
         "$.x = 5; $.y = function() { var x = 6; }");
  }

  public void testGlobalNonVar() {
    testSame("x = 5");
    testSame("x = f()");
  }

  public void testGlobalScope() {
    // Debatable. Right now we don't touch these, but perhaps we
    // should? Generally they're an indication of a function being
    // inlined, and other cross-module functions won't rely on this
    // var.
    testSame("if (x) { var y = 3; }");
  }

  public void testSelfReference() {
    test("var x = x || {}", "$.x = $.x || {}");
    test("var x = [x, x, y, 2]", "$.x = [$.x, $.x, y, 2]");
  }
}
