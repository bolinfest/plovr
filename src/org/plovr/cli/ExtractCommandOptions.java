package org.plovr.cli;

import java.util.List;

import org.kohsuke.args4j.Argument;

import com.google.common.collect.Lists;

public class ExtractCommandOptions extends AbstractCommandOptions {

  @Argument
  private List<String> arguments = Lists.newLinkedList();

  public List<String> getArguments() {
    return arguments;
  }
}
