package org.plovr;

import java.util.Comparator;

public class JsInputComparator implements Comparator<JsInput> {

  public static final JsInputComparator SINGLETON = new JsInputComparator();

  private JsInputComparator() {}

  @Override
  public int compare(JsInput a, JsInput b) {
    return a.getName().compareTo(b.getName());
  }
}
