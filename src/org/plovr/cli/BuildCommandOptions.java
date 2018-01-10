package org.plovr.cli;

import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import com.google.common.collect.Lists;

public class BuildCommandOptions extends AbstractCommandOptions {

  @Option(name = "--create_source_map",
      usage = "Specifies the path where the source map for the compilation should be written")
  private String sourceMapPath = null;

  @Option(name = "--language",
      usage = "Specifies the language to use for translations.")
  private String language = null;

  @Option(name = "--print_config",
          usage = "Print all configuration options of the compiler to stderr.")
  private boolean printConfig = false;

  @Argument
  private List<String> arguments = Lists.newLinkedList();

  public String getSourceMapPath() {
    return sourceMapPath;
  }

  public String getLanguage() {
    return language;
  }

  public boolean getPrintConfig() {
    return printConfig;
  }

  public List<String> getArguments() {
    return arguments;
  }
}
