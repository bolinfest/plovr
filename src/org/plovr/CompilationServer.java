package org.plovr;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.google.common.collect.Maps;
import com.sun.net.httpserver.HttpServer;

class CompilationServer implements Runnable {

  private static final Logger logger =
      Logger.getLogger("org.plovr.CompilationServer");

  private final Map<String, Config> configMap;

  private final int port;

  CompilationServer(int port) {
    this.configMap = Maps.newHashMap();
    this.port = port;
  }

  public void registerConfig(Config config) {
    String id = config.getId();
    if (configMap.containsKey(id)) {
      throw new IllegalArgumentException(
          "A config with this id has already been registered: " + id);
    }
    configMap.put(id, config);
  }

  @Override
  public void run() {
    InetSocketAddress addr = new InetSocketAddress(port);
    HttpServer server;
    try {
      server = HttpServer.create(addr, 0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    server.createContext("/compile", new CompileRequestHandler(this));
    server.createContext("/input", new InputFileHandler(this));
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
  }

  public boolean containsConfigWithId(String id) {
    return configMap.containsKey(id);
  }

  public Config getConfigById(String id) {
    return configMap.get(id);
  }
}
