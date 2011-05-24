package org.plovr.docgen;

import com.google.common.base.Function;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.SoyMapData;


public class ParamDescriptor implements Descriptor {

  private String name;
  private String description;
  private TypeExpression typeExpression;

  private ParamDescriptor(String name, String description, TypeExpression typeExpression) {
    this.name = name;
    this.description = description;
    this.typeExpression = typeExpression;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public TypeExpression getTypeExpression() {
    return typeExpression;
  }

  @Override
  public SoyMapData toSoyData() {
    return new SoyMapData(
        "name", name,
        "description", description,
        "type", typeExpression.getParamName());
  }

  public static final Function<ParamDescriptor, SoyData> TO_SOY_DATA =
      new Function<ParamDescriptor, SoyData>() {
    @Override
    public SoyData apply(ParamDescriptor param) {
      return param.toSoyData();
    }
  };

  public static class Builder {

    private String name;
    private String description;
    private TypeExpression typeExpression;

    private Builder() {}

    public ParamDescriptor build() {
      return new ParamDescriptor(
          name,
          description,
          typeExpression);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public TypeExpression getTypeExpression() {
      return typeExpression;
    }

    public void setTypeExpression(TypeExpression typeExpression) {
      this.typeExpression = typeExpression;
    }
  }
}
