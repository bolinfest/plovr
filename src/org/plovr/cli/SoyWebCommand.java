package org.plovr.cli;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import org.plovr.soy.server.Config;
import org.plovr.soy.server.Server;
import org.plovr.util.SoyDataUtil;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.template.soy.data.SoyData;

public class SoyWebCommand extends AbstractCommandRunner<SoyWebCommandOptions> {

  @Override
  SoyWebCommandOptions createOptions() {
    return new SoyWebCommandOptions();
  }

  @Override
  int runCommandWithOptions(SoyWebCommandOptions options) throws IOException {
    String pathToGlobals = options.getCompileTimeGlobalsFile();
    Map<String, ?> globals;
    if (pathToGlobals == null) {
      globals = ImmutableMap.of();
    } else {
      File globalsFile = new File(pathToGlobals);
      if (!globalsFile.exists()) {
        throw new RuntimeException("Could not find file: " + pathToGlobals);
      }
      JsonParser parser = new JsonParser();
      JsonElement root = parser.parse(new FileReader(globalsFile));
      if (!root.isJsonObject()) {
        throw new RuntimeException("Root of globals file must be a map");
      }
      JsonObject json = root.getAsJsonObject();
      ImmutableMap.Builder<String, SoyData> builder = ImmutableMap.builder();
      for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
        JsonElement el = entry.getValue();
        builder.put(entry.getKey(), SoyDataUtil.jsonToSoyData(el));
      }
      globals = builder.build();
    }

    Config config = new Config(
        options.getPort(),
        new File(options.getDir()),
        options.isStatic(),
        globals);
    Server server = new Server(config);
    server.run();
    return STATUS_NO_EXIT;
  }

  @Override
  String getUsageIntro() {
    return "Specify a directory that contains Soy files";
  }
}
