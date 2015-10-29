/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.template.soy.MainClassUtils.Main;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.jssrc.SoyJsSrcOptions;
import com.google.template.soy.xliffmsgplugin.XliffMsgPluginModule;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Executable for compiling a set of Soy files into corresponding JS source files.
 *
 */
public final class SoyToJsSrcCompiler {


  /** The string to prepend to the usage message. */
  private static final String USAGE_PREFIX =
      "Usage:\n" +
      "java com.google.template.soy.SoyToJsSrcCompiler  \\\n" +
      "     [<flag1> <flag2> ...] --outputPathFormat <formatString>  \\\n" +
      "     --srcs <soyFilePath>,... [--deps <soyFilePath>,...]\n";


  @Option(name = "--inputPrefix",
          usage = "If provided, this path prefix will be prepended to each input file path" +
                  " listed on the command line. This is a literal string prefix, so you'll need" +
                  " to include a trailing slash if necessary.")
  private String inputPrefix = "";

  @Option(name = "--srcs",
          usage = "[Required] The list of source Soy files.",
          handler = MainClassUtils.StringListOptionHandler.class)
  private List<String> srcs = Lists.newArrayList();

  @Option(name = "--deps",
          usage = "The list of dependency Soy files (if applicable). The compiler needs deps for" +
                  " analysis/checking, but will not generate code for dep files.",
          handler = MainClassUtils.StringListOptionHandler.class)
  private List<String> deps = Lists.newArrayList();

  @Option(name = "--indirectDeps",
          usage = "Soy files required by deps, but which may not be used by srcs.",
          handler = MainClassUtils.StringListOptionHandler.class)
  private List<String> indirectDeps = Lists.newArrayList();

  @Option(name = "--allowExternalCalls",
          usage = "Whether to allow external calls. New projects should set this to false, and" +
                  " existing projects should remove existing external calls and then set this" +
                  " to false. It will save you a lot of headaches. Currently defaults to true" +
                  " for backward compatibility.",
          handler = MainClassUtils.BooleanOptionHandler.class)
  private boolean allowExternalCalls = true;

  @Option(name = "--outputPathFormat",
          required = true,
          usage = "[Required] A format string that specifies how to build the path to each" +
                  " output file. If not generating localized JS, then there will be one output" +
                  " JS file (UTF-8) for each input Soy file. If generating localized JS, then" +
                  " there will be one output JS file for each combination of input Soy file and" +
                  " locale. The format string can include literal characters as well as the" +
                  " placeholders {INPUT_PREFIX}, {INPUT_DIRECTORY}, {INPUT_FILE_NAME}," +
                  " {INPUT_FILE_NAME_NO_EXT}, {LOCALE}, {LOCALE_LOWER_CASE}. Note" +
                  " {LOCALE_LOWER_CASE} also turns dash into underscore, e.g. pt-BR becomes" +
                  " pt_br.")
  private String outputPathFormat = "";

  @Option(name = "--syntaxVersion",
          usage = "User-declared syntax version for the Soy file bundle (e.g. 2.0, 2.3).")
  private String syntaxVersion = "";

  @Option(name = "--isUsingIjData",
          usage = "Whether to enable use of injected data (syntax is '$ij.*').",
          handler = MainClassUtils.BooleanOptionHandler.class)
  private boolean isUsingIjData = false;

  // TODO(user): remove
  @Option(name = "--codeStyle",
          usage = "The code style to use when generating JS code ('stringbuilder' or 'concat').")
  private String codeStyle = "concat";

  @Option(name = "--shouldGenerateJsdoc",
          usage = "Whether we should generate JSDoc with type info for the Closure Compiler." +
                  " Note the generated JSDoc does not have description text, only types for the" +
              " benefit of the Closure Compiler.",
          handler = MainClassUtils.BooleanOptionHandler.class)
  private boolean shouldGenerateJsdoc = false;

  @Option(name = "--shouldProvideRequireSoyNamespaces",
          usage = "When this option is used, each generated JS file will contain (a) one single" +
                  " goog.provide statement for the corresponding Soy file's namespace and" +
                  " (b) goog.require statements for the namespaces of the called templates.",
          handler = MainClassUtils.BooleanOptionHandler.class)
  private boolean shouldProvideRequireSoyNamespaces = false;

