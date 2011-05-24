package org.plovr.cli;

import java.util.List;

import org.kohsuke.args4j.Argument;

import com.google.common.collect.Lists;

public class JsDocCommandOptions extends AbstractCommandOptions {

  // TODO(bolinfest): Create an option to identify the output documentation
  // type, which defaults to "html".

  @Argument
  private List<String> arguments = Lists.newLinkedList();

  public List<String> getArguments() {
    return arguments;
  }
}
