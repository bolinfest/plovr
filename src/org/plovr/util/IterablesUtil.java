package org.plovr.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.collect.Iterables;

/**
 * {@link IterablesUtil} has methods that are not available in
 * {@link Iterables}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class IterablesUtil {

  /** Utility class; do not instantiate. */
  private IterablesUtil() {}

  /**
   * @throws NoSuchElementException if the {@link Iterator} returned by
   *     iterable is empty.
   */
  public static <T> T getFirst(Iterable<T> iterable) {
    Iterator<T> iter = iterable.iterator();
    return iter.next();
  }
}
