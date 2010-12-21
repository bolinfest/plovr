package org.plovr;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.data.SoyMapData;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.tofu.SoyTofu;

/**
 * {@link ConfigOptionDocumentationGenerator} is a class that generates HTML
 * documentation for {@link ConfigOption}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class ConfigOptionDocumentationGenerator {

  private static final SoyTofu TOFU;

  static {
    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(Resources.getResource(ConfigOptionDocumentationGenerator.class,
        "options.soy"));

    // TODO(mbolin): Find a better way to share globals.
    Map<String, String> globals = ImmutableMap.<String, String>builder()
        .put("YEAR", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))
        .put("BOOK_URL", "http://www.amazon.com/gp/product/1449381871?ie=UTF8&tag=bolinfestcom-20&link_code=as3&camp=211189&creative=373489&creativeASIN=1449381871")
        .build();
    builder.setCompileTimeGlobals(globals);

    builder.add(new File("www/__common.soy"));
    SoyFileSet fileSet = builder.build();
    TOFU = fileSet.compileToJavaObj();
  }

  private static class OptionDescriptor {
    final String name;
    boolean acceptsString = false;
    boolean acceptsBoolean = false;
    boolean acceptsNumber = false;
    boolean acceptsArray = false;
    boolean acceptsObject = false;
    boolean supportsQueryDataOverride = false;

    OptionDescriptor(String name) {
      Preconditions.checkNotNull(name);
      this.name = name;
    }

    SoyMapData asSoyMapData() {
      List<String> acceptedValues = Lists.newLinkedList();
      if (acceptsString) acceptedValues.add("string");
      if (acceptsBoolean) acceptedValues.add("boolean");
      if (acceptsNumber) acceptedValues.add("number");
      if (acceptsArray) acceptedValues.add("array");
      if (acceptsObject) acceptedValues.add("object");

      ImmutableMap.Builder<String, Object> builder =
          ImmutableMap.<String, Object>builder()
          .put("name", name)
          .put("acceptedValues", Joiner.on(", ").join(acceptedValues))
          .put("supportsQueryDataOverride", supportsQueryDataOverride);
      return new SoyMapData(builder.build());
    }
  }

  private static List<OptionDescriptor> createDescriptors() {
    ImmutableList.Builder<OptionDescriptor> builder = ImmutableList.builder();
    for (ConfigOption option : ConfigOption.values()) {
      OptionDescriptor descriptor = new OptionDescriptor(option.getName());
      Config.Builder configBuilder = Config.builderForTesting();

      if (testArgumentSupport(option, new JsonPrimitive("foo"), configBuilder)) {
        descriptor.acceptsString = true;
      }
      if (testArgumentSupport(option, new JsonPrimitive(true), configBuilder)) {
        descriptor.acceptsBoolean = true;
      }
      if (testArgumentSupport(option, new JsonPrimitive(42), configBuilder)) {
        descriptor.acceptsNumber = true;
      }
      if (testArgumentSupport(option, new JsonArray(), configBuilder)) {
        descriptor.acceptsArray = true;
      }
      if (testArgumentSupport(option, new JsonObject(), configBuilder)) {
        descriptor.acceptsObject = true;
      }
      if (testQueryParamSupport(option, configBuilder)) {
        descriptor.supportsQueryDataOverride = true;
      }

      builder.add(descriptor);
    }
    return builder.build();
  }

  /**
   * Tests whether a {@link ConfigOption} supports a JSON type as an argument by
   * giving it a dummy value and checking whether it throws an
   * {@link UnsupportedOperationException}.
   */
  private static boolean testArgumentSupport(
      ConfigOption option, JsonElement dummyValue, Config.Builder builder) {
    try {
      option.update(builder, dummyValue);
    } catch (UnsupportedOperationException e) {
      return false;
    } catch (Throwable t) {
      // It is given a dummy value, so it cannot be expected to work.
      return true;
    }
    return true;
  }

  /**
   * Tests whether a {@link ConfigOption} can be overriden by query data.
   */
  private static boolean testQueryParamSupport(ConfigOption option,
      Config.Builder builder) {
    URI uri;
    try {
      uri = new URI(String.format("http://localhost:9810/compile?%s=test",
          option.getName()));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    try {
      QueryData data = QueryData.createFromUri(uri);
      return option.update(builder, data);
    } catch (Throwable t) {
      return true;
    }
  }

  private static String generateHtml() {
    List<String> allNames = Lists.newArrayList();
    for (ConfigOption option : ConfigOption.values()) {
      allNames.add(option.getName());
    }
    Collections.sort(allNames);

    List<OptionDescriptor> descriptors = createDescriptors();
    Function<OptionDescriptor,SoyMapData> f = new Function<OptionDescriptor,SoyMapData>() {
      @Override
      public SoyMapData apply(OptionDescriptor descriptor) {
        return descriptor.asSoyMapData();
      }
    };
    List<SoyMapData> descriptorData = Lists.transform(descriptors, f);
    Map<String, Object> soyData =
        ImmutableMap.<String, Object>builder()
        .put("allNames", allNames)
        .put("descriptors", descriptorData)
        .build();

    final SoyMsgBundle messageBundle = null;
    return TOFU.render("org.plovr.base", soyData, messageBundle);
  }

  public static void main(String[] args) {
    System.out.println(generateHtml());
  }
}
