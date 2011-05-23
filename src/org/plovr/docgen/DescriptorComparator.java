package org.plovr.docgen;

import java.util.Comparator;

class DescriptorComparator implements Comparator<Descriptor> {

  private static final DescriptorComparator instance =
      new DescriptorComparator();

  private DescriptorComparator() {}

  @Override
  public int compare(Descriptor a, Descriptor b) {
    return a.getName().compareTo(b.getName());
  }

  public static DescriptorComparator getInstance() {
    return instance;
  }
}
