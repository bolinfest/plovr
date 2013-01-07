package org.plovr;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import sun.org.mozilla.javascript.internal.Context;
import sun.org.mozilla.javascript.internal.JavaScriptException;
import sun.org.mozilla.javascript.internal.NativeObject;
import sun.org.mozilla.javascript.internal.Scriptable;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * Class that takes a compiler written in JavaScript and uses it to generate
 * JavaScript from another source language, such as CoffeeScript, TypeScript,
 * or Traceur.
 */
abstract class AbstractJavaScriptBasedCompiler<T extends Exception> {

  /** Scope that has the compiler in memory. */
  private final Scriptable globalScope;

  /**
   * @param pathToCompiler should be a path that can be loaded via
   *     {@link Resources#getResource(String)}, which yields the
   *     JavaScript source code for the compiler.
   */
  protected AbstractJavaScriptBasedCompiler(String pathToCompiler) {
    try {
      try {
        URL compilerUrl = Resources.getResource(pathToCompiler);
        String compilerJs = Resources.toString(compilerUrl,
            Charsets.UTF_8);
        Context context = Context.enter();
        context.setOptimizationLevel(-1);
        try {
          globalScope = context.initStandardObjects();
          context.evaluateString(globalScope,
              compilerJs,
              pathToCompiler,
              /* lineno */ 1,
              /* securityDomain */ null);
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
   * Compiles/translates the string of source code code to JavaScript.
   * If the compiler encounters an error, an exception will be thrown.
   * The name of the input is specified so it can be included in error messages.
   */
  public final synchronized String compile(String sourceCode,
      String sourceName) throws T {
    Context context = Context.enter();
    try {
      Scriptable compileScope = context.newObject(globalScope);
      compileScope.setParentScope(globalScope);
      String js = insertScopeVariablesAndGenerateExecutableJavaScript(
          compileScope, sourceCode, sourceName);
      try {
        // Run the compiler.
        final Object securityDomain = null;
        Object result = context.evaluateString(compileScope, js, sourceName, 1,
            securityDomain);

        // Return the appropriate value depending on the type of result.
        if (result == null) {
          String compilerName = this.getClass().getSimpleName();
          throw new RuntimeException(
              "Result from " + compilerName + " compiler was null.");
        } else if (result instanceof String) {
          // This is the expected case: source code was successfully
          // translated to JavaScript.
          return (String)result;
        } else if (result instanceof NativeObject) {
          NativeObject obj = (NativeObject)result;
          String message = NativeObject.getProperty(obj, "message").toString();
          throw generateExceptionFromMessage(message);
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

  protected abstract T generateExceptionFromMessage(String message);

  /**
   * @param compileScope scope to add things to
   * @param sourceCode the input that this compiler is compiling
   * @param sourceName the name of the input being compiled (often useful for
   *     debugging)
   * @return the JavaScript to execute. When executed, it should return a
   *     String containing the compiled JavaScript in the event of a success,
   *     or an object literal with a String property named "message" in the
   *     event of a failure.
   */
  abstract protected String insertScopeVariablesAndGenerateExecutableJavaScript(
      Scriptable compileScope, String sourceCode, String sourceName);
}
