package org.plovr;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * {@link DummyJsInput} is a dummy implementation of JsInput that can
 * be used in tests that want to easily manipulate all of the aspects
 * of a JsInput.
 *
 * @author imirkin@alum.mit.edu (Ilia Mirkin)
 */
public class DummyJsInput implements JsInput {

  private String name;
  private String code;
  private List<String> provides;
  private List<String> requires;
  private boolean soyFile;
  private String templateCode;

  public DummyJsInput(String name, String code, List<String> provides,
      List<String> requires) {
    this(name, code, provides, requires, false, null);
  }

  public DummyJsInput(String name, String code, List<String> provides,
      List<String> requires, boolean soyFile, String templateCode) {
    this.name = name;
    this.code = code;
    if (provides != null) {
      this.provides = ImmutableList.copyOf(provides);
    } else {
      this.provides = ImmutableList.of();
    }
    if (requires != null) {
      this.requires = ImmutableList.copyOf(requires);
    } else {
      this.requires = ImmutableList.of();
    }
    this.soyFile = soyFile;
    this.templateCode = templateCode;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getCode() {
    return code;
  }

  @Override
  public List<String> getProvides() {
    return provides;
  }

  @Override
  public List<String> getRequires() {
    return requires;
  }

  @Override
  public boolean isSoyFile() {
    return soyFile;
  }

  @Override
  public String getTemplateCode() throws UnsupportedOperationException {
    if (!soyFile) {
      throw new UnsupportedOperationException(
        "This does not represent a Soy file");
    }
    return templateCode;
  }

  @Override
  public boolean supportsEtags() {
    return false;
  }

  @Override
  public CodeWithEtag getCodeWithEtag() {
    throw new UnsupportedOperationException();
  }
}
