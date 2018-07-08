package org.plovr;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
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
   * This is based on _BASE_REGEX_STRING defined in
   * https://github.com/google/closure-library/blob/master/closure/bin/build/source.py
   * to ensure consistency with closurebuilder.py.
   */
  private static final Pattern GOOG_PROVIDE_OR_REQUIRE =
      Pattern.compile("\\s*goog\\.(module|provide|require)\\(\\s*['\"]([\\w\\.]+)['\"]\\s*\\);?.*");

  private final String name;

  private String code;
  private List<String> provides;
  private List<String> requires;

  AbstractJsInput(String name) {
    this.name = name;
  }

  /**
   * If the underlying file changes, then remove all cached information.
   */
  synchronized void markDirty() {
    provides = null;
    requires = null;
    code = null;
  }

  @Override
  public final String getCode() {
    cacheExpensiveValuesIfNecessary();
    return Preconditions.checkNotNull(code, getName());
  }

  protected abstract String generateCode();

  @Override
  public boolean supportsEtags() {
    return false;
  }

  @Override
  public CodeWithEtag getCodeWithEtag() {
    if (!supportsEtags()) {
      throw new UnsupportedOperationException(
          "This input does not know how to calculate its own ETags.");
    }

    String code = getCode();
    String eTag = calculateEtagFor(code);
    return new CodeWithEtag(code, eTag);
  }

  /**
   * Must return a stable ETag for the supplied code. Note that for some
   * JsInputs, such as Soy and CoffeeScript files, the ETag should be a
   * function of the source code (the .soy or .coffee rather than the JS) as
   * well as the options used to translate the source code to JS. For example,
   * if a user loads a Soy file and an ETag is returned based on the content of
   * the .soy, then the user modifies the Soy options in the plovr config and
   * reloads, then the generated JS as well as the ETag must be different to
   * reflect that change.
   */
  final protected String calculateEtagFor(String code) {
    // Consider creating a stronger eTag.
    // Note that an ETag must be quoted.
    return "\"" + Integer.toHexString(code.hashCode()) + "\"";
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<String> getProvides() {
    cacheExpensiveValuesIfNecessary();
    return Preconditions.checkNotNull(provides, getName());
  }

  @Override
  public List<String> getRequires() {
    cacheExpensiveValuesIfNecessary();
    return Preconditions.checkNotNull(requires, getName());
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

  private synchronized void cacheExpensiveValuesIfNecessary() {
    if (code != null && !hasInputChanged()) {
      return;
    }

    code = generateCode();

    // TODO(nick): It would be nice to replace this code with the closure-compiler
    // built-in deps analysis.
    List<String> provides = Lists.newArrayList();
    List<String> requires = Lists.newArrayList();
    StringLineReader lineReader = new StringLineReader(code);
    String line;
    while ((line = lineReader.readLine()) != null) {
      Matcher matcher = GOOG_PROVIDE_OR_REQUIRE.matcher(line);
      if (matcher.matches()) {
        String type = matcher.group(1);
        String namespace = matcher.group(2);
        boolean isProvide = "provide".equals(type) || "module".equals(type);
        if (isProvide) {
          provides.add(namespace);
        } else {
          requires.add(namespace);
        }
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
