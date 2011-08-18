package org.plovr;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import com.google.javascript.jscomp.mozilla.rhino.Context;
import com.google.javascript.jscomp.mozilla.rhino.JavaScriptException;
import com.google.javascript.jscomp.mozilla.rhino.NativeObject;
import com.google.javascript.jscomp.mozilla.rhino.Scriptable;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Adapted from JCoffeeScriptCompiler.java in
 * https://github.com/yeungda/jcoffeescript.
 *
 * This class would not need to exist if
 * com.sun.script.javascript.RhinoScriptEngine behaved reasonably in Java 6:
 * http://stackoverflow.com/questions/7000108/is-it-possible-to-
 *     set-the-optimization-level-for-rhinoscriptengine-in-java-6
 */
public class CoffeeScriptCompiler {

  /**
   * @return the singleton instance of the {@link CoffeeScriptCompiler}
   */
  public static CoffeeScriptCompiler getInstance() {
    return CoffeeScriptCompilerHolder.instance;
  }

  /** Scope that has the CoffeeScript compiler in memory. */
  private final Scriptable globalScope;

  /**
   * Creates a new instance of the CoffeeScriptCompiler by reading and
   * interpreting the CoffeeScript compiler's JavaScript source code. This is
   * done once to create a scope that can be reused by
   * {@link #compile(String, String)}.
   */
  private CoffeeScriptCompiler() {
    try {
      try {
        URL coffeeScriptUrl = Resources.getResource(
            "org/plovr/coffee-script.js");
        String coffeeScriptJs = Resources.toString(coffeeScriptUrl,
            Charsets.UTF_8);
        Context context = Context.enter();
        context.setOptimizationLevel(-1);
        try {
          globalScope = context.initStandardObjects();
          final Object securityDomain = null;
          context.evaluateString(globalScope, coffeeScriptJs,
              "coffee-script.js", 1, securityDomain);
        } finally {
          Context.exit();
        }
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e); // This should never happen
      }
    } catch (IOException e) {
      throw new RuntimeException(e); // This should never happen
    }
  }

  /**
   * Compiles the string of CoffeeScript code to JavaScript.
   * If the CoffeeScript compiler encounters an error, a
   * {@link CoffeeScriptCompilerException} will be thrown.
   * The name of the input is specified so it can be included in error messages.
   * @throws CoffeeScriptCompilerException
   */
  public synchronized String compile(String coffeeScriptSource,
      String sourceName) throws CoffeeScriptCompilerException {
    Context context = Context.enter();
    try {
      Scriptable compileScope = context.newObject(globalScope);
      compileScope.setParentScope(globalScope);
      compileScope.put("coffeeScriptSource", compileScope, coffeeScriptSource);
      try {
        // Build up the options to the CoffeeScript compiler.
        JsonObject opts = new JsonObject();
        opts.addProperty("bare", true);
        opts.addProperty("filename", sourceName);
        JsonObject googleOpts = new JsonObject();
        googleOpts.add("includes", new JsonArray());
        googleOpts.add("provides", new JsonArray());
        opts.add("google", googleOpts);

        // Run the CoffeeScript compiler.
        String js = Joiner.on('\n').join(
            "(function() {",
            "  try {",
            "    return CoffeeScript.compile(coffeeScriptSource, %s);",
            "  } catch (e) {",
            "    return {message: e.message}",
            "  }",
            "})();"
            );
        js = String.format(js, opts.toString());
        final Object securityDomain = null;
        Object result = context.evaluateString(compileScope, js, sourceName, 1,
            securityDomain);

        // Return the appropriate value depending on the type of result.
        if (result == null) {
          throw new RuntimeException("Result from CoffeeScript compiler was " +
          		"null.");
        } else if (result instanceof String) {
          return (String)result;
        } else if (result instanceof NativeObject) {
          NativeObject obj = (NativeObject)result;
          String message = (String)NativeObject.getProperty(obj, "message");
          throw new CoffeeScriptCompilerException(message);
        } else {
          throw new RuntimeException("Unexpected return type: " +
              result.getClass().getName());
        }
      } catch (JavaScriptException e) {
        throw new RuntimeException(e);
      }
    } finally {
      Context.exit();
    }
  }

  // Lazy-loading singleton pattern:
  // http://blog.crazybob.org/2007/01/lazy-loading-singletons.html
  private static class CoffeeScriptCompilerHolder {
    private static CoffeeScriptCompiler instance = new CoffeeScriptCompiler();
  }
}
