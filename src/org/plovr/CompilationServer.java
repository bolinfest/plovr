package org.plovr;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.plovr.cli.HttpServerUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.javascript.jscomp.SourceMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public final class CompilationServer implements Runnable {

  private final String listenAddress;

  private int port;

  // All maps are keyed on a Config id rather than a Config because there could
  // be multiple, different Config objects with the same id because of how query
  // data can be used to redefine a Config for an individual request.

  /**
   * Map of config ids to original Config objects.
   */
  private final ConcurrentMap<String, Config> configs;

  /**
   * Maps a config id to the last Compilation performed for that config.
   */
  private final ConcurrentMap<String, Compilation> compilations;

  public CompilationServer(String listenAddress, int port) {
    this.listenAddress = listenAddress;
    this.port = port;
    this.configs = Maps.newConcurrentMap();
    this.compilations = Maps.newConcurrentMap();
  }

  public void registerConfig(Config config) {
    String id = config.getId();
    if (configs.containsKey(id)) {
      throw new IllegalArgumentException(
          "A config with this id has already been registered: " + id);
    }
    configs.put(id, config);
  }

  private void reload(Config config) throws IOException {
    Preconditions.checkNotNull(config.getConfigFile(),
        "Can't reload a config without a file");

    System.err.println("Reloading " + config.getConfigFile().getPath());
    Config newConfig = ConfigParser.parseFile(config.getConfigFile());
    replaceConfig(config, newConfig);
  }

  private void replaceConfig(Config oldConfig, Config newConfig) {
    // Remove compilations from the old configuration
    compilations.remove(oldConfig.getId());
    configs.put(newConfig.getId(), newConfig);

    if (!oldConfig.getId().equals(newConfig.getId())) {
      // Remove the old config if the ID has changed
      configs.remove(oldConfig.getId());
    }
  }

  private void detectChangesToConfigs() {
    // Create a copy of the values to ensure no ConcurrentModificationExceptions
    // are thrown
    List<Config> values = ImmutableList.copyOf(configs.values());

    for (Config config : values) {
      if (config.isOutOfDate()) {
        try {
          reload(config);
        } catch (IOException e) {
          throw new RuntimeException("Error reloading config: "
              + config.getId());
        }
      }
    }
  }

  @Override
  public void run() {
    InetSocketAddress addr = new InetSocketAddress(listenAddress, port);
    HttpServer server;
    try {
      server = HttpServer.create(addr, 0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    InetSocketAddress serverAddress = server.getAddress();
    // This will be different than the port passed to the constructor if the
    // value passed to the constructor was 0.
    port = serverAddress.getPort();

    // Feature request http://code.google.com/p/plovr/issues/detail?id=23
    HttpServerUtil.printListeningStatus(server);

    // Register all of the handlers.
    for (Handler handler : Handler.values()) {
      final HttpHandler delegate = handler.createHandlerForCompilationServer(this);

      server.createContext(handler.getContext(), new HttpHandler() {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
          // Reload any configuration files if necessary
          detectChangesToConfigs();

          delegate.handle(exchange);
        }
      });
    }

    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
  }

  public boolean containsConfigWithId(String id) {
    return configs.containsKey(id);
  }

  public Config getConfigById(String id) {
    return configs.get(id);
  }

  public Iterable<Config> getAllConfigs() {
    return configs.values();
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
