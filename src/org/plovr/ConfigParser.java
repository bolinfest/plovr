package org.plovr;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.Result;

/**
 * {@link ConfigParser} extracts a {@link Config} from a JSON config file.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class ConfigParser {

  /** Utility class; do not instantiate. */
  private ConfigParser() {}

  public static Config parseFile(File file) throws IOException {
    String jsonWithoutComments =
        JsonCommentStripper.stripCommentsFromJson(file);
    JsonParser jsonParser = new JsonParser();
    JsonElement root = jsonParser.parse(jsonWithoutComments);

    Preconditions.checkNotNull(root);
    Preconditions.checkArgument(root.isJsonObject());

    Config.Builder builder = Config.builder(file.getParentFile());

    // Get the id for the config.
    JsonObject map = root.getAsJsonObject();
    for (ConfigOption option : ConfigOption.values()) {
      JsonElement element = map.get(option.getName());
      if (element != null) {
        option.update(builder, element);
      }
    }

    return builder.build();
  }

  public static Config update(Config config, QueryData queryData) {
    Config.Builder builder = Config.builder(config);
    for (ConfigOption option : ConfigOption.values()) {
      option.update(builder, queryData);
    }
    return builder.build();
  }

  /**
   * Takes a config file, performs the compilation, and prints the results to
   * standard out.
   * @throws MissingProvideException
   */
  public static void main(String[] args) throws IOException, MissingProvideException {
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
    CompilerArguments compilerArguments =
        config.getManifest().getCompilerArguments();
    Compiler compiler = new Compiler();
    Result result =
        compiler.compile(compilerArguments.getExterns(), compilerArguments
            .getInputs(), config.getCompilerOptions());

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
