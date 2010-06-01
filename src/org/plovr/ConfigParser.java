package org.plovr;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;

/**
 * {@link ConfigParser} extracts a {@link Config} from a JSON config file.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
final class ConfigParser {
  
  /** Utility class; do not instantiate. */
  private ConfigParser() {}

  public static Config parseFile(File file) throws IOException {
    JsonParser jsonParser = new JsonParser();
    JsonElement root = jsonParser.parse(new FileReader(file));
    
    Preconditions.checkNotNull(root);
    Preconditions.checkArgument(root.isJsonObject());
    
    // Get the id for the config.
    JsonObject map = root.getAsJsonObject();
    String id = map.get("id").getAsString();

    // Create the manifest.
    File closureLibraryDirectory = null;
    String closureLibraryValue = maybeGetString(map, "closure-library");
    if (closureLibraryValue != null) {
      closureLibraryDirectory = new File(closureLibraryValue);
    }

    List<String> deps = getAsStringList(map, "deps");
    List<String> inputs = getAsStringList(map, "inputs");    
    JsonElement externsEl = map.get("externs");
    List<File> externs = externsEl.isJsonNull()
        ? null
        : Lists.transform(getAsStringList(map, "externs"), STRING_TO_FILE);
    Manifest manifest = new Manifest(closureLibraryDirectory,
        Lists.transform(deps, STRING_TO_FILE),
        Lists.transform(inputs, STRING_TO_JS_INPUT),
        externs);

    // Extract the Compiler options.
    CompilationLevel level = CompilationLevel.SIMPLE_OPTIMIZATIONS;
    JsonElement optionsEl = map.get("options");
    if (optionsEl.isJsonObject()) {
//      JsonObject options = optionsEl.getAsJsonObject();
//      JsonElement levelEl = options.get("level");
//      if (levelEl.isJsonPrimitive() && levelEl.getAsJsonPrimitive().isString()) {
//        String levelValue = levelEl.getAsString();
//        try {
//          level = CompilationLevel.valueOf(levelValue);
//        } catch (IllegalArgumentException e) {
//          throw new RuntimeException("Not a valid compilation level: " + levelValue);
//        }
//      }
    }

    return new Config(id, manifest, level);
  }

  /**
   * If the key is defined in the map and its corresponding value is a string,
   * return it.
   */
  private static String maybeGetString(JsonObject map, String key) {
    if (!map.has(key)) {
      return null;
    }
    
    JsonElement element = map.get(key);
    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
      return element.getAsString();
    } else {
      return null;
    }
  }

  private static List<String> getAsStringList(JsonObject map, String key) {
    JsonElement value = map.get(key);
    if (value.isJsonArray()) {
      JsonArray array = value.getAsJsonArray();
      List<String> values = Lists.newLinkedList();
      for (JsonElement item : array) {
        values.add(item.getAsString());
      }
      return values;
    } else if (value.isJsonNull()) {
      return ImmutableList.of();
    } else {
      return ImmutableList.of(value.getAsString());
    }
  }

  private static Function<String, File> STRING_TO_FILE = new Function<String, File>() {
    @Override
    public File apply(String s) { return new File(s); }
  };

  private static Function<String, JsInput> STRING_TO_JS_INPUT = new Function<String, JsInput>() {
    @Override
    public JsInput apply(String fileName) {
      return LocalFileJsInput.createForName(fileName);
    }
  };

  /**
   * Takes a config file, performs the compilation, and prints the results to
   * standard out.
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.err.println("Must supply exactly one argument: the config file");
      System.exit(1);
      return;
    }

    Logger compilerLogger = Logger.getLogger("com.google.javascript.jscomp");
    compilerLogger.setLevel(Level.OFF);
    Logger soyFileLogger = Logger.getLogger("org.plovr.SoyFile");
    soyFileLogger.setLevel(Level.OFF);
    
    File configFile = new File(args[0]);
    Config config = ConfigParser.parseFile(configFile);
    CompilerArguments compilerArguments = config.getManifest().getCompilerArguments();
    Compiler compiler = new Compiler();
    Result result = compiler.compile(compilerArguments.getExterns(),
        compilerArguments.getInputs(), config.getCompilerOptions());

    if (result.success) {
      System.out.println(compiler.toSource());
    } else {
      for (JSError warning : result.warnings) {
        System.err.println(warning);
      }
      for (JSError error : result.errors) {
        System.err.println(error);
      }
    }
  }
}
