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
   * designed to produce. If the {@link CompilerPass} has a no-arg constructor,
   * then that will be used. Failing that, a constructor that takes a single
   * {@link AbstractCompiler} parameter will be used.
   */
  public CompilerPass createCompilerPass(AbstractCompiler compiler) {
    for (Constructor<?> ctor : clazz.getConstructors()) {
      Class<?>[] params = ctor.getParameterTypes();
      try {
        if (params.length == 0) {
          return (CompilerPass) ctor.newInstance();
        }
        if (params.length == 1 && AbstractCompiler.class.isAssignableFrom(params[0])) {
          return (CompilerPass) ctor.newInstance(compiler);
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
