package org.plovr.util;

import com.google.common.base.Objects;

public final class Pair<F, S> {

  private final F first;

  private final S second;

  private Pair(F first, S second) {
    this.first = first;
    this.second = second;
  }

  public static <F,S> Pair<F, S> of(F first, S second) {
    return new Pair<F, S>(first, second);
  }

  public F getFirst() {
    return first;
  }

  public S getSecond() {
    return second;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Pair<?, ?>)) {
      return false;
    }
    Pair<?, ?> pair = (Pair<?, ?>) obj;
    return Objects.equal(getFirst(), pair.getFirst()) &&
        Objects.equal(getSecond(), pair.getSecond());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getFirst(), getSecond());
  }

  @Override
  public String toString() {
    return first + "," + second;
  }
}
