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
  private String etag;
  private List<String> provides;
  private List<String> requires;
  private boolean soyFile;
  private String templateCode;
  long lastModified = 0L;

  public DummyJsInput(String name, String code) {
    this(name, code, null, null, false, null, null);
  }

  public DummyJsInput(String name, String code, String etag) {
    this(name, code, null, null, false, null, etag);
  }

  public DummyJsInput(String name, String code, List<String> provides,
      List<String> requires) {
    this(name, code, provides, requires, false, null, null);
  }

  public DummyJsInput(String name, String code, List<String> provides,
      List<String> requires, boolean soyFile, String templateCode) {
    this(name, code, provides, requires, soyFile, templateCode, null);
  }

  public DummyJsInput(String name, String code, List<String> provides,
      List<String> requires, boolean soyFile, String templateCode, String etag) {
    this.name = name;
    this.code = code;
    this.etag = etag;
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
    return this.etag != null;
  }

  @Override
  public CodeWithEtag getCodeWithEtag() {
    if (this.etag == null) {
      throw new UnsupportedOperationException();
    }

    return new CodeWithEtag(this.getCode(), this.etag);
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }
}
