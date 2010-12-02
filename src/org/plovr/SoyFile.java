package org.plovr;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.plovr.io.Files;

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.jssrc.SoyJsSrcOptions;
import com.google.template.soy.jssrc.SoyJsSrcOptions.CodeStyle;
import com.google.template.soy.msgs.SoyMsgBundle;

/**
 * {@link JsSourceFile} represents a Soy file.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class SoyFile extends LocalFileJsInput {

  private static final Logger logger = Logger.getLogger("org.plovr.SoyFile");

  private static final SoyJsSrcOptions SOY_OPTIONS;

  static {
    SoyJsSrcOptions jsSrcOptions = new SoyJsSrcOptions();
    jsSrcOptions.setShouldGenerateJsdoc(true);
    jsSrcOptions.setShouldProvideRequireSoyNamespaces(true);
    jsSrcOptions.setShouldDeclareTopLevelNamespaces(true);

    // TODO(mbolin): Make this configurable, though for now, prefer CONCAT
    // because the return type in STRINGBUILDER mode is {string|undefined}
    // whereas in CONCAT mode, it is simply {string}, which is much simplier to
    // deal with in the context of the Closure Compiler's type system.
    jsSrcOptions.setCodeStyle(CodeStyle.CONCAT);

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

}
