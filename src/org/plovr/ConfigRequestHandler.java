package org.plovr;

import java.io.IOException;

import org.plovr.io.Responses;

import com.sun.net.httpserver.HttpExchange;

/**
 * {@link ConfigRequestHandler} displays the contents of the config file that
 * is responsible for creating the corresponding {@link Config}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
final class ConfigRequestHandler extends AbstractGetHandler {

  public ConfigRequestHandler(CompilationServer server) {
    super(server, true /* usesRestfulPath */);
  }

  @Override
  protected void doGet(HttpExchange exchange, QueryData data, Config config)
      throws IOException {
    Responses.writePlainText(config.getRootConfigFileContent(), exchange);
  }
}
