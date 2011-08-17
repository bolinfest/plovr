package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.base.Joiner;

/**
 * {@link CoffeeScriptCompilerTest} is a unit test for {CoffeeScriptCompiler}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class CoffeeScriptCompilerTest {

  @Test
  public void testSimpleCompilation() throws CoffeeScriptCompilerException {
    String compiledCoffeeScript = CoffeeScriptCompiler.getInstance().compile(
        Joiner.on('\n').join(
            "class example.Point",
            "  constructor: (@x, @y) ->"
            ),
        "point.coffee");
    assertEquals(Joiner.on('\n').join(
        "goog.provide('example.Point');",
        "",
        "",
        "",
        "goog.scope(function() {",
        "",
        "/**",
        " * @constructor",
        " */",
        "example.Point = function(x, y) {",
        "  this.x = x;",
        "  this.y = y;",
        "};",
        ";",
        "",
        "}); // close goog.scope()",
        ""),
        compiledCoffeeScript);
  }

  @Test
  public void testSimpleCompilationError() {
    CoffeeScriptCompiler compiler = CoffeeScriptCompiler.getInstance();
    CoffeeScriptCompilerException ex = null;
    try {
      compiler.compile("foo -", "foo.coffee");
      fail("Should throw CoffeeScriptCompilerException");
    } catch (CoffeeScriptCompilerException e) {
      ex = e;
    }
    assertEquals("In foo.coffee, Parse error on line 1: Unexpected 'CALL_END'",
        ex.getMessage());
  }
}
