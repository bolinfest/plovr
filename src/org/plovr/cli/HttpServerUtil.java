package org.plovr.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
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

  public static HttpServer create(InetSocketAddress addr, int backlog, String jksFile, String passphrase) throws IOException {
    if (jksFile.isEmpty()) {
      // return normal HttpServer
      return HttpServer.create(addr, backlog);
    } else {
      // create HttpsServer
      HttpsServer server = HttpsServer.create(addr, backlog);

      // define variables required for the context
      KeyStore keystore = null;
      char[] pass = passphrase.toCharArray();
      KeyManagerFactory keyManagerFactory = null;
      TrustManagerFactory trustManagerFactory = null;
      SSLContext sslContext = null;

      try {
        // try loading KeyStore
        keystore = KeyStore.getInstance("JKS");
        keystore.load(new FileInputStream(jksFile), pass);
        keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keystore, pass);
        trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keystore);

        // initialize SSL Context
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

      } catch (IOException e) {
        System.err.println("Invalid keyfile or passphrase");
      } catch (Exception e) {
        System.err.println("Failed setting SSL Context: " + e.getMessage());
      }

      // set configuration for SSL connections
      server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
        public void configure(HttpsParameters params) {

          SSLContext sslContext = getSSLContext();

          // set default parameters
          SSLParameters sslParams = sslContext.getDefaultSSLParameters();
          params.setSSLParameters(sslParams);

        }
      });

      // return HttpsServer
      return server;
    }
  }
}
