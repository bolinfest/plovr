package org.plovr;

import java.io.File;
import java.io.IOException;

import org.plovr.io.Files;

public class TypeScriptFile extends LocalFileJsInput {

  TypeScriptFile(String name, File source) {
    super(name, source);
  }

  @Override
  public String getCode() {
    try {
      return TypeScriptCompiler.getInstance().compile(
          Files.toString(getSource()), getName());
    } catch (TypeScriptCompilerException e) {
      // TODO(nick): Write a instance of CompilationException
      // for this error type.
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
