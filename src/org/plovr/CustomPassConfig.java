package org.plovr;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.javascript.jscomp.CustomPassExecutionTime;

public class CustomPassConfig {
  private String className;
  private CustomPassExecutionTime when;

  public CustomPassConfig() {}

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public CustomPassExecutionTime getWhen() {
    return when;
  }

  public void setWhen(CustomPassExecutionTime when) {
    this.when = when;
  }

  /**
   * Unfortunately, {@link CustomPassConfig} needs a custom deserializer because
   * "class" is a Java keyword, so having a getClass() setter that returns a
   * String is problematic. All plovr options are hyphenated rather than
   * camel cased, so they do not work out of the box with Gson deserialization.
   */
  public static class CustomPassConfigDeserializer implements JsonDeserializer<CustomPassConfig> {
    @Override
    public CustomPassConfig deserialize(
        JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      CustomPassConfig config = new CustomPassConfig();
      config.setClassName(json.getAsJsonObject().get("class-name").getAsString());
      String when = json.getAsJsonObject().get("when").getAsString();
      CustomPassExecutionTime executionTime = CustomPassExecutionTime.valueOf(when);
      config.setWhen(executionTime);
      return config;
    }
  }
}
