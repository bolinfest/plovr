package org.plovr;

import sun.org.mozilla.javascript.internal.Scriptable;

public class TypeScriptCompiler
    extends AbstractJavaScriptBasedCompiler<TypeScriptCompilerException> {

  /**
   * @return the singleton instance of the {@link TypeScriptCompiler}
   */
  public static TypeScriptCompiler getInstance() {
    return TypeScriptCompilerHolder.instance;
  }

  private TypeScriptCompiler() {
    super("org/plovr/typescript.js");
  }

  @Override
  protected String insertScopeVariablesAndGenerateExecutableJavaScript(
      Scriptable compileScope, String sourceCode, String sourceName) {
    compileScope.put("typeScriptInput", compileScope, sourceCode);
    compileScope.put("filenameForErrorReportingPurposes", compileScope, sourceName);

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
    		"\n" +
    		// TODO(bolinfest): Include lib.d.ts until there is a plan
    		// for converting d.ts files into extern files.
//    		"    // EXTERNS may not be defined when developing the demo locally.\n" +
//    		"    if (typeof EXTERNS != 'undefined') {\n" +
//    		"      compiler.addUnit(EXTERNS, 'lib.d.ts');\n" +
//    		"    }\n" +
//    		"\n" +
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
