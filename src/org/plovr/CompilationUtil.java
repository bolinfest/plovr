package org.plovr;

import java.io.IOException;

import com.google.common.base.Preconditions;

public final class CompilationUtil {

  /** Private class; do not instantiate. */
  private CompilationUtil() {}

  /**
   * Tries to return the most recent {@link Compilation} for the specified
   * config. If that is not available, then this will compile the code using the
   * config (but not any referrer information).
   *
   * If the config specifies RAW mode and there is no recent
   * {@link Compilation}, then an exception will be thrown.
   * @throws CompilationException
   */
  public static Compilation getCompilationOrFail(CompilationServer server,
      Config config, boolean recordCompilation) throws IOException, CompilationException {
    Preconditions.checkState(config.getCompilationMode() != CompilationMode.RAW);
    Compilation compilation = server.getLastCompilation(config);
    if (compilation == null) {
      compilation = CompileRequestHandler.compile(config);
      if (recordCompilation) {
        server.recordCompilation(config, compilation);
      }
    }
    return compilation;
  }
}
