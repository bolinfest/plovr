package org.plovr;

import java.io.File;
import java.util.Map;

import org.plovr.ModuleConfig.BadDependencyTreeException;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
      String resolvedPath = maybeResolvePath(path, builder);
      builder.addPath(resolvedPath);
    }

    @Override
    public void apply(JsonArray paths, Config.Builder builder) {
      for (JsonElement item : paths) {
        String path = getAsString(item);
        String resolvedPath = maybeResolvePath(path, builder);
        builder.addPath(resolvedPath);
      }
    }
  }),

  INPUTS("inputs" , new ConfigUpdater() {
    @Override
    public void apply(String input, Config.Builder builder) {
      String resolvedPath = maybeResolvePath(input, builder);
      builder.addInput(resolvedPath);
    }

    @Override
    public void apply(JsonArray inputs, Config.Builder builder) {
      for (JsonElement item : inputs) {
        String path = getAsString(item);
        String resolvedPath = maybeResolvePath(path, builder);
        builder.addInput(resolvedPath);
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

  MODULES("modules", new ConfigUpdater() {
    @Override
    public void apply(JsonObject modules, Config.Builder builder) {
      try {
        ModuleConfig moduleConfig = ModuleConfig.create(modules);

        // Extract the output_path_prefix property, if it is available.
        String outputPathPrefix = "";
        JsonElement outputPathPrefixEl = modules.get("output_path_prefix");
        if (outputPathPrefixEl != null && outputPathPrefixEl.isJsonPrimitive()) {
          JsonPrimitive primitive = outputPathPrefixEl.getAsJsonPrimitive();
          if (primitive.isString()) {
            outputPathPrefix = primitive.getAsString();
          }
        }

        // Set the paths to write the compiled module files to.
        Map<String, File> moduleToOutputPath = Maps.newHashMap();
        for (String moduleName : moduleConfig.getModuleNames()) {
          String partialPath = outputPathPrefix + moduleName + ".js";
          File moduleFile = new File(maybeResolvePath(partialPath, builder));
          moduleToOutputPath.put(moduleName, moduleFile);
        }
        moduleConfig.setModuleToOutputPath(moduleToOutputPath);

        builder.setModuleConfig(moduleConfig);
      } catch (BadDependencyTreeException e) {
        throw new RuntimeException(e);
      }
    }
  })

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

    public void apply(JsonObject value, Config.Builder builder) {
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
      } else if (json.isJsonObject()) {
        apply(json.getAsJsonObject(), builder);
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

  /**
   * Config files often contain relative paths, so it is important to resolve
   * them against the directory that contains the config file when that is the
   * case.
   *
   * @param path
   * @param builder
   * @return
   */
  private static String maybeResolvePath(String path, Config.Builder builder) {
    // Unfortunately, a File object must be constructed in order to determine
    // whether the path is absolute.
    File file = new File(path);
    if (file.isAbsolute()) {
      return path;
    } else {
      return (new File(builder.getRelativePathBase(), path)).getAbsolutePath();
    }
  }
}
