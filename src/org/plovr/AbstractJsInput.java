package org.plovr;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * {@link AbstractJsInput} provides the default logic for extracting
 * {@code goog.provide()} and {@code goog.require()} information.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
abstract class AbstractJsInput implements JsInput {

  private static final Pattern GOOG_PROVIDE_OR_REQUIRE =
      Pattern.compile("\\s*goog\\.(provide|require)\\(['\"]([\\w\\.]+)['\"]\\);?.*");

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

  protected void processProvidesAndRequires() {
    List<String> provides = Lists.newLinkedList();
    List<String> requires = Lists.newLinkedList();
    for (String line : getCode().split("\n")) {
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

  @Override
  public String toString() {
    return name;
  }

}
