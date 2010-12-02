package org.plovr.io;

import java.nio.charset.Charset;

import com.google.common.base.Charsets;

public final class Settings {

  /**
   * The {@link Charset} to use when doing I/O operations for which the user
   * is currently unable to specify his own charset.
   */
  public static final Charset CHARSET = Charsets.UTF_8;

  /** Utility class: do not instantiate. */
  private Settings() {}
}
