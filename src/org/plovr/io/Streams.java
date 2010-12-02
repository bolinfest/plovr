package org.plovr.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.plovr.Config;

import com.google.common.base.Charsets;

public final class Streams {

  /** Utility class: do not instantiate. */
  private Streams() {}

  public static OutputStreamWriter createOutputStreamWriter(
      OutputStream ostream, Config config) {
   return createOutputStreamWriter(ostream, config.getOutputCharset());
  }

  /**
   * As suggested by the {@link FileWriter} Javadoc, because we want to specify
   * the character encoding, use this method to create a {@link Writer} for a
   * {@link File} instead of creating a new {@link FileWriter}.
   *
   * @param outputFile
   * @throws FileNotFoundException
   */
  public static Writer createFileWriter(File outputFile, Config config)
  throws FileNotFoundException {
    return createOutputStreamWriter(new FileOutputStream(outputFile), config);
  }

  /**
   * As suggested by the {@link FileWriter} Javadoc, because we want to specify
   * the character encoding, use this method to create a {@link Writer} for a
   * {@link File} instead of creating a new {@link FileWriter}.
   *
   * @param outputFileName
   * @throws FileNotFoundException
   */
  public static Writer createFileWriter(String outputFileName, Config config)
  throws FileNotFoundException {
    return createFileWriter(new File(outputFileName), config);
  }

  /**
   * Special method to produce a {@link Writer} that will write localized
   * files (i.e. handles non-Latin characters).
   */
  public static Writer createL10nFileWriter(File outputFile)
  throws FileNotFoundException {
    return createOutputStreamWriter(
        new FileOutputStream(outputFile), Charsets.UTF_8);
  }

  /**
   * This is private to force clients of this class to specify a Config
   * whenever possible to force the user's settings to be honored.
   * @return
   */
  private static OutputStreamWriter createOutputStreamWriter(
      OutputStream ostream, Charset charset) {
    return new OutputStreamWriter(ostream, charset);
  }
}
