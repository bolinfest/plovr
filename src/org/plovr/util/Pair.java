package org.plovr.util;

import com.google.common.base.Equivalence;
import com.google.common.base.Equivalences;

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
    Equivalence<Object> eq = Equivalences.nullAwareEquals();
    return eq.equivalent(getFirst(), pair.getFirst()) &&
        eq.equivalent(getSecond(), pair.getSecond());
  }

  @Override
  public int hashCode() {
    Equivalence<Object> eq = Equivalences.nullAwareEquals();
    int h1 = getFirst() == null ? 0 : eq.hash(getFirst());
    int h2 = getSecond() == null ? 0 : eq.hash(getSecond());
    return h1 + 17 * h2;
  }

  @Override
  public String toString() {
    return first + "," + second;
  }
}
