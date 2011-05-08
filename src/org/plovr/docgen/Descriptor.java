package org.plovr.docgen;

import com.google.template.soy.data.SoyMapData;

public interface Descriptor {

  public String getName();

  public String getDescription();

  public SoyMapData toSoyData();

}
