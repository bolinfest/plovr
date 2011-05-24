package org.plovr.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.plovr.CompilationException;
import org.plovr.Config;
import org.plovr.ConfigParser;
import org.plovr.JsInput;
import org.plovr.Manifest;

import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;
import com.google.template.soy.SoyFileSet;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.SoyMsgPlugin;
import com.google.template.soy.msgs.SoyMsgBundleHandler.OutputFileOptions;
import com.google.template.soy.xliffmsgplugin.XliffMsgPlugin;

public class ExtractCommand extends AbstractCommandRunner<ExtractCommandOptions> {

  @Override
  ExtractCommandOptions createOptions() {
    return new ExtractCommandOptions();
  }

  @Override
  String getUsageIntro() {
    return "Specify a config file with the messages to extract.";
  }

  @Override
  int runCommandWithOptions(ExtractCommandOptions options) throws IOException {
    // Exit if the user did not supply a single config file.
    List<String> arguments = options.getArguments();
    if (arguments.size() != 1) {
      printUsage();
      return 1;
    }

    // Use the config file to get the list of inputs, in order.
    String configFile = arguments.get(0);
    Config config = ConfigParser.parseFile(new File(configFile));
    Manifest manifest = config.getManifest();
    List<JsInput> inputs;
    try {
      inputs = manifest.getInputsInCompilationOrder();
    } catch (CompilationException e) {
      System.err.println(e.getMessage());
      return 1;
    }

    // This logic is modeled after the implementation of
    // com.google.template.soy.SoyMsgExtractor#execMain(String[]).

    // Select all of the Soy files in the list of inputs and add them to a
    // SoyFileSet
    SoyFileSet.Builder sfsBuilder = new SoyFileSet.Builder();
    for (final JsInput input : inputs) {
      if (input.isSoyFile()) {
        InputSupplier<? extends Reader> reader = new InputSupplier<StringReader>() {
          @Override
          public StringReader getInput() throws IOException {
            return new StringReader(input.getTemplateCode());
          }
        };
        sfsBuilder.add(reader, input.getName());
      }
    }

    printMessages(sfsBuilder.build());
    return 0;
  }

  /**
   * Writes the extracted messages to standard out.
   */
  private void printMessages(SoyFileSet sfs) throws IOException {
    SoyMsgBundle msgBundle = sfs.extractMsgs();
    OutputFileOptions soyOutputFileOptions = new OutputFileOptions();
    soyOutputFileOptions.setSourceLocaleString("en");

    SoyMsgPlugin msgPlugin = new XliffMsgPlugin();
    CharSequence seq = msgPlugin.generateExtractedMsgsFile(msgBundle,
        soyOutputFileOptions);
    OutputSupplier<PrintStream> out = new OutputSupplier<PrintStream>() {
      @Override
      public PrintStream getOutput() throws IOException {
        return System.out;
      }
    };
    CharStreams.write(seq, out);
  }

}
