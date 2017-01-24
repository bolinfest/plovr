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

import com.google.template.soy.SoyFileSet;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.SoyMsgPlugin;
import com.google.template.soy.msgs.SoyMsgBundleHandler.OutputFileOptions;
import com.google.template.soy.xliffmsgplugin.XliffMsgPlugin;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.escape.Escaper;
import com.google.common.xml.XmlEscapers;
import com.google.javascript.jscomp.GoogleJsMessageIdGenerator;
import com.google.javascript.jscomp.JsMessage;
import com.google.javascript.jscomp.JsMessageExtractor;
import com.google.javascript.jscomp.SourceFile;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.SoyMsgBundleHandler.OutputFileOptions;
import com.google.template.soy.msgs.restricted.SoyMsg;
import com.google.template.soy.msgs.restricted.SoyMsgBundleImpl;
import com.google.template.soy.msgs.restricted.SoyMsgPart;
import com.google.template.soy.msgs.restricted.SoyMsgPlaceholderPart;
import com.google.template.soy.msgs.restricted.SoyMsgRawTextPart;
import com.google.template.soy.xliffmsgplugin.XliffMsgPlugin;

public class ExtractCommand extends AbstractCommandRunner<ExtractCommandOptions> {
  private static final Escaper attributeEscaper = XmlEscapers.xmlAttributeEscaper();
  private static final Escaper contentEscaper = XmlEscapers.xmlContentEscaper();

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
      e.print(System.err);
      return 1;
    }

    JsMessageExtractor extractor =
        new JsMessageExtractor(
            new GoogleJsMessageIdGenerator(null), JsMessage.Style.CLOSURE);

    Iterable<JsMessage> messages = extractor.extractMessages(
        Iterables.transform(inputs, new Function<JsInput, SourceFile>() {
          @Override public SourceFile apply(JsInput input) {
            return SourceFile.fromGenerator(input.getName(), input);
          }
        }));

    if (options.getFormat() == Format.XTB) {
      System.out.println(
          "<translationbundle lang=\"" +
          attributeEscaper.escape(config.getLanguage()) +
          "\">");
      for (JsMessage message : messages) {
        System.out.println(
            "<translation id=\"" + attributeEscaper.escape(message.getId()) + "\">" +
                formatMessage(message) +
            "</translation>");
      }
      System.out.println("</translationbundle>");
    } else if (options.getFormat() == Format.XLIFF) {
      OutputFileOptions soyOutputFileOptions = new OutputFileOptions();
      soyOutputFileOptions.setSourceLocaleString(config.getLanguage());
      CharSequence output = new XliffMsgPlugin().generateExtractedMsgsFile(
          convertToBundle(messages), soyOutputFileOptions);
      System.out.print(output);
    } else {
      System.err.println("Unknown format: " + options.getFormat());
    }
    return 0;
  }

  private String formatMessage(JsMessage message) {
    StringBuilder out = new StringBuilder();
    if (message.isHidden()) {
      out.append("<hidden/>\n");
    }
    if (message.getDesc() != null) {
      out.append("<desc>" + contentEscaper.escape(message.getDesc()) + "</desc>\n");
    }
    if (message.getMeaning() != null) {
      out.append("<meaning>" + contentEscaper.escape(message.getMeaning()) + "</meaning>\n");
    }

    for (CharSequence part : message.parts()) {
      // TODO: XML-escape
      if (part instanceof JsMessage.PlaceholderReference) {
        // Placeholder References need to be stored in
        // UPPER_UNDERSCORE format, with some exceptions. See
        // JsMessageVisitor.toLowerCamelCaseWithNumericSuffixes for
        // details.
        String phName = toUpperUnderscoreWithNumbericSuffixes(
            ((JsMessage.PlaceholderReference)part).getName());
        out.append("<ph name=\"" + attributeEscaper.escape(phName) + "\"/>");
      } else {
        out.append(contentEscaper.escape(part.toString()));
      }
    }
    return out.toString();
  }

  private SoyMsgBundle convertToBundle(Iterable<JsMessage> messages) {
    List<SoyMsg> soyMsgs = Lists.newArrayList();
    for (JsMessage msg : messages) {
      List<SoyMsgPart> parts = Lists.newArrayList();
      for (CharSequence part : msg.parts()) {
        if (part instanceof JsMessage.PlaceholderReference) {
          parts.add(
              new SoyMsgPlaceholderPart(
            ((JsMessage.PlaceholderReference)part).getName()));
        } else {
          parts.add(SoyMsgRawTextPart.of((String)part));
        }
      }

      soyMsgs.add(
          new SoyMsg(
              Long.valueOf(msg.getId()),
              null /* localeString */,
              msg.getMeaning(),
              msg.getDesc(),
              msg.isHidden(),
              null /* contentType */,
              new SourceLocation(msg.getSourceName()),
              parts));
    }

    return new SoyMsgBundleImpl(null, soyMsgs);
  }

  static enum Format {
    XTB,
    XLIFF,
    ;
  }

  /**
   * Converts the given string from lower-camel case to
   * upper-underscore case, preserving numeric suffixes. For example,
   * "name" -> "NAME", "A4_LETTER" -> "a4Letter", "START_SPAN_1_23" ->
   * "startSpan_1_23". This is done to counteract the logic that
   * happens when the XTB bundle is read in.
   */
  static String toUpperUnderscoreWithNumbericSuffixes(String input) {
    // Copied from JsMessageVisitor.toLowerCamelCaseWithNumericSuffixes
    // Determine where the numeric suffixes begin
    int suffixStart = input.length();
    while (suffixStart > 0) {
      char ch = '\0';
      int numberStart = suffixStart;
      while (numberStart > 0) {
        ch = input.charAt(numberStart - 1);
        if (Character.isDigit(ch)) {
          numberStart--;
        } else {
          break;
        }
      }
      if ((numberStart > 0) && (numberStart < suffixStart) && (ch == '_')) {
        suffixStart = numberStart - 1;
      } else {
        break;
      }
    }

    if (suffixStart == input.length()) {
      return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, input);
    } else {
      return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE,
          input.substring(0, suffixStart)) +
                  input.substring(suffixStart);
    }
  }

}
