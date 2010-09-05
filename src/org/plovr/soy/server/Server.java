package org.plovr.soy.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

/**
 * {@link Server}
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class Server implements Runnable {

  private final Config config;

  public Server(Config config) {
    this.config = config;
  }

  @Override
  public void run() {
    InetSocketAddress addr = new InetSocketAddress(config.getPort());
    HttpServer server;
    try {
      // 0 indicates the system default should be used.
      final int maxQueuedIncomingConnections = 0;
      server = HttpServer.create(addr, maxQueuedIncomingConnections);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    server.createContext("/", new RequestHandlerSelector(config));
    server.start();
  }
}
