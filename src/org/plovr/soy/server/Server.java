package org.plovr.soy.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.plovr.cli.HttpServerUtil;

import com.sun.net.httpserver.HttpServer;

/**
 * {@link Server} for SoyWeb.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class Server implements Runnable {

  private final Config config;

  private HttpServer server;

  public Server(Config config) {
    this.config = config;
  }

  @Override
  public void run() {
    if (server != null) {
      throw new RuntimeException("Server already started");
    }

    InetSocketAddress addr = new InetSocketAddress(config.getPort());
    try {
      // 0 indicates the system default should be used.
      final int maxQueuedIncomingConnections = 0;
      server = HttpServerUtil.create(
          addr, maxQueuedIncomingConnections, config.isHttps());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    HttpServerUtil.printListeningStatus(server);

    server.createContext("/", new RequestHandlerSelector(config));
    server.start();
  }

  /**
   * Synonym for {@link #run()}
   */
  public void start() {
    run();
  }

  /**
   * Stop listening and close all connections.
   * @param delay Seconds to wait for existing connections to finish.
   */
  public void stop(int delay) {
    if (server == null) {
      throw new RuntimeException("Server not started");
    }
    server.stop(delay);
    server = null;
  }

}
