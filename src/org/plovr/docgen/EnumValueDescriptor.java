package org.plovr.docgen;

import com.google.common.base.Function;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.SoyMapData;

public class EnumValueDescriptor implements Descriptor {

  private final String name;
  private final String description;

  public EnumValueDescriptor(String name, String description) {
    this.name = name;
    this.description = description;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public SoyMapData toSoyData() {
    return new SoyMapData(
        "name", name,
        "description", description);
  }

  public static final Function<EnumValueDescriptor, SoyData> TO_SOY_DATA =
      new Function<EnumValueDescriptor, SoyData>() {
    @Override
    public SoyData apply(EnumValueDescriptor param) {
      return param.toSoyData();
    }
  };
}
