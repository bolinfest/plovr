package org.plovr;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.CompilerPass;

/**
 * {@link CompilerPassFactory} instantiates {@link CompilerPass}es using
 * reflection, based on the class names that are provided to the "custom-passes"
 * option in a plovr config.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class CompilerPassFactory {

  /** The class to instantiate from this factory. */
  private final Class<?> clazz;

  /**
   * @param clazz a class that implements {@link CompilerPass} that will be
   *        instantiated by this factory
   */
  public CompilerPassFactory(Class<?> clazz) {
    Preconditions.checkNotNull(clazz);
    Preconditions.checkArgument(CompilerPass.class.isAssignableFrom(clazz),
        "This class is not a CompilerPass: %s", clazz);
    this.clazz = clazz;
  }

  /**
   * Creates an instance of the {@link CompilerPass} that this factory is
   * designed to produce. It does so via reflection, using the first constructor
   * it finds that matches one of the following signatures (in priority order):
   * <ul>
   *   <li><code>CompilerPass(AbstractCompiler, Config)</code></li>
   *   <li><code>CompilerPass(AbstractCompiler)</code></li>
   *   <li><code>CompilerPass()</code></li>
   * </ul>
   */
  public CompilerPass createCompilerPass(
      AbstractCompiler compiler, Config config) {
    for (Constructor<?> ctor : clazz.getConstructors()) {
      Class<?>[] params = ctor.getParameterTypes();
      try {
        if (params.length == 2 &&
            AbstractCompiler.class.isAssignableFrom(params[0]) &&
            Config.class.isAssignableFrom(params[1])) {
          return (CompilerPass) ctor.newInstance(compiler, config);
        }
        if (params.length == 1 && AbstractCompiler.class.isAssignableFrom(params[0])) {
          return (CompilerPass) ctor.newInstance(compiler);
        }
        if (params.length == 0) {
          return (CompilerPass) ctor.newInstance();
        }
      } catch (InstantiationException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException("Could not find a valid constructor for: " + clazz);
  }
}
