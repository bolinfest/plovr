package org.plovr.docgen;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.template.soy.data.SoyMapData;

public class EnumDescriptor implements Descriptor {

  private final String name;
  private final String description;
  private final List<EnumValueDescriptor> values;

  private EnumDescriptor(
      String name,
      String description,
      List<EnumValueDescriptor> values) {
    this.name = name;
    this.description = description;
    this.values = values;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public SoyMapData toSoyData() {
    return new SoyMapData(
        "name", name,
        "description", description,
        "values", Lists.transform(values, EnumValueDescriptor.TO_SOY_DATA));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private String description;
    private List<EnumValueDescriptor> values = Lists.newArrayList();

    private Builder() {}

    public EnumDescriptor build() {
      return new EnumDescriptor(
          name, description, values);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public void addValue(String name, String description) {
      values.add(new EnumValueDescriptor(name, description));
    }
  }
}
