package org.plovr;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import javax.annotation.Nullable;
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
      String errorMessage;

      // Return the appropriate value depending on the type of result.
      if (result == null) {
        String compilerName = this.getClass().getSimpleName();
        throw new RuntimeException(
            "Result from " + compilerName + " compiler was null.");
      } else if (result instanceof String) {
        // This is the expected case: source code was successfully
        // translated to JavaScript.
        return (String)result;
      } else if ((errorMessage = extractMessageFromMysteryResultType(result)) != null) {
        throw generateExceptionFromMessage(errorMessage);
      } else {
        throw new RuntimeException("Unexpected return type: " +
            result.getClass().getName());
      }
    } catch (ScriptException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  private static String extractMessageFromMysteryResultType(Object result) {
    String message;

    // Oracle JDK 7.
    message = tryExtractMessageFromMysteryResultType(result,
        "sun.org.mozilla.javascript.internal.NativeObject",
        "sun.org.mozilla.javascript.internal.Scriptable");
    if (message != null) {
      return message;
    }

    // OpenJDK 7.
    message = tryExtractMessageFromMysteryResultType(result,
        "sun.org.mozilla.javascript.NativeObject",
        "sun.org.mozilla.javascript.Scriptable");
    if (message != null) {
      return message;
    }

    return null;
  }

  @Nullable
  private static String tryExtractMessageFromMysteryResultType(
      Object result,
      String resultClassName,
      String scriptableClassName) {
    if (!result.getClass().getName().equals(resultClassName)) {
      return null;
    }

    try {
      Method method = result.getClass().getMethod(
          "getProperty",
          Class.forName(scriptableClassName),
          String.class);
      return method.invoke(null, result, "message").toString();
    } catch (NoSuchMethodException e) {
      return null;
    } catch (SecurityException e) {
      return null;
    } catch (ClassNotFoundException e) {
      return null;
    } catch (IllegalAccessException e) {
      return null;
    } catch (IllegalArgumentException e) {
      return null;
    } catch (InvocationTargetException e) {
      return null;
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
