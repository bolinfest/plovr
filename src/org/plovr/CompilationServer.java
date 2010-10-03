package org.plovr;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executors;

import com.google.common.collect.Maps;
import com.google.javascript.jscomp.SourceMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class CompilationServer implements Runnable {

  private final int port;

  // All maps are keyed on a Config id rather than a Config because there could
  // be multiple, different Config objects with the same id because of how query
  // data can be used to redefine a Config for an individual request.

  /**
   * Map of config ids to original Config objects.
   */
  private final Map<String, Config> configs;

  /**
   * Maps a config id to the last Compilation performed for that config.
   */
  private final Map<String, Compilation> compilations;

  public CompilationServer(int port) {
    this.port = port;
    this.configs = Maps.newHashMap();
    this.compilations = Maps.newHashMap();
  }

  public void registerConfig(Config config) {
    String id = config.getId();
    if (configs.containsKey(id)) {
      throw new IllegalArgumentException(
          "A config with this id has already been registered: " + id);
    }
    configs.put(id, config);
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
    server.createContext("/externs", new ExternsHandler(this));
    server.createContext("/input", new InputFileHandler(this));
    server.createContext("/modules", new ModulesHandler(this));
    server.createContext("/size", new SizeHandler(this));
    server.createContext("/sourcemap", new SourceMapHandler(this));
    server.createContext("/view", new ViewFileHandler(this));
    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
  }

  public boolean containsConfigWithId(String id) {
    return configs.containsKey(id);
  }

  public Config getConfigById(String id) {
    return configs.get(id);
  }

  /** Records the last compilation for the config. */
  public void recordCompilation(Config config, Compilation compilation) {
    compilations.put(config.getId(), compilation);
  }

  /** @return the last recorded compilation for the specified config */
  public Compilation getLastCompilation(Config config) {
    return compilations.get(config.getId());
  }

  public SourceMap getSourceMapFor(Config config) {
    Compilation compilation = getLastCompilation(config);
    return compilation.getResult().sourceMap;
  }

  public String getExportsAsExternsFor(Config config) {
    Compilation compilation = getLastCompilation(config);
    return compilation.getResult().externExport;
  }

  /**
   * Returns the server name using an incoming request to this CompilationServer.
   *
   * Unfortunately, HttpExchange does not appear to have a getServerName()
   * method like ServletRequest does, so this method must use a heuristic.
   * If the hostname is not specified in the config file, and it cannot be
   * determined from the referrer, then it is assumed to be localhost.
   * @param exchange
   * @return the server scheme, name, and port, such as "http://localhost:9810/"
   */
  public String getServerForExchange(HttpExchange exchange) {
    URI referrer = HttpUtil.getReferrer(exchange);
    String scheme;
    String host;
    if (referrer == null) {
      scheme = "http";
      host = "localhost";
    } else {
      scheme = referrer.getScheme();
      host = referrer.getHost();
    }
    return String.format("%s://%s:%d/", scheme, host, this.port);
  }
}
