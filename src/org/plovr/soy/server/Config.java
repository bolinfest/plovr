package org.plovr.soy.server;

import java.io.File;
import java.util.Map;

public final class Config {

  private final String templateToRender;

  private final int port;

  private final File contentDirectory;

  private final boolean isStatic;

  private final Map<String, ?> compileTimeGlobals;

  public Config(
      String templateToRender,
      int port,
      File contentDirectory,
      boolean isStatic,
      Map<String, ?> compileTimeGlobals) {
    this.templateToRender = templateToRender;
    this.port = port;
    this.contentDirectory = contentDirectory;
    this.isStatic = isStatic;
    this.compileTimeGlobals = compileTimeGlobals;
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
}
