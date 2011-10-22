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
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.jssrc.SoyJsSrcOptions;
import com.google.template.soy.jssrc.SoyJsSrcOptions.CodeStyle;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.shared.SoyGeneralOptions.CssHandlingScheme;

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

  SoyFile(String name, File source, SoyFileOptions soyFileOptions) {
    super(name, source);
    this.injector = createInjector(soyFileOptions.pluginModuleNames);
    this.jsSrcOptions = get(soyFileOptions);
  }

  private static SoyJsSrcOptions get(SoyFileOptions options) {
    SoyJsSrcOptions value = jsSrcOptionsMap.get(options);
    if (value == null) {
      value = new SoyJsSrcOptions();
      value.setShouldGenerateJsdoc(true);
      value.setShouldProvideRequireSoyNamespaces(options.useClosureLibrary);
      value.setShouldDeclareTopLevelNamespaces(options.useClosureLibrary);

      // TODO(mbolin): Make this configurable, though for now, prefer CONCAT
      // because the return type in STRINGBUILDER mode is {string|undefined}
      // whereas in CONCAT mode, it is simply {string}, which is much simplier
      // to deal with in the context of the Closure Compiler's type system.
      value.setCodeStyle(CodeStyle.CONCAT);
      jsSrcOptionsMap.put(options, value);
    }
    return value;
  }

  @Override
  public String getCode() {
    SoyFileSet.Builder builder = injector.getInstance(SoyFileSet.Builder.class);
    builder.add(getSource());
    builder.setCssHandlingScheme(CssHandlingScheme.BACKEND_SPECIFIC);
    SoyFileSet fileSet = builder.build();
    final SoyMsgBundle msgBundle = null;
    try {
      String code = fileSet.compileToJsSrc(jsSrcOptions, msgBundle).get(0);
      logger.fine(code);
      return code;
    } catch (SoySyntaxException e) {
      throw new PlovrSoySyntaxException(e, this);
    }
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
