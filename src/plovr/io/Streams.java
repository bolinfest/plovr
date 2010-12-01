package plovr.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public final class Streams {

  /** Utility class: do not instantiate. */
  private Streams() {}

  public static OutputStreamWriter createOutputStreamWriter(OutputStream ostream) {
    return new OutputStreamWriter(ostream, Settings.CHARSET);
  }

  /**
   * As suggested by the {@link FileWriter} Javadoc, because we want to specify
   * the character encoding, use this method to create a {@link Writer} for a
   * {@link File} instead of creating a new {@link FileWriter}.
   *
   * @param outputFile
   * @throws FileNotFoundException
   */
  public static Writer createFileWriter(File outputFile)
  throws FileNotFoundException {
    return createOutputStreamWriter(new FileOutputStream(outputFile));
  }

  /**
   * As suggested by the {@link FileWriter} Javadoc, because we want to specify
   * the character encoding, use this method to create a {@link Writer} for a
   * {@link File} instead of creating a new {@link FileWriter}.
   *
   * @param outputFileName
   * @throws FileNotFoundException
   */
  public static Writer createFileWriter(String outputFileName)
  throws FileNotFoundException {
    return createFileWriter(new File(outputFileName));
  }
}
