package com.google.javascript.jscomp;

/**
 * {@link CompilerOptions} has a number of fields that are package private
 * that need to be inspected during unit tests. Therefore, we subclass
 * {@link CompilerOptions} with {@link TestCompilerOptions} in the same
 * package so we can expose the field values via getter methods.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class TestCompilerOptions extends CompilerOptions {

  public boolean getAcceptConstKeyword() {
    return acceptConstKeyword;
  }

  public String getOutputCharset() {
    return outputCharset;
  }
}
