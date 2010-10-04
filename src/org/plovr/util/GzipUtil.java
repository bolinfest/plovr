package org.plovr.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

public final class GzipUtil {

  /** Utility class; do not instantiate. */
  private GzipUtil() {}

  // TODO(bolinfest): Create unit test or add documentation about why this does
  // not match the values returned by using gzip from the command line.
  public static int getGzipSize(String str) throws IOException {
    // Setup the streams.
    ByteArrayInputStream input = new ByteArrayInputStream(str.getBytes(Charsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    GZIPOutputStream gzip = new GZIPOutputStream(output);

    // Write to the output to trigger the gzipping.
    ByteStreams.copy(input, gzip);

    // Flush the buffers and return its size.
    input.close();
    gzip.finish();
    gzip.close();
    output.close();
    return output.toByteArray().length;
  }
}
