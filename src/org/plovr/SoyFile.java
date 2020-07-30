package org.plovr;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.plovr.io.Files;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.SoyModule;
import com.google.template.soy.jssrc.SoyJsSrcOptions;
import com.google.template.soy.msgs.SoyMsgBundle;

/**
 * {@link JsSourceFile} represents a Soy file.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class SoyFile extends LocalFileJsInput {

  private static final Logger logger = Logger.getLogger("org.plovr.SoyFile");

  private static final Map<SoyFileOptions, SoyJsSrcOptions> jsSrcOptionsMap =
      Maps.newHashMap();

  private final Injector injector;

  private final SoyJsSrcOptions jsSrcOptions;
  private final SoyMsgBundle msgBundle;

  SoyFile(String name, File source, SoyFileOptions soyFileOptions) {
    super(name, source);
    this.injector = createInjector(soyFileOptions.pluginModuleNames);
    this.jsSrcOptions = get(soyFileOptions);
    this.msgBundle = soyFileOptions.msgBundle;
  }

  private static SoyJsSrcOptions get(SoyFileOptions options) {
    SoyJsSrcOptions value = jsSrcOptionsMap.get(options);
    if (value == null) {
      value = new SoyJsSrcOptions();
      value.setShouldProvideRequireSoyNamespaces(options.useClosureLibrary);
      value.setShouldGenerateGoogMsgDefs(options.useClosureLibrary && options.msgBundle == null);
      value.setUseGoogIsRtlForBidiGlobalDir(options.useClosureLibrary && options.msgBundle == null);

      jsSrcOptionsMap.put(options, value);
    }

    return value;
  }

  @Override
  public String generateCode() {
    SoyFileSet.Builder builder = injector.getInstance(SoyFileSet.Builder.class);
    builder.add(getSource());
    SoyFileSet fileSet = builder.build();
    String code = fileSet.compileToJsSrc(jsSrcOptions, msgBundle).get(0);
    logger.fine(code);
    return code;
  }

  @Override
  public boolean isSoyFile() {
    return true;
  }

  @Override
  public String getTemplateCode() {
    try {
      return Files.toString(getSource());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Injector createInjector(List<String> pluginModuleNames) {
    List<Module> guiceModules = Lists.newArrayList();
    guiceModules.add(new SoyModule());

    for (String name : pluginModuleNames) {
      try {
        guiceModules.add((Module) Class.forName(name).newInstance());
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Cannot find plugin module \"" + name + "\".", e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("Cannot access plugin module \"" + name + "\".", e);
      } catch (InstantiationException e) {
        throw new RuntimeException("Cannot instantiate plugin module \"" + name + "\".", e);
      }
    }

    return Guice.createInjector(guiceModules);
  }
}