  @Option(name = "--shouldDeclareTopLevelNamespaces",
          usage = "[Only applicable when generating regular JS code to define namespaces (i.e." +
                  " not generating goog.provide/goog.require).] When this option is set to" +
                  " false, each generated JS file will not attempt to declare the top-level" +
                  " name in its namespace, instead assuming the top-level name is already" +
                  " declared in the global scope. E.g. for namespace aaa.bbb, the code will not" +
                  " attempt to declare aaa, but will still define aaa.bbb if it's not already" +
                  " defined.",
          handler = MainClassUtils.BooleanOptionHandler.class)
  private boolean shouldDeclareTopLevelNamespaces = true;

  @Option(name = "--locales",
          usage = "[Required for generating localized JS] Comma-delimited list of locales for" +
                  " which to generate localized JS. There will be one output JS file for each" +
                  " combination of input Soy file and locale.",
          handler = MainClassUtils.StringListOptionHandler.class)
  private List<String> locales = Lists.newArrayList();

  @Option(name = "--messageFilePathFormat",
          usage = "[Required for generating localized JS] A format string that specifies how to" +
                  " build the path to each translated messages file. The format string can" +
                  " include literal characters as well as the placeholders {INPUT_PREFIX}," +
                  " {LOCALE}, and {LOCALE_LOWER_CASE}. Note {LOCALE_LOWER_CASE} also turns dash" +
                  " into underscore, e.g. pt-BR becomes pt_br. The format string must end with" +
                  " an extension matching the message file format (case-insensitive).")
  private String messageFilePathFormat = "";

  @Option(name = "--shouldGenerateGoogMsgDefs",
          usage = "When this option is used, all 'msg' blocks will be turned into goog.getMsg" +
                  " definitions and corresponding usages. Must be used with either" +
                  " --bidiGlobalDir, or --useGoogIsRtlForBidiGlobalDir, usually the latter." +
                  " Also see --googMsgsAreExternal.")
  private boolean shouldGenerateGoogMsgDefs = false;

  @Option(name = "--googMsgsAreExternal",
          usage = "[Only applicable if --shouldGenerateGoogMsgDefs is true]" +
                  " If this option is true, then we generate" +
                  " \"var MSG_EXTERNAL_<soyGeneratedMsgId> = goog.getMsg(...);\"." +
                  " If this option is false, then we generate" +
                  " \"var MSG_UNNAMED_<uniquefier> = goog.getMsg(...);\"." +
                  "  [Explanation of true value]" +
                  " Set this option to true if your project is having Closure Templates do" +
                  " message extraction (e.g. with SoyMsgExtractor) and then having the Closure" +
                  " Compiler do translated message insertion." +
                  "  [Explanation of false value]" +
                  " Set this option to false if your project is having the Closure Compiler do" +
                  " all of its localization, i.e. if you want the Closure Compiler to do both" +
                  " message extraction and translated message insertion. A significant drawback" +
                  " to this setup is that, if your templates are used from both JS and Java, you" +
                  " will end up with two separate and possibly different sets of translations" +
                  " for your messages.")
  private boolean googMsgsAreExternal = false;

  @Option(name = "--bidiGlobalDir",
          usage = "The bidi global directionality (ltr=1, rtl=-1). Only applicable if your Soy" +
                  " code uses bidi functions/directives. Also note that this flag is usually not" +
                  " necessary if a message file is provided, because by default the bidi global" +
                  " directionality is simply inferred from the message file.")
  private int bidiGlobalDir = 0;

  @Option(name = "--useGoogIsRtlForBidiGlobalDir",
          usage = "[Only applicable if both --shouldGenerateGoogMsgDefs and" +
                  " --shouldProvideRequireSoyNamespaces" +
                  " is true]" +
                  " Whether to determine the bidi global direction at template runtime by" +
                  " evaluating goog.i18n.bidi.IS_RTL. Do not combine with --bidiGlobalDir.")
  private boolean useGoogIsRtlForBidiGlobalDir = false;

  @Option(name = "--compileTimeGlobalsFile",
          usage = "The path to a file containing the mappings for global names to be substituted" +
                  " at compile time. Each line of the file should have the format" +
                  " \"<global_name> = <primitive_data>\" where primitive_data is a valid Soy" +
                  " expression literal for a primitive type (null, boolean, integer, float, or" +
                  " string). Empty lines and lines beginning with \"//\" are ignored. The file" +
                  " should be encoded in UTF-8. If you need to generate a file in this format" +
                  " from Java, consider using the utility" +
                  " SoyUtils.generateCompileTimeGlobalsFile().")
  private String compileTimeGlobalsFile = "";

  @Option(name = "--messagePluginModule",
          usage = "Specifies the full class name of a Guice module that binds a SoyMsgPlugin." +
                  " If not specified, the default is" +
                  " com.google.template.soy.xliffmsgplugin.XliffMsgPluginModule, which binds" +
                  " the XliffMsgPlugin.")
  private String messagePluginModule = XliffMsgPluginModule.class.getName();

