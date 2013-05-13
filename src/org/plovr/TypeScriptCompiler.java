package org.plovr;

import java.io.IOException;
import java.net.URL;

import javax.script.Bindings;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Resources;

public class TypeScriptCompiler
    extends AbstractJavaScriptBasedCompiler<TypeScriptCompilerException> {

  /**
   * @return the singleton instance of the {@link TypeScriptCompiler}
   */
  public static TypeScriptCompiler getInstance() {
    return TypeScriptCompilerHolder.instance;
  }

  private final String libraryDefinitions;

  private TypeScriptCompiler() {
    super("org/plovr/typescript.js");

    URL libraryDefinitionsUrl = Resources.getResource("org/plovr/lib.d.ts");
    try {
      libraryDefinitions = Resources.toString(libraryDefinitionsUrl, Charsets.UTF_8);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  protected String insertScopeVariablesAndGenerateExecutableJavaScript(
      Bindings compileScope, String sourceCode, String sourceName) {
    compileScope.put("typeScriptInput", sourceCode);
    compileScope.put("filenameForErrorReportingPurposes", sourceName);
    compileScope.put("libraryDefinitions", libraryDefinitions);

    String js =
        "(function() {\n" +
    		"  var createITextWriter = function() {\n" +
    		"    var out = '';\n" +
    		"    return {\n" +
    		"      Write: function(s) {\n" +
    		"        out += s;\n" +
    		"      },\n" +
    		"      WriteLine: function(s) {\n" +
    		"        out += s + '\\n';\n" +
    		"      },\n" +
    		"      Close: function() {},\n" +
    		"      getOutput : function() { return out; }\n" +
    		"    };\n" +
    		"  }\n" +
        "\n" +
        "  var value = '';\n" +
    		"  var error = null;\n" +
    		"  try {\n" +
    		"    var errorOutput = createITextWriter();\n" +
    		"    var logger = new TypeScript.NullLogger();\n" +
    		"    var settings = new TypeScript.CompilationSettings();\n" +
    		"    settings.outputGoogleClosureAnnotations = true;\n" +
    		"\n" +
    		"    var compiler = new TypeScript.TypeScriptCompiler(errorOutput, logger, settings);\n" +
    		"    compiler.addUnit(libraryDefinitions, 'lib.d.ts');\n" +
    		"    compiler.addUnit(typeScriptInput, filenameForErrorReportingPurposes);\n" +
    		"    compiler.typeCheck();\n" +
    		"\n" +
    		"    var originalTextWriter;\n" +
    		"    var createFile = function(path, useUtf8) {\n" +
    		"      var newTextWriter = createITextWriter();\n" +
    		"      if (!originalTextWriter) originalTextWriter = newTextWriter;\n" +
    		"      return newTextWriter;\n" +
    		"    };\n" +
    		"    compiler.emit(createFile);\n" +
    		"\n" +
    		"    if (errorOutput.getOutput()) {\n" +
    		"      return {message: errorOutput.getOutput()};\n" +
    		"    } else {\n" +
        "      return originalTextWriter.getOutput();\n" +
        "    }\n" +
    		"  } catch (e) {\n" +
    		"    return {message: e.message};\n" +
    		"  }\n" +
    		"})();";
    return js;
  }

  @Override
  protected TypeScriptCompilerException generateExceptionFromMessage(
      String message) {
    return new TypeScriptCompilerException(message);
  }

  private static class TypeScriptCompilerHolder {
    private static TypeScriptCompiler instance = new TypeScriptCompiler();
  }
}
