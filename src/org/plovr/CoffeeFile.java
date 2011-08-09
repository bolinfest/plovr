package org.plovr;

import com.google.common.base.Joiner;

public class CoffeeFile {

  public static void main(String[] args) throws CoffeeScriptCompilerException {
    String coffeeScript = Joiner.on('\n').join(
        "class example.Person",
        "  constructor: (@first, @last) ->",
        "  getFirst: -> @first",
        "  setFirst: (first) -> @first = first"
        );
    CoffeeScriptCompiler compiler = CoffeeScriptCompiler.getInstance();
    System.out.println(compiler.compile(coffeeScript, "example.coffee"));
  }
}
