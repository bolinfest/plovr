package plovr.io;

import java.nio.charset.Charset;

import com.google.common.base.Charsets;

public final class Settings {

  /** The {@link Charset} to use for the plovr project. */
  public static final Charset CHARSET = Charsets.UTF_8;

  /** Utility class: do not instantiate. */
  private Settings() {}
}
