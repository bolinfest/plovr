package org.plovr.cli;

import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import com.google.common.collect.Lists;

public class BuildCommandOptions extends AbstractCommandOptions {

  @Option(name = "--create_source_map",
      usage = "Specifies where the source map for the compilation should be written")
  private String sourceMapPath = null;

  @Argument
  private List<String> arguments = Lists.newLinkedList();

  public String getSourceMapPath() {
    return sourceMapPath;
  }

  public List<String> getArguments() {
    return arguments;
  }
}
