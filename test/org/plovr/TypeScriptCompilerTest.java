package org.plovr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit test for {@link TypeScriptCompiler}.
 */
public class TypeScriptCompilerTest {

  @Test
  public void testSimpleCompilation() throws TypeScriptCompilerException {
    String compiledTypeScript = TypeScriptCompiler.getInstance().compile(
            "class Animal {\n" +
            "  constructor(public name : string) { }\n" +
            "  move(meters : number) {\n" +
            "    alert(this.name + \" moved \" + meters + \"m.\");\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "class Snake extends Animal {\n" +
            "  constructor(name) { super(name); }\n" +
            "  myMove() {\n" +
            "    super.move(5);\n" +
            "  }\n" +
            "}",
        "testcase.ts");
    assertEquals(
        "goog.provide('Animal');\n" +
        "/**\n" +
        " * @param {string} name\n" +
        " * @constructor\n" +
        " */\n" +
        "Animal = function(name) {\n" +
        "  this.name = name;\n" +
        "}\n" +
        "/**\n" +
        " * @param {number} meters\n" +
        " * @return {void}\n" +
        " */\n" +
        "Animal.prototype.move = function (meters) {\n" +
        "  alert(this.name + \" moved \" + meters + \"m.\");\n" +
        "};\n" +
        "\n" +
        "goog.provide('Snake');\n" +
        "/**\n" +
        " * @param {?} name\n" +
        " * @constructor\n" +
        " * @extends {Animal}\n" +
        " */\n" +
        "Snake = function(name) {\n" +
        "  Animal.call(this, name);\n" +
        "}\n" +
        "goog.inherits(Snake, Animal);\n" +
        "/**\n" +
        " * @return {void}\n" +
        " */\n" +
        "Snake.prototype.myMove = function () {\n" +
        "  Animal.prototype.move.call(this, 5);\n" +
        "};\n" +
        "\n",
        compiledTypeScript);
  }

  @Test
  public void testSimpleCompilationError() {
    TypeScriptCompiler compiler = TypeScriptCompiler.getInstance();
    try {
      compiler.compile("var s = 'foo'; s = 42;", "testcase.ts");
      fail("Should throw TypeScriptCompilerException");
    } catch (TypeScriptCompilerException e) {
      assertEquals("testcase.ts(1,19): Cannot convert 'number' to 'string'\n",
          e.getMessage());
      assertEquals("testcase.ts", e.getSourceFile());
      assertEquals(1, e.getLineNumber());
      assertEquals(19, e.getCharOffset());
      assertEquals("Cannot convert 'number' to 'string'", e.getErrorMessage());
    }
  }
}
