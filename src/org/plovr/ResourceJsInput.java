package org.plovr;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import org.plovr.io.Settings;

import com.google.common.io.Resources;

/**
 * {@link ResourceJsInput} represents a JavaScript file loaded from a JAR, so
 * it will never change, so its dependencies must only be read once.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class ResourceJsInput extends AbstractJsInput {

  private static final Logger logger = Logger.getLogger(
      "org.plovr.ResourceJsInput");

  private final String pathToResource;

  private CodeWithEtag codeWithEtag;

  ResourceJsInput(String pathToResource) {
    super(pathToResource);
    this.pathToResource = pathToResource;
  }

  @Override
  public CodeWithEtag getCodeWithEtag() {
    // The assignment of codeWithEtag is not thread-safe, though this does not
    // appear to be particularly important.
    if (codeWithEtag == null) {
      // The code for a ResourceJsInput is read once and stored in memory
      // because it must be immutable.
      URL url = Resources.getResource(ResourceReader.class, pathToResource);
      String code;
      try {
        code = Resources.toString(url, Settings.CHARSET);
      } catch (IOException e) {
        logger.severe(e.getMessage());
        throw new RuntimeException(e);
      }
      // The ETag is a function of the code rather than just the name because
      // the content of a ResourceJsInput may change between plovr versions.
      String eTag = calculateEtagFor(code);
      codeWithEtag = new CodeWithEtag(code, eTag);
    }
    return codeWithEtag;
  }

  @Override
  public String getCode() {
    return getCodeWithEtag().code;
  }

  @Override
  public boolean supportsEtags() {
    return true;
  }
}
