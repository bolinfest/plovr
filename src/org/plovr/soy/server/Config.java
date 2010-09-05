package org.plovr.soy.server;

import java.io.File;

public final class Config {

  private final int port;

  private final File contentDirectory;

  private final boolean useDynamicRecompilation;

  public Config(int port, File contentDirectory, boolean useDynamicRecompilation) {
    this.port = port;
    this.contentDirectory = contentDirectory;
    this.useDynamicRecompilation = useDynamicRecompilation;
  }

  public int getPort() {
    return port;
  }

  public File getContentDirectory() {
    return contentDirectory;
  }

  public boolean useDynamicRecompilation() {
    return useDynamicRecompilation;
  }
}
