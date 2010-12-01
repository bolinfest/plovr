package plovr.io;

import java.nio.charset.Charset;

import com.google.common.base.Charsets;

public final class Settings {

  // TODO(bolinfest): Ultimately, it will probably be necessary to make these configurable.

  /** The {@link Charset} to use for the plovr project. */
  public static final Charset CHARSET = Charsets.UTF_8;

  /** The {@link Charset} to use for Compiler output. */
  public static final Charset COMPILER_OUTPUT_CHARSET = CHARSET;

  /**
   * The {@link Charset} to use when writing JavaScript content in response to
   * an HTTP request.
   */
  public static final String JS_CONTENT_TYPE = "text/javascript; charset=" +
      COMPILER_OUTPUT_CHARSET.name();

  /** Utility class: do not instantiate. */
  private Settings() {}
}
