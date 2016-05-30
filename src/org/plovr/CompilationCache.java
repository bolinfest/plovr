package org.plovr;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Caches results of CompileRequestHandler.
 *
 * Uses an in-memory cache first, and an on-disk cache if explicitly specified.
 */
public class CompilationCache {
  private static final Logger logger = Logger.getLogger(
      CompilationCache.class.getName());

  private final Cache<Object, Entry> cache = CacheBuilder.newBuilder().maximumSize(10).build();

  /**
   * Gets a previously computed output JS if the inputs haven't changed
   * since it was generated.
   *
   * @throws CompilationException because just gathering all the inputs
   *     may lead to a compilation error.
   */
  public String getIfUpToDate(Config config) throws CompilationException {
    Entry entry = cache.getIfPresent(config.getCacheOutputKey());
    if (entry != null && !haveInputsChangedSince(config, entry.generatedAt)) {
      logger.info("JS recompile of " + config.getId() + " fetched from in-memory cache");
      return entry.output;
    }

    File cacheOutputFile = config.getCacheOutputFile();
    if (cacheOutputFile != null && cacheOutputFile.exists() &&
        !haveInputsChangedSince(config, cacheOutputFile.lastModified())) {
      try {
        String cachedJs = Files.toString(cacheOutputFile, config.getOutputCharset());
        logger.info("JS recompile of " + config.getId() + " fetched from file cache");
        return cachedJs;
      } catch (IOException e) {
        logger.log(Level.WARNING, "Error loading JS from disk cache", e);
      }
    }

    return null;
  }

  /**
   * Writes generated JS to the cache.
   */
  public void put(Config config, String output, long generatedAt) {
    cache.put(config.getCacheOutputKey(), new Entry(output, generatedAt));

    File cacheOutputFile = config.getCacheOutputFile();
    if (cacheOutputFile != null) {
      try {
        cacheOutputFile.getParentFile().mkdirs();
        Files.write(output, cacheOutputFile, config.getOutputCharset());
      } catch (IOException e) {
        logger.log(Level.WARNING, "Error writing JS to disk cache", e);
      }
    }
  }

  boolean haveInputsChangedSince(Config config, long timestamp) throws CompilationException {
    if (config.isOutOfDate() || config.hasChangedSince(timestamp)) {
      logger.info("JS recompile of " + config.getId() + " required (config-file newer)");
      return true;
    }

    Manifest manifest = config.getManifest();
    List<JsInput> inputs = manifest.getInputsInCompilationOrder();
    for (JsInput input : inputs) {
      if (input.getLastModified() > timestamp) {
        logger.info("JS recompile of " + config.getId() + " required (found newer file): " + input);
        return true;
      }
    }

    return false;
  }

  private static class Entry {
    private final String output;
    private final long generatedAt;

    Entry(String output, long generatedAt) {
      this.output = output;
      this.generatedAt = generatedAt;
    }
  }
}
