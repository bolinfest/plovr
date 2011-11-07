package org.plovr.soy.server;

import java.io.File;
import java.util.Map;

public final class Config {

  private final String templateToRender;

  private final int port;

  private final File contentDirectory;

  private final boolean isStatic;

  private final Map<String, ?> compileTimeGlobals;

  private final boolean isSafeMode;

  private boolean indexPagesAreEnabled;

  public Config(
      String templateToRender,
      int port,
      File contentDirectory,
      boolean isStatic,
      Map<String, ?> compileTimeGlobals,
      boolean isSafeMode,
      boolean indexPagesAreEnabled) {
    this.templateToRender = templateToRender;
    this.port = port;
    this.contentDirectory = contentDirectory;
    this.isStatic = isStatic;
    this.compileTimeGlobals = compileTimeGlobals;
    this.isSafeMode = isSafeMode;
    this.indexPagesAreEnabled = indexPagesAreEnabled;
  }

  public String getTemplateToRender() {
    return templateToRender;
  }

  public int getPort() {
    return port;
  }

  public File getContentDirectory() {
    return contentDirectory;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public Map<String, ?> getCompileTimeGlobals() {
    return compileTimeGlobals;
  }

  public boolean isSafeMode() {
    return isSafeMode;
  }

  public boolean indexPagesAreEnabled() {
    return indexPagesAreEnabled;
  }
}
