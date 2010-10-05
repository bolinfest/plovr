package org.plovr;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;

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
   */
  // TODO(bolinfest): Support RAW mode.
  public static Compilation getCompilationOrFail(CompilationServer server,
      Config config) throws IOException {
    Compilation compilation = server.getLastCompilation(config);
    if (compilation == null) {
      try {
        compilation = CompileRequestHandler.compile(config);
      } catch (MissingProvideException e) {
        throw new RuntimeException(e);
      } catch (CheckedSoySyntaxException e) {
        throw new RuntimeException(e);
      }
      server.recordCompilation(config, compilation);
    }
    return compilation;
  }
}
