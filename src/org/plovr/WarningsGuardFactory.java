package org.plovr;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.javascript.jscomp.WarningsGuard;

/**
 * {@link WarningsGuardFactory} instantiates {@link WarningsGuard}s using
 * reflection, based on the class names that are provided to the
 * "custom-warnings-guards" option in a plovr config.
 *
 * @author mihai@persistent.info (Mihai Parparita)
 */
public class WarningsGuardFactory {

  /** The class to instantiate from this factory. */
  private final Class<?> clazz;

  /**
   * @param clazz a class that implements {@link WarningsGuard} that will be
   *        instantiated by this factory
   */
  public WarningsGuardFactory(Class<?> clazz) {
    Preconditions.checkNotNull(clazz);
    Preconditions.checkArgument(WarningsGuard.class.isAssignableFrom(clazz),
        "This class is not a WarningsGuard: %s", clazz);
    this.clazz = clazz;
  }

  /**
   * Creates an instance of the {@link WarningsGuard} that this factory is
   * designed to produce. It does so via reflection, using the first constructor
   * it finds that matches one of the following signatures (in priority order):
   * <ul>
   *   <li><code>WarningsGuard()</code></li>
   * </ul>
   */
  public WarningsGuard createWarningsGuard() {
    for (Constructor<?> ctor : clazz.getConstructors()) {
      Class<?>[] params = ctor.getParameterTypes();
      try {
        if (params.length == 0) {
          return (WarningsGuard) ctor.newInstance();
        }
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw Throwables.propagate(e);
      }
    }
    throw new RuntimeException("Could not find a valid constructor for: " + clazz);
  }
}
