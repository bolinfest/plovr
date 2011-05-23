package org.plovr.docgen;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.template.soy.data.SoyMapData;

public class ClassDescriptor implements Descriptor {

  private final String name;
  @Nullable private final TypeExpression superClass;
  private final String description;
  private final List<MethodDescriptor> instanceMethods;
  private final List<MethodDescriptor> staticMethods;

  private ClassDescriptor(
      String name,
      @Nullable TypeExpression superClass,
      String description,
      List<MethodDescriptor> instanceMethods,
      List<MethodDescriptor> staticMethods) {
    Preconditions.checkNotNull(name, "Must specify a name for ClassDescriptor");
    this.name = name;
    this.superClass = superClass;
    this.description = description;
    this.instanceMethods = instanceMethods;
    this.staticMethods = staticMethods;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String getName() {
    return name;
  }

  public TypeExpression getSuperClass() {
    return superClass;
  }

  @Override
  public String getDescription() {
    return description;
  }

  public List<MethodDescriptor> getInstanceMethods() {
    return instanceMethods;
  }

  public List<MethodDescriptor> getStaticMethods() {
    return staticMethods;
  }

  @Override
  public SoyMapData toSoyData() {
    Collections.sort(instanceMethods, DescriptorComparator.getInstance());
    Collections.sort(staticMethods, DescriptorComparator.getInstance());
    return new SoyMapData(
        "name", name,
        "superClass", superClass != null ? superClass.getDisplayName() : null,
        "description", description,
        "instanceMethods", Lists.transform(
            instanceMethods,
            MethodDescriptor.TO_SOY_DATA),
        "staticMethods", Lists.transform(
            staticMethods,
            MethodDescriptor.TO_SOY_DATA));
  }

  public static class Builder {

    private String name;
    @Nullable private TypeExpression superClass;
    private String description;
    private List<MethodDescriptor> instanceMethods = Lists.newArrayList();
    private List<MethodDescriptor> staticMethods = Lists.newArrayList();

    private Builder() {}

    public ClassDescriptor build() {
      return new ClassDescriptor(
          name,
          superClass,
          description,
          instanceMethods,
          staticMethods);
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public TypeExpression getSuperClass() {
      return superClass;
    }

    public void setSuperClass(TypeExpression superClass) {
      this.superClass = superClass;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public void addInstanceMethod(MethodDescriptor instanceMethod) {
      instanceMethods.add(instanceMethod);
    }

    public void addStaticMethod(MethodDescriptor staticMethod) {
      staticMethods.add(staticMethod);
    }

    public MethodDescriptor getInstanceMethodByName(String name) {
      for (MethodDescriptor method : instanceMethods) {
        if (method.getName().equals(name)) {
          return method;
        }
      }
      return null;
    }
  }
}