  @Option(name = "--pluginModules",
          usage = "Specifies the full class names of Guice modules for function plugins and" +
                  " print directive plugins (comma-delimited list).")
  private String pluginModules = "";

  @Option(name = "--supportContentSecurityPolicy",
          usage = "Adds attributes so that browsers that support the Content Security Policy" +
                  " (CSP) can distinguish inline scripts written by the template author from" +
                  " any injected via XSS.  If true, the parameter {$ij.csp_nonce} should" +
                  " contain an unpredictable per-page-render secret consisting of ASCII" +
                  " alpha-numerics, plus (+), and solidus (/).  Off by default.",
          handler = MainClassUtils.BooleanOptionHandler.class)
  private boolean supportContentSecurityPolicy = false;

  /** The remaining arguments after parsing command-line flags. */
  @Argument
  private List<String> arguments = Lists.newArrayList();


  /**
   * Compiles a set of Soy files into corresponding JS source files.
   *
   * @param args Should contain command-line flags and the list of paths to the Soy files.
   * @throws IOException If there are problems reading the input files or writing the output file.
   * @throws SoySyntaxException If a syntax error is detected.
   */
  public static void main(final String[] args) throws IOException, SoySyntaxException {
    MainClassUtils.run(new Main() {
      @Override
      public CompilationResult main() throws IOException {
        return new SoyToJsSrcCompiler().execMain(args);
      }
    });
  }

  private SoyToJsSrcCompiler() {}

  private CompilationResult execMain(String[] args) throws IOException {

    final CmdLineParser cmdLineParser = MainClassUtils.parseFlags(this, args, USAGE_PREFIX);

    final Function<String, Void> exitWithErrorFn = new Function<String, Void>() {
      @Override public Void apply(String errorMsg) {
        MainClassUtils.exitWithError(errorMsg, cmdLineParser, USAGE_PREFIX);
        return null;
      }
    };

    if (outputPathFormat.isEmpty()) {
      exitWithErrorFn.apply("Must provide the output path format.");
    }

    Injector injector = MainClassUtils.createInjector(messagePluginModule, pluginModules);

    // Create SoyFileSet.
    SoyFileSet.Builder sfsBuilder = injector.getInstance(SoyFileSet.Builder.class);
    MainClassUtils.addSoyFilesToBuilder(
        sfsBuilder, inputPrefix, srcs, arguments, deps, indirectDeps, exitWithErrorFn);
    if (!syntaxVersion.isEmpty()) {
      if (syntaxVersion.equals("1.0")) {
        exitWithErrorFn.apply("Declared syntax version must be 2.0 or greater.");
      }
      sfsBuilder.setDeclaredSyntaxVersionName(syntaxVersion);
    }
    sfsBuilder.setAllowExternalCalls(allowExternalCalls);
    if (!compileTimeGlobalsFile.isEmpty()) {
      sfsBuilder.setCompileTimeGlobals(new File(compileTimeGlobalsFile));
    }
    sfsBuilder.setSupportContentSecurityPolicy(supportContentSecurityPolicy);
    SoyFileSet sfs = sfsBuilder.build();

    // Create SoyJsSrcOptions.
    SoyJsSrcOptions jsSrcOptions = new SoyJsSrcOptions();
    jsSrcOptions.setIsUsingIjData(isUsingIjData);
    jsSrcOptions.setShouldGenerateJsdoc(shouldGenerateJsdoc);
    jsSrcOptions.setShouldProvideRequireSoyNamespaces(shouldProvideRequireSoyNamespaces);
    jsSrcOptions.setShouldDeclareTopLevelNamespaces(shouldDeclareTopLevelNamespaces);
    jsSrcOptions.setShouldGenerateGoogMsgDefs(shouldGenerateGoogMsgDefs);
    jsSrcOptions.setGoogMsgsAreExternal(googMsgsAreExternal);
    jsSrcOptions.setBidiGlobalDir(bidiGlobalDir);
    jsSrcOptions.setUseGoogIsRtlForBidiGlobalDir(useGoogIsRtlForBidiGlobalDir);

    // Compile.
    boolean generateLocalizedJs = !locales.isEmpty();
    return generateLocalizedJs
        ? sfs.compileToJsSrcFiles(
        outputPathFormat, inputPrefix, jsSrcOptions, locales, messageFilePathFormat)
        : sfs.compileToJsSrcFiles(
            outputPathFormat, inputPrefix, jsSrcOptions, locales, null);
  }
}
