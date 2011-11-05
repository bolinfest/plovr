package org.plovr.cli;

import org.kohsuke.args4j.Option;
import org.plovr.soy.server.SoyRequestHandler;

public class SoyWebCommandOptions extends AbstractCommandOptions {

  /**
   * By default, when a Soy file is rendered, the {@link SoyRequestHandler} uses
   * the template by this name as the one to render as the response to the
   * request. This may be overridden by the {@code templateToRender} constructor
   * parameter.
   */
  private static final String DEFAULT_TEMPLATE_FOR_PAGE = "soyweb";

  @Option(name = "--template",
      aliases = {"-t"},
      usage = "The port on which to run the server. Defaults to \"" +
          DEFAULT_TEMPLATE_FOR_PAGE + "\".")
  private String templateName = DEFAULT_TEMPLATE_FOR_PAGE;

  @Option(name = "--port",
      aliases = {"-p"},
      usage = "The port on which to run the server.")
  private int port = 9811;

  @Option(name = "--dir",
      aliases = {"-d"},
      usage = "Directory that contains the Soy files")
  private String dir = null;

  @Option(name = "--static",
      aliases = {"-s"},
      usage = "Parse the Soy files once on startup")
  private boolean isStatic = false;

  @Option(name = "--globals",
      aliases = {"-g"},
      usage = "File where global variables for Soy are defined")
  private String compileTimeGlobalsFile = null;

  @Option(name = "--unsafe",
      usage = "Lets a user inject template data via URL query parameters. " +
      "This is considered unsafe because it creates the possibility for XSS " +
      "attacks. Therefore, this should be used for internal prototyping ONLY.")
  private boolean allowQueryParameterInjection = false;

  public SoyWebCommandOptions() {}

  public String getTemplateName() {
    return templateName;
  }

  public int getPort() {
    return port;
  }

  public String getDir() {
    return dir;
  }

  public boolean isStatic() {
    return isStatic;
  }

  public String getCompileTimeGlobalsFile() {
    return compileTimeGlobalsFile;
  }

  public boolean isSafeMode() {
    return !allowQueryParameterInjection;
  }
}
