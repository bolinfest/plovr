package org.plovr;

import javax.script.Bindings;

import com.google.gson.JsonObject;

/**
 * Java wrapper around the JavaScript CoffeeScript compiler.
 * <p>
 * Adapted from JCoffeeScriptCompiler.java in
 * https://github.com/yeungda/jcoffeescript.
 */
public class CoffeeScriptCompiler
    extends AbstractJavaScriptBasedCompiler<CoffeeScriptCompilerException> {

  /**
   * @return the singleton instance of the {@link CoffeeScriptCompiler}
   */
  public static CoffeeScriptCompiler getInstance() {
    return CoffeeScriptCompilerHolder.instance;
  }

  /**
   * Creates a new instance of the CoffeeScriptCompiler by reading and
   * interpreting the CoffeeScript compiler's JavaScript source code. This is
   * done once to create a scope that can be reused by
   * {@link #compile(String, String)}.
   */
  private CoffeeScriptCompiler() {
    super("org/plovr/coffee-script.js");
  }

  @Override
  protected String insertScopeVariablesAndGenerateExecutableJavaScript(
      Bindings compileScope, String sourceCode, String sourceName) {
    compileScope.put("coffeeScriptSource", sourceCode);

    // Build up the options to the CoffeeScript compiler.
    JsonObject opts = new JsonObject();
    opts.addProperty("bare", true);
    opts.addProperty("filename", sourceName);

    String js =
        "(function() {" +
        "  try {" +
        "    return CoffeeScript.compile(coffeeScriptSource, %s);" +
        "  } catch (e) {" +
        "    return {message: e.message}" +
        "  }" +
        "})();";
    return String.format(js, opts.toString());
  }

  @Override
  protected CoffeeScriptCompilerException generateExceptionFromMessage(String message) {
    return new CoffeeScriptCompilerException(message);
  }

  // Lazy-loading singleton pattern:
  // http://blog.crazybob.org/2007/01/lazy-loading-singletons.html
  private static class CoffeeScriptCompilerHolder {
    private static CoffeeScriptCompiler instance = new CoffeeScriptCompiler();
  }
}
