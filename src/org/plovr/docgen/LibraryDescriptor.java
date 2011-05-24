package org.plovr.docgen;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.template.soy.data.SoyMapData;

public class LibraryDescriptor implements Descriptor {

  private final String name;
  private final String description;
  private final List<MethodDescriptor> methods;

  public LibraryDescriptor(
      String name,
      String description,
      List<MethodDescriptor> methods) {
    this.name = name;
    this.description = description;
    this.methods = methods;
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
    Collections.sort(methods, DescriptorComparator.getInstance());
    return new SoyMapData(
        "name", name,
        "description", description,
        "methods", Lists.transform(
            methods,
            MethodDescriptor.TO_SOY_DATA));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String name;
    private String description;
    private List<MethodDescriptor> methods = Lists.newArrayList();

    private Builder() {}

    public LibraryDescriptor build() {
      return new LibraryDescriptor(
          name, description, methods);
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

    public void addMethod(MethodDescriptor method) {
      methods.add(method);
    }
  }
}
