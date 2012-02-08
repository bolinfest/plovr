package org.plovr;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import com.google.common.base.Preconditions;
import com.sun.net.httpserver.HttpHandler;

/**
 * {@link Handler} is an enumeration of {@link HttpHandler}s that are registered
 * with the {@link CompilationServer}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public enum Handler {
  // Creating an enumeration of handlers makes it easier to generate HTML
  // documentation, as demonstrated by ConfigOption and
  // ConfigOptionDocumentationGenerator.
  INDEX("/", IndexRequestHandler.class),
  CONFIG("/config", ConfigRequestHandler.class),
  COMPILE("/compile", CompileRequestHandler.class),
  CSS("/css", CssHandler.class),
  EXTERNS("/externs", ExternsHandler.class),
  INPUT("/input", InputFileHandler.class),
  LIST("/list", ListHandler.class),
  MODULE("/module", ModuleHandler.class),
  MODULES("/modules", ModulesHandler.class),
  SIZE("/size", SizeHandler.class),
  SOURCEMAP("/sourcemap", SourceMapHandler.class),
  TEST("/test", TestHandler.class),
  VIEW("/view", ViewFileHandler.class),
  ;

  private final String context;

  private final Class<?> httpHandlerClass;

  Handler(String context, Class<?> httpHandlerClass) {
    Preconditions.checkNotNull(context);
    Preconditions.checkNotNull(httpHandlerClass);
    Preconditions.checkArgument(
        implementsInterface(httpHandlerClass, HttpHandler.class),
        "Must implement HttpHandler: " + httpHandlerClass);
    this.context = context;
    this.httpHandlerClass = httpHandlerClass;
  }

  public String getContext() {
    return context;
  }

  public HttpHandler createHandlerForCompilationServer(
      CompilationServer server) {
    try {
      @SuppressWarnings("unchecked")
      Constructor<HttpHandler> constructor = (Constructor<HttpHandler>) httpHandlerClass
          .getConstructor(CompilationServer.class);
      return constructor.newInstance(server);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean implementsInterface(Class<?> clazz, Class<?> iface) {
    while (clazz != null) {
      Class<?>[] interfaces = clazz.getInterfaces();
      if (Arrays.asList(interfaces).contains(iface)) {
        return true;
      } else {
        clazz = clazz.getSuperclass();
      }
    }
    return false;
  }
}
