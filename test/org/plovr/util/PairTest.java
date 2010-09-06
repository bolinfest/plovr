package org.plovr.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * {@link PairTest} is a unit test for {@link Pair}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class PairTest {

  /** An object whose hashCode() is 100. */
  private final Object obj = new Object() {
    @Override
    public int hashCode() {
      return 100;
    }
  };

  @Test
  public void testGetters() {
    Pair<String, Integer> p = Pair.of("Foo", 42);
    assertEquals("Foo", p.getFirst());
    assertEquals(Integer.valueOf(42), p.getSecond());
  }

  @Test
  public void testGettersWithNull() {
    Pair<String, Integer> p = Pair.of(null, null);
    assertEquals(null, p.getFirst());
    assertEquals(null, p.getSecond());
  }

  @Test
  public void testToString() {
    Pair<String, Integer> p = Pair.of("Foo", 42);
    assertEquals("Foo,42", p.toString());
  }

  @Test
  public void testToStringWithNull() {
    Pair<String, Integer> p = Pair.of(null, null);
    assertEquals("null,null", p.toString());
  }

  @Test
  public void testEquals() {
    // Although the use of the String and Integer constructors is frowned upon,
    // they are used here to ensure that the instances are .equals() to one
    // another but not == to one another.
    Pair<String, Integer> p1 = Pair.of(new String("Foo"), new Integer(42));
    Pair<String, Integer> p2 = Pair.of(new String("Foo"), new Integer(42));
    assertTrue(p1.equals(p1));
    assertTrue(p1.equals(p2));
    assertTrue(p2.equals(p1));

    assertFalse(p1.equals(null));
    assertFalse(p1.equals(Pair.of("Food", 42)));
    assertFalse(p1.equals(Pair.of("Foo", 44)));
  }

  @Test
  public void testEqualsWithNull() {
    Pair<String, Integer> p1 = Pair.of(null, null);
    Pair<String, Integer> p2 = Pair.of(null, null);
    assertTrue(p1.equals(p1));
    assertTrue(p1.equals(p2));
    assertTrue(p2.equals(p1));
  }

  @Test
  public void testHashCode() {
    Pair<Integer, Object> p1 = Pair.of(Integer.valueOf(42), obj);
    assertEquals(31*(31*1 + 42) + 100, p1.hashCode());

    Pair<Object, Integer> p2 = Pair.of(obj, Integer.valueOf(42));
    assertEquals(31*(31*1 + 100) + 42, p2.hashCode());
  }

  @Test
  public void testHashCodeWithNull() {
    Pair<Integer, Object> p1 = Pair.of(null, null);
    assertEquals(31*(31*1 + 0) + 0, p1.hashCode());

    Pair<Integer, Object> p2 = Pair.of(42, null);
    assertEquals(31*(31*1 + 42) + 0, p2.hashCode());

    Pair<Integer, Object> p3 = Pair.of(null, obj);
    assertEquals(31*(31*1 + 0) + 100, p3.hashCode());
  }
}
