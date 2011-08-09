package org.plovr;

import java.io.File;
import java.io.IOException;

import org.plovr.io.Files;

/**
 * {@link CoffeeFile} represents a CoffeeScript source file on disk.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class CoffeeFile extends LocalFileJsInput {

  CoffeeFile(String name, File source) {
    super(name, source);
  }

  /**
   * @throws PlovrCoffeeScriptCompilerException if the CoffeeScript compiler
   *     encounters an error trying to compile the source
   */
  @Override
  public String getCode() throws PlovrCoffeeScriptCompilerException {
    try {
      return CoffeeScriptCompiler.getInstance().compile(
          Files.toString(getSource()), getName());
    } catch (CoffeeScriptCompilerException e) {
      throw new PlovrCoffeeScriptCompilerException(e, this);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
