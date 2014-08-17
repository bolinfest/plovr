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
      server = HttpServerUtil.create(
          addr, maxQueuedIncomingConnections, config.getJksFile(), config.getPassphrase());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    HttpServerUtil.printListeningStatus(server);

    server.createContext("/", new RequestHandlerSelector(config));
    server.start();
  }
}
