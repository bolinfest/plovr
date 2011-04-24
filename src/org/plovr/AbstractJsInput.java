package org.plovr;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.LineReader;

/**
 * {@link AbstractJsInput} provides the default logic for extracting
 * {@code goog.provide()} and {@code goog.require()} information.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public abstract class AbstractJsInput implements JsInput {

  /**
   * This should match the _BASE_REGEX_STRING defined in
   * http://code.google.com/p/closure-library/source/browse/trunk/closure/bin/build/source.py
   * to ensure consistency with closurebuilder.py.
   */
  private static final Pattern GOOG_PROVIDE_OR_REQUIRE =
      Pattern.compile("\\s*goog\\.(provide|require)\\(\\s*['\"]([\\w\\.]+)['\"]\\s*\\);?.*");

  private final String name;

  protected List<String> provides;

  protected List<String> requires;

  AbstractJsInput(String name) {
    this.name = name;
  }

  @Override
  public abstract String getCode();

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<String> getProvides() {
    if (provides == null || hasInputChanged()) {
      processProvidesAndRequires();
    }
    return provides;
  }

  @Override
  public List<String> getRequires() {
    if (requires == null || hasInputChanged()) {
      processProvidesAndRequires();
    }
    return requires;
  }

  protected boolean hasInputChanged() {
    return false;
  }

  @Override
  public boolean isSoyFile() {
    return false;
  }

  @Override
  public String getTemplateCode() {
    throw new UnsupportedOperationException("This does not represent a Soy file");
  }

  protected void processProvidesAndRequires() {
    List<String> provides = Lists.newLinkedList();
    List<String> requires = Lists.newLinkedList();
    StringLineReader lineReader = new StringLineReader(getCode());
    String line;
    while ((line = lineReader.readLine()) != null) {
      Matcher matcher = GOOG_PROVIDE_OR_REQUIRE.matcher(line);
      if (matcher.matches()) {
        String type = matcher.group(1);
        String namespace = matcher.group(2);
        (("provide".equals(type)) ? provides : requires).add(namespace);
      }
    }
    this.provides = ImmutableList.copyOf(provides);
    this.requires = ImmutableList.copyOf(requires);
  }

  /**
   * {@link StringLineReader} works like {@link com.google.common.io.LineReader}
   * except that it rethrows an {@link IOException} as a {@link RuntimeException}
   * as reading from a string should never cause one, so declaring a checked
   * exception for readLine() creates an unnecessary burden for the client.
   */
  private static class StringLineReader {

    private final LineReader lineReader;

    StringLineReader(String str) {
      lineReader = new LineReader(new StringReader(str));
    }

    String readLine() {
      try {
        return this.lineReader.readLine();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public String toString() {
    return name;
  }

}
