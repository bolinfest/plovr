package org.plovr.soy.server;

import java.io.File;

public final class Config {

  private final int port;

  private final File contentDirectory;

  private final boolean isStatic;

  public Config(int port, File contentDirectory, boolean isStatic) {
    this.port = port;
    this.contentDirectory = contentDirectory;
    this.isStatic = isStatic;
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
}
