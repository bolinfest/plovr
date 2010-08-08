package org.plovr;

import java.io.File;
import java.util.Map;

import org.plovr.ModuleConfig.BadDependencyTreeException;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.DiagnosticGroup;
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
        String path = GsonUtil.stringOrNull(item);
        if (path != null) {
          apply(path, builder);
        }
      }
    }
  }),

  INPUTS("inputs" , new ConfigUpdater() {
    @Override
    public void apply(String input, Config.Builder builder) {
      String resolvedPath = maybeResolvePath(input, builder);
      builder.addInput(new File(resolvedPath), input);
    }

    @Override
    public void apply(JsonArray inputs, Config.Builder builder) {
      for (JsonElement item : inputs) {
        String input = GsonUtil.stringOrNull(item);
        if (input != null) {
          apply(input, builder);
        }
      }
    }
  }),

  EXTERNS("externs", new ConfigUpdater() {
    @Override
    public void apply(String extern, Config.Builder builder) {
      String resolvedPath = maybeResolvePath(extern, builder);
      builder.addExtern(resolvedPath);
    }

    @Override
    public void apply(JsonArray externs, Config.Builder builder) {
      for (JsonElement item : externs) {
        String extern = GsonUtil.stringOrNull(item);
        if (extern != null) {
          apply(extern, builder);
        }
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
        ModuleConfig.Builder moduleConfigBuilder = builder.getModuleConfigBuilder();
        moduleConfigBuilder.setModuleInfo(modules);
      } catch (BadDependencyTreeException e) {
        throw new RuntimeException(e);
      }
    }
  }),

  MODULE_OUTPUT_PATH("module_output_path", new ConfigUpdater() {
    @Override
    public void apply(String outputPath, Config.Builder builder) {
      ModuleConfig.Builder moduleConfigBuilder = builder.getModuleConfigBuilder();
      moduleConfigBuilder.setOutputPath(outputPath);
    }
  }),

  MODULE_PRODUCTION_URI("module_production_uri", new ConfigUpdater() {
    @Override
    public void apply(String productionUri, Config.Builder builder) {
      ModuleConfig.Builder moduleConfigBuilder = builder.getModuleConfigBuilder();
      moduleConfigBuilder.setProductionUri(productionUri);
    }
  }),

  DIAGNOSTIC_GROUPS("checks", new ConfigUpdater() {
    @Override
    public void apply(JsonObject obj, Config.Builder builder) {
      Map<DiagnosticGroup, CheckLevel> groups = Maps.newHashMap();
      for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
        DiagnosticGroup group = DiagnosticGroupUtil.forName(entry.getKey());
        if (group == null) {
          continue;
        }

        String checkLevelString = GsonUtil.stringOrNull(entry.getValue());
        if (checkLevelString == null) {
          continue;
        }
        CheckLevel checkLevel = CheckLevel.valueOf(checkLevelString.toUpperCase());

        groups.put(group, checkLevel);
      }
      builder.setDiagnosticGroups(groups);
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
   * Config files often contain relative paths, so it is important to resolve
   * them against the directory that contains the config file when that is the
   * case.
   *
   * @param path
   * @param builder
   * @return
   */
  private static String maybeResolvePath(String path, Config.Builder builder) {
    return maybeResolvePath(path, builder.getRelativePathBase());
  }

  static String maybeResolvePath(String path, File relativePathBase) {
    // Unfortunately, a File object must be constructed in order to determine
    // whether the path is absolute.
    File file = new File(path);
    if (file.isAbsolute()) {
      return path;
    } else {
      return (new File(relativePathBase, path)).getAbsolutePath();
    }
  }

  static void assertContainsModuleNamePlaceholder(String path) {
    if (path == null || !path.contains("%s")) {
      throw new IllegalArgumentException("Does not contain %s: " + path);
    }
  }
}
