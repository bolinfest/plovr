package org.plovr.cli;

import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import com.google.common.collect.Lists;

public class BuildCommandOptions extends AbstractCommandOptions {

  @Option(name = "--create_source_map",
      usage = "Specifies where the source map for the compilation should be written")
  private String sourceMapPath = null;

  @Option(name = "--goog-debug",
      usage = "Define goog.DEBUG is true")
  private boolean debug = false;

  @Argument
  private List<String> arguments = Lists.newLinkedList();

  public String getSourceMapPath() {
    return sourceMapPath;
  }

  public boolean getDebug() {
    return debug;
  }

  public List<String> getArguments() {
    return arguments;
  }
}
