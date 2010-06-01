package org.plovr;

import java.io.File;
import java.util.logging.Logger;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.jssrc.SoyJsSrcOptions;
import com.google.template.soy.msgs.SoyMsgBundle;

/**
 * {@link JsSourceFile} represents a Soy file.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
class SoyFile extends LocalFileJsInput {

  private static final Logger logger = Logger.getLogger("org.plovr.SoyFile");

  private static final SoyJsSrcOptions SOY_OPTIONS;
  
  static {
    SoyJsSrcOptions jsSrcOptions = new SoyJsSrcOptions();
    jsSrcOptions.setShouldGenerateJsdoc(true);
    jsSrcOptions.setShouldProvideRequireSoyNamespaces(true);
    jsSrcOptions.setShouldDeclareTopLevelNamespaces(true);
    SOY_OPTIONS = jsSrcOptions;
  }

  SoyFile(String name, File source) {
    super(name, source);
  }

  @Override
  public String getCode() {
    SoyFileSet.Builder builder = new SoyFileSet.Builder();
    builder.add(getSource());
    SoyFileSet fileSet = builder.build();
    final SoyMsgBundle msgBundle = null;
    String code = fileSet.compileToJsSrc(SOY_OPTIONS, msgBundle).get(0);
    logger.fine(code);
    return code;
  }

}
