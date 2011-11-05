package org.plovr.cli;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

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
}
