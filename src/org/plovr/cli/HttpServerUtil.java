package org.plovr.cli;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;

/**
 * {@link HttpServerUtil} is a collection of utilities for dealing with
 * {@link HttpServer} objects.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class HttpServerUtil {

  /** Utility class: do not instantiate. */
  private HttpServerUtil() {}

  public static void printListeningStatus(HttpServer server) {
    InetSocketAddress serverAddress = server.getAddress();
    System.err.println("Listening on " + serverAddress);
  }

  public static HttpServer create(InetSocketAddress addr, int backlog, boolean isHttps) throws IOException {
    if (isHttps) {
      return HttpsServer.create(addr, backlog);
    } else {
      return HttpServer.create(addr, backlog);
    }
  }
}
