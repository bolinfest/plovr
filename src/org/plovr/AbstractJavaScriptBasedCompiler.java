package org.plovr;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * Class that takes a compiler written in JavaScript and uses it to generate
 * JavaScript from another source language, such as CoffeeScript, TypeScript,
 * or Traceur.
 */
abstract class AbstractJavaScriptBasedCompiler<T extends Exception> {

  /** Scope that has the compiler in memory. */
  private final Bindings globalScope;

  /**
   * @param pathToCompiler should be a path that can be loaded via
   *     {@link Resources#getResource(String)}, which yields the
   *     JavaScript source code for the compiler.
   */
  protected AbstractJavaScriptBasedCompiler(String pathToCompiler) {
    try {
      try {
        URL compilerUrl = Resources.getResource(pathToCompiler);
        String compilerJs = Resources.toString(compilerUrl, Charsets.UTF_8);
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        engine.eval(compilerJs);
        globalScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
      } catch (ScriptException e) {
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

    try {
      ScriptEngineManager factory = new ScriptEngineManager();
      ScriptEngine engine = factory.getEngineByName("JavaScript");
      engine.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);

      Bindings localBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
      String js = insertScopeVariablesAndGenerateExecutableJavaScript(
          localBindings, sourceCode, sourceName);

      Object result = engine.eval(js);
      // Return the appropriate value depending on the type of result.
      if (result == null) {
        String compilerName = this.getClass().getSimpleName();
        throw new RuntimeException(
            "Result from " + compilerName + " compiler was null.");
      } else if (result instanceof String) {
        // This is the expected case: source code was successfully
        // translated to JavaScript.
        return (String)result;
      } else if (result.getClass().getName().equals(
          "sun.org.mozilla.javascript.internal.NativeObject")) {
        String message = extractMessageFromMysteryResultType(result);
        throw generateExceptionFromMessage(message);
      } else {
        throw new RuntimeException("Unexpected return type: " +
            result.getClass().getName());
      }
    } catch (ScriptException e) {
      throw new RuntimeException(e);
    }
  }

  private static String extractMessageFromMysteryResultType(Object result) {
    try {
      Method method = result.getClass().getMethod(
          "getProperty",
          Class.forName("sun.org.mozilla.javascript.internal.Scriptable"),
          String.class);
      String message = method.invoke(null, result, "message").toString();
      return message;
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (SecurityException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
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
      Bindings compileScope, String sourceCode, String sourceName);
}
