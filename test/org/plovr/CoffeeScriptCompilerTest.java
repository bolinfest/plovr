package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.base.Joiner;

/**
 * Unit test for {@link CoffeeScriptCompiler}.
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
        "",
        "example.Point = (function() {",
        "",
        "  function Point(x, y) {",
        "    this.x = x;",
        "    this.y = y;",
        "  }",
        "",
        "  return Point;",
        "",
        "})();",
        ""),
        compiledCoffeeScript);
  }

  @Test
  public void testSimpleCompilationError() {
    CoffeeScriptCompiler compiler = CoffeeScriptCompiler.getInstance();
    try {
      compiler.compile("foo -", "foo.coffee");
      fail("Should throw CoffeeScriptCompilerException");
    } catch (CoffeeScriptCompilerException e) {
      assertEquals("In foo.coffee, Parse error on line 1: Unexpected 'CALL_END'",
          e.getMessage());
    }
  }
}
