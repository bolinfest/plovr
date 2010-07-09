package org.plovr;

import java.io.File;

/**
 * {@link LocalFileJsInput} represents a JavaScript input to the Closure
 * Compiler that can be read from a local file.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
abstract class LocalFileJsInput extends AbstractJsInput {
  
  private final File source;
  
  private long lastModified;

  LocalFileJsInput(String name, File source) {
    super(name);

    // TODO(bolinfest): Use java.nio to listen for updates to the underlying
    // file and invoke markDirty() if it changes. Upon doing so, remove the
    // hasInputChanged() method from the superclass.
    this.source = source;

    this.lastModified = source.lastModified();
  }
  
  static JsInput createForName(String name) {
    return createForFileWithName(new File(name), name);
  }

  static JsInput createForFileWithName(File file, String name) {
    if (file.getName().endsWith(".soy")) {
      return new SoyFile(name, file);
    } else {
      return new JsSourceFile(name, file);        
    }
  }

  final protected File getSource() {
    return source;
  }

  @Override
  protected boolean hasInputChanged() {
    long currentlastModified = source.lastModified();
    if (currentlastModified != lastModified) {
      this.lastModified = currentlastModified;
      return true;
    }
    return false;
  }

  /**
   * If the underlying file changes, then remove all cached information.
   */
  void markDirty() {
    this.provides = null;
    this.requires = null;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof LocalFileJsInput)) {
      return false;
    }
    LocalFileJsInput otherInput = (LocalFileJsInput)obj;
    return source.equals(otherInput.source);
  }

  @Override
  public int hashCode() {
    return source.hashCode();
  }

}
