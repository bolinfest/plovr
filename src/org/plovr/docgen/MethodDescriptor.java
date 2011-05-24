package org.plovr.docgen;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.data.SoyMapData;

public class MethodDescriptor implements Descriptor {

  private final String name;
  @Nullable private final String description;
  @Nullable private final TypeExpression returnType;
  private final List<ParamDescriptor> params;
  private final AccessLevel accessLevel;

  private MethodDescriptor(
      String name,
      String description,
      TypeExpression returnType,
      List<ParamDescriptor> params,
      AccessLevel accessLevel) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(params);
    Preconditions.checkNotNull(accessLevel);

    this.name = name;
    this.description = description;
    this.returnType = returnType;
    this.params = params;
    this.accessLevel = accessLevel;
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

  public TypeExpression getReturnType() {
    return returnType;
  }

  public List<ParamDescriptor> getParams() {
    return params;
  }

  public AccessLevel getAccessLevel() {
    return accessLevel;
  }

  @Override
  public SoyMapData toSoyData() {
    return new SoyMapData(
        "name", name,
        "description", description != null ? description : "",
        "returnType", returnType != null ? returnType.getParamName() : null,
        "params", new SoyListData(Lists.transform(params, ParamDescriptor.TO_SOY_DATA)),
        "accessLevel", accessLevel.name());
  }

  public static final Function<MethodDescriptor, SoyData> TO_SOY_DATA =
      new Function<MethodDescriptor, SoyData>() {
    @Override
    public SoyData apply(MethodDescriptor param) {
      return param.toSoyData();
    }
  };

  public static class Builder {

    private String name = null;
    private String description = null;
    private TypeExpression returnType = null;
    private List<ParamDescriptor> params = Lists.newArrayList();
    private AccessLevel accessLevel;

    private Builder() {}

    public MethodDescriptor build() {
      return new MethodDescriptor(
          name,
          description,
          returnType,
          params,
          accessLevel);
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

    public TypeExpression getReturnType() {
      return returnType;
    }

    public void setReturnType(TypeExpression returnType) {
      this.returnType = returnType;
    }

    public void addParam(ParamDescriptor param) {
      params.add(param);
    }

    public AccessLevel getAccessLevel() {
      return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
      this.accessLevel = accessLevel;
    }
  }
}
