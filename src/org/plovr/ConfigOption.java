package org.plovr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.javascript.jscomp.WarningLevel;

public enum ConfigOption {

  ID("id", new ConfigUpdater() {
    @Override
    public void apply(String id, Config.Builder builder) {
      builder.setId(id);
    }
  }),

  PATHS("paths", new ConfigUpdater() {
    @Override
    public void apply(String path, Config.Builder builder) {
      builder.addPath(path);
    }

    @Override
    public void apply(JsonArray paths, Config.Builder builder) {
      for (JsonElement item : paths) {
        String path = getAsString(item);
        builder.addPath(path);
      }
    }
  }),

  INPUTS("inputs" , new ConfigUpdater() {
    @Override
    public void apply(String input, Config.Builder builder) {
      builder.addInput(input);
    }

    @Override
    public void apply(JsonArray inputs, Config.Builder builder) {
      for (JsonElement item : inputs) {
        String path = getAsString(item);
        builder.addInput(path);
      }
    }
  }),

  EXTERNS("externs", new ConfigUpdater() {
    @Override
    public void apply(String extern, Config.Builder builder) {
      builder.addExtern(extern);
    }

    @Override
    public void apply(JsonArray externs, Config.Builder builder) {
      for (JsonElement item : externs) {
        String extern = getAsString(item);
        builder.addInput(extern);
      }
    }
  }),

  CLOSURE_LIBRARY("closure-library", new ConfigUpdater() {
    @Override
    public void apply(String path, Config.Builder builder) {
      builder.setPathToClosureLibrary(path);
    }
  }),

  COMPILATION_MODE("mode", new ConfigUpdater() {
    @Override
    public void apply(String mode, Config.Builder builder) {
      try {
        CompilationMode compilationMode = CompilationMode.valueOf(mode.toUpperCase());
        builder.setCompilationMode(compilationMode);
      } catch (IllegalArgumentException e) {
        // OK
      }
    }

    @Override
    public void update(String mode, Config.Builder builder) {
      apply(mode, builder);
    }
  }),

  WARNING_LEVEL("level", new ConfigUpdater() {
    @Override
    public void apply(String level, Config.Builder builder) {
      try {
        WarningLevel warningLevel = WarningLevel.valueOf(level.toUpperCase());
        builder.setWarningLevel(warningLevel);
      } catch (IllegalArgumentException e) {
        // OK
      }
    }

    @Override
    public void update(String level, Config.Builder builder) {
      apply(level, builder);
    }
  }),

  PRINT_INPUT_DELIMITER("print-input-delimiter", new ConfigUpdater() {
    @Override
    public void apply(boolean printInputDelimiter, Config.Builder builder) {
      builder.setPrintInputDelimiter(printInputDelimiter);
    }

    @Override
    public void update(String printInputDelimiterParam, Config.Builder builder) {
      boolean printInputDelimiter = Boolean.valueOf(printInputDelimiterParam);
      builder.setPrintInputDelimiter(printInputDelimiter);
    }
  }),

  ;

  private static class ConfigUpdater {

    public void apply(String json, Config.Builder builder) {
      throw new UnsupportedOperationException();
    }

    public void apply(boolean value, Config.Builder builder) {
      throw new UnsupportedOperationException();
    }

    public void apply(Number value, Config.Builder builder) {
      throw new UnsupportedOperationException();
    }

    public void apply(JsonArray value, Config.Builder builder) {
      throw new UnsupportedOperationException();
    }

    public void apply(JsonElement json, Config.Builder builder) {
      if (json.isJsonPrimitive()) {
        JsonPrimitive primitive = json.getAsJsonPrimitive();
        if (primitive.isString()) {
          apply(primitive.getAsString(), builder);
        } else if (primitive.isBoolean()) {
          apply(primitive.getAsBoolean(), builder);
        } else if (primitive.isNumber()) {
          apply(primitive.getAsNumber(), builder);
        }
      } else if (json.isJsonArray()) {
        apply(json.getAsJsonArray(), builder);
      }
    }

    /**
     * Only override this method if this option can be overridden using query
     * data.
     * @param queryDataValue
     * @param builder
     */
    public void update(String queryDataValue, Config.Builder builder) {
      // By default, does nothing. Only override if it is safe to update the
      // Config using a query data parameter, which anyone could pass in.
    }
  }

  private final String name;

  private final ConfigUpdater configUpdater;

  ConfigOption(String name, ConfigUpdater configUpdater) {
    this.name = name;
    this.configUpdater = configUpdater;
  }

  public String getName() {
    return name;
  }

  public void update(Config.Builder builder, JsonElement json) {
    if (json == null) {
      return;
    }
    configUpdater.apply(json, builder);
  }

  public void update(Config.Builder builder, QueryData data) {
    String value = data.getParam(name);
    if (value == null) {
      return;
    }
    configUpdater.update(value, builder);
  }

  /**
   *
   * @param element
   * @return the string value of the JsonElement
   * @throws IllegalArgumentException if element does not correspond to a JSON
   *     string primitive
   */
  private static String getAsString(JsonElement element) {
    if (element == null || !element.isJsonPrimitive() ||
        !element.getAsJsonPrimitive().isString()) {
      throw new IllegalArgumentException(element + " is not a JSON string");
    } else {
      return element.getAsJsonPrimitive().getAsString();
    }
  }
}
