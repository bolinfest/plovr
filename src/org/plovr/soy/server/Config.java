package org.plovr.soy.server;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.plovr.SoyFile;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Injector;

public final class Config {

  private final String templateToRender;

  private final int port;

  private final File contentDirectory;

  private final boolean isStatic;

  private final Map<String, ?> compileTimeGlobals;

  private final boolean isSafeMode;

  private boolean indexPagesAreEnabled;

  private final Injector injector;


  public Config(
      String templateToRender,
      int port,
      File contentDirectory,
      boolean isStatic,
      Map<String, ?> compileTimeGlobals,
      boolean isSafeMode,
      boolean indexPagesAreEnabled,
      String pluginModuleNames) {
    this.templateToRender = templateToRender;
    this.port = port;
    this.contentDirectory = contentDirectory;
    this.isStatic = isStatic;
    this.compileTimeGlobals = compileTimeGlobals;
    this.isSafeMode = isSafeMode;
    this.indexPagesAreEnabled = indexPagesAreEnabled;

    List<String> moduleNames = Lists.newArrayList();
    moduleNames.add("org.plovr.soy.function.PlovrModule");
    for (String plugin : Strings.nullToEmpty(pluginModuleNames).split(",")) {
      plugin = plugin.trim();
      if (!plugin.isEmpty()) {
        moduleNames.add(plugin);
      }
    }
    this.injector = SoyFile.createInjector(moduleNames);
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

  public Injector getInjector() {
    return injector;
  }
}
