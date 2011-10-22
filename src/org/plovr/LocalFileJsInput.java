package org.plovr;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.plovr.util.Pair;

import com.google.common.collect.Maps;

/**
 * {@link LocalFileJsInput} represents a JavaScript input to the Closure
 * Compiler that can be read from a local file.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public abstract class LocalFileJsInput extends AbstractJsInput {

  private static final Logger logger = Logger.getLogger(
      LocalFileJsInput.class.getName());

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

  static JsInput createForFileWithName(File file, String name,
      SoyFileOptions soyFileOptions) {
    // Cache requests for existing inputs to minimize how often files are
    // re-parsed.
    Pair<File,String> pair = Pair.of(file, name);
    JsInput existingInput = jsInputCache.get(pair);
    if (existingInput != null) {
      return existingInput;
    }

    JsInput newInput;
    String fileName = file.getName();
    if (fileName.endsWith(".soy")) {
      newInput = new SoyFile(name, file, soyFileOptions);
    } else if (fileName.endsWith(".coffee")) {
      newInput = new CoffeeFile(name, file);
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

  /**
   * Gets a normalized path name for the source. This is important because the
   * same file may be both an "input" and a "path" for a config, but it may
   * be referenced via different File names because of how relative paths
   * are resolved.
   * @return
   */
  private String getCanonicalPath() {
    try {
      return source.getCanonicalPath();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot get the canonical path for <" +
          source.getAbsolutePath() + "> what kind of file is this? ", e);
      return source.getAbsolutePath();
    }
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
    return getCanonicalPath().equals(otherInput.getCanonicalPath());
  }

  @Override
  public int hashCode() {
    return getCanonicalPath().hashCode();
  }

}
