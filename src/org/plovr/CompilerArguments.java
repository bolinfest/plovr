package org.plovr;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.javascript.jscomp.JSSourceFile;

/**
 * {@link CompilerArguments} represents the arguments that the Closure Compiler
 * needs to compile JavaScript.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class CompilerArguments {

  private final List<JSSourceFile> externs;
  private final List<JSSourceFile> inputs;

  public CompilerArguments(List<JSSourceFile> externs, List<JSSourceFile> inputs) {
    this.externs = ImmutableList.copyOf(externs);
    this.inputs = ImmutableList.copyOf(inputs);
  }

  public List<JSSourceFile> getExterns() {
    return externs;
  }

  public List<JSSourceFile> getInputs() {
    return inputs;
  }

  @Override
  public String toString() {
    return "Inputs: " + getInputs() + "; Externs: " + getExterns();
  }

}
