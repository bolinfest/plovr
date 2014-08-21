package org.plovr;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.plovr.util.Pair;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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

  private static final CacheLoader<Key, JsInput> fileLoader =
      new CacheLoader<Key, JsInput>() {
    public JsInput load(Key key) {
      File file = key.file;
      String name = key.name;
      String fileName = file.getName();
      if (fileName.endsWith(".soy")) {
        return new SoyFile(name, file, key.soyFileOptions);
      } else if (fileName.endsWith(".coffee")) {
        return new CoffeeFile(name, file);
      } else if (fileName.endsWith(".ts")) {
        return new TypeScriptFile(name, file);
      } else {
        return new JsSourceFile(name, file);
      }
    }
  };

  private static final LoadingCache<Key, JsInput> jsInputCache =
      CacheBuilder.newBuilder().build(fileLoader);

  LocalFileJsInput(String name, File source) {
    super(name);

    // TODO(bolinfest): Use java.nio to listen for updates to the underlying
    // file and invoke markDirty() if it changes. Upon doing so, remove the
    // hasInputChanged() method from the superclass.
    this.source = source;

    this.lastModified = source.lastModified();
  }

  private static final class Key {
    private final File file;
    private final String name;
    private final SoyFileOptions soyFileOptions;

    Key(File file, String name, SoyFileOptions soyFileOptions) {
      this.file = file;
      this.name = name;
      this.soyFileOptions = soyFileOptions;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(file, name, soyFileOptions);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Key)) {
        return false;
      }
      Key that = (Key) obj;
      return Objects.equal(this.file, that.file) &&
          Objects.equal(this.name, that.name) &&
          Objects.equal(this.soyFileOptions, that.soyFileOptions);
    }
  }

  static JsInput createForFileWithName(File file, String name,
      SoyFileOptions soyFileOptions) {
    try {
      return jsInputCache.get(new Key(file, name, soyFileOptions));
    } catch (ExecutionException e) {
      throw Throwables.propagate(e);
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
