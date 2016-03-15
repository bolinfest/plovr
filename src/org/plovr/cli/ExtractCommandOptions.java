package org.plovr.cli;

import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import com.google.common.collect.Lists;

public class ExtractCommandOptions extends AbstractCommandOptions {

  @Argument
  private List<String> arguments = Lists.newLinkedList();

  @Option(name="--format",
      usage="Specifies the output message file format")
  private ExtractCommand.Format format = ExtractCommand.Format.XTB;

  public List<String> getArguments() {
    return arguments;
  }

  public ExtractCommand.Format getFormat() {
    return format;
  }
}
