package org.plovr;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

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
 *
 * Further issues arise from differences between the Sun JDK and the OpenJDK:
 * http://codereview.appspot.com/4901042/
 * This is the reason for all of the reflection in this class.
 */
public class CoffeeScriptCompiler {

  /**
   * @return the singleton instance of the {@link CoffeeScriptCompiler}
   */
  public static CoffeeScriptCompiler getInstance() {
    return Holder.coffeeScriptCompilerInstance;
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
        // Due to reflection, cannot use instanceof.
        // } else if (result instanceof NativeObject) {
        } else if (Holder.nativeObjectClass.isInstance(result)) {
          // API if reflection were not used:
          // NativeObject obj = (NativeObject)result;
          // String message = (String)NativeObject.getProperty(obj, "message");
          // throw new CoffeeScriptCompilerException(message);

          Method getPropertyMethod = result.getClass().getMethod(
              "getProperty", Holder.scriptableClass, String.class);
          String message = (String)getPropertyMethod.invoke(
              null, result, "message");
          throw new CoffeeScriptCompilerException(message);
        } else {
          throw new RuntimeException("Unexpected return type: " +
              result.getClass().getName());
        }
        // If reflection were not used, all of these catch blocks would be
        // replaced with a single catch for a JavaScriptException.
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    } finally {
      Context.exit();
    }
  }

  // What follows is a fairly gross use of reflection that was born out of this
  // code review: http://codereview.appspot.com/4901042/
  // The goal is to make it possible to use plovr with either the Sun JDK or the
  // OpenJDK. If the differences in JDKs cause more code like this, then plovr
  // will no longer support the OpenJDK.

  // Lazy-loading singleton pattern:
  // http://blog.crazybob.org/2007/01/lazy-loading-singletons.html
  private static class Holder {
    private static Class contextClass = getRhinoClassByName("Context");
    private static Class scriptableClass = getRhinoClassByName("Scriptable");
    private static Class nativeObjectClass =
        getRhinoClassByName("NativeObject");
    private static CoffeeScriptCompiler coffeeScriptCompilerInstance =
        new CoffeeScriptCompiler();

    private static Class getRhinoClassByName(String rhinoClassName) {
      String[] names = {
          "sun.org.mozilla.javascript." + rhinoClassName,
          "sun.org.mozilla.javascript.internal." + rhinoClassName
      };
      for (String name : names) {
        try {
          Class clazz = Class.forName(name);
          if (clazz != null) {
            return clazz;
          }
        } catch (ClassNotFoundException e) {
          // OK, try next class name.
        }
      }
      throw new IllegalArgumentException("No Rhino class: " + rhinoClassName);
    }
  }

  private static class Scriptable {
    private final Object scriptableInstance;

    private Scriptable(Object scriptableInstance) {
      this.scriptableInstance = scriptableInstance;
    }

    public void setParentScope(Scriptable parentScope) {
      try {
        Method setParentScopeMethod = scriptableInstance.getClass().
            getMethod("setParentScope", Holder.scriptableClass);
        setParentScopeMethod.invoke(scriptableInstance,
            parentScope.scriptableInstance);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void put(String name, Scriptable start, Object value) {
      try {
        Method setOptimizationLevelMethod = scriptableInstance.getClass().
            getMethod("put", String.class, Holder.scriptableClass,
                Object.class);
        setOptimizationLevelMethod.invoke(scriptableInstance, name,
            start.scriptableInstance, value);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class Context {
    private final Object contextInstance;

    private Context(Object contextInstance) {
      this.contextInstance = contextInstance;
    }

    public static Context enter() {
      try {
        Method enterMethod = Holder.contextClass.getMethod("enter");
        Object contextInstance = enterMethod.invoke(null);
        return new Context(contextInstance);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public static void exit() {
      try {
        Method exitMethod = Holder.contextClass.getMethod("exit");
        exitMethod.invoke(null);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void setOptimizationLevel(int level) {
      try {
        Method setOptimizationLevelMethod = contextInstance.getClass().
            getMethod("setOptimizationLevel", int.class);
        setOptimizationLevelMethod.invoke(contextInstance, level);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public Scriptable initStandardObjects() {
      try {
        Method initStandardObjectsMethod = contextInstance.getClass().getMethod(
            "initStandardObjects");
        Object newScriptableInstance = initStandardObjectsMethod.invoke(
            contextInstance);
        return new Scriptable(newScriptableInstance);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public Scriptable newObject(Scriptable globalScope) {
      try {
        Method newObjectMethod = contextInstance.getClass().getMethod(
            "newObject", Holder.scriptableClass);
        Object newScriptableInstance = newObjectMethod.invoke(
            contextInstance, globalScope.scriptableInstance);
        return new Scriptable(newScriptableInstance);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public Object evaluateString(
        Scriptable scope,
        String source,
        String sourceName,
        int lineno,
        Object securityDomain) {
      try {
        Method evaluateStringMethod = contextInstance.getClass().getMethod(
            "evaluateString", Holder.scriptableClass, String.class,
            String.class, int.class, Object.class);
        return evaluateStringMethod.invoke(contextInstance,
            scope.scriptableInstance, source, sourceName, lineno,
            securityDomain);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
