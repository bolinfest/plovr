package org.plovr;

import java.io.File;
import java.util.Map;

import com.google.common.base.Pair;
import com.google.common.collect.Maps;

/**
 * {@link LocalFileJsInput} represents a JavaScript input to the Closure
 * Compiler that can be read from a local file.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
abstract class LocalFileJsInput extends AbstractJsInput {

  private final File source;

  private long lastModified;

  private static Map<Pair<File,String>, JsInput> jsInputCache = Maps.newHashMap();

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
    // Cache requests for existing inputs to minimize how often files are
    // re-parsed.
    Pair<File,String> pair = Pair.of(file, name);
    JsInput existingInput = jsInputCache.get(pair);
    if (existingInput != null) {
      return existingInput;
    }

    JsInput newInput;
    if (file.getName().endsWith(".soy")) {
      newInput = new SoyFile(name, file);
    } else {
      newInput = new JsSourceFile(name, file);
    }
    Pair<File,String> newPair = Pair.of(file, name);
    jsInputCache.put(newPair, newInput);
    return newInput;
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
