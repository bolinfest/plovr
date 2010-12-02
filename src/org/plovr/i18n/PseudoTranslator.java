package org.plovr.i18n;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Deque;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.plovr.io.Streams;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * {@link PseudoTranslator} translates an English string into a "pseudo-English"
 * string which contains characters that are slightly different than the
 * original English characters. For example, a lowercase 'a' will be rewritten
 * as a lowercase Greek alpha. By displaying pseudo-translated strings in the
 * UI, it makes it easier to see which strings have not been marked for
 * translation.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public final class PseudoTranslator {

  // TODO(bolinfest): Find good substitutions for more characters in the map.
  private static final Map<Character, Character> ENGLISH_TO_ENGLISH_XXX =
      ImmutableMap.<Character, Character>builder().
      put('a', '\u03B1'). // small alpha
      put('b', 'b').
      put('c', 'c').
      put('d', 'd').
      put('e', '\u03B5'). // small epsilon
      put('f', 'f').
      put('g', 'g').
      put('h', 'h').
      put('i', '\u1F76'). // small iota with varia
      put('j', 'j').
      put('k', '\u03BA'). // small kappa
      put('l', 'l').
      put('m', 'm').
      put('n', '\u03B7'). // small eta
      put('o', '\u1F79'). // small omicron with oxia
      put('p', '\u03C1'). // small rho
      put('q', 'q').
      put('r', 'r').
      put('s', 's').
      put('t', 't').
      put('u', 'u').
      put('v', '\u1F7B'). // small upsilon with oxia
      put('w', '\u03C9'). // small omega
      put('x', '\u03C7'). // small chi
      put('y', 'y').
      put('z', 'z').
      put('A', 'A').
      put('B', 'B').
      put('C', 'C').
      put('D', 'D').
      put('E', 'E').
      put('F', 'F').
      put('G', 'G').
      put('H', 'H').
      put('I', 'I').
      put('J', 'J').
      put('K', 'K').
      put('L', 'L').
      put('M', 'M').
      put('N', 'N').
      put('O', 'O').
      put('P', 'P').
      put('Q', 'Q').
      put('R', 'R').
      put('S', 'S').
      put('T', 'T').
      put('U', 'U').
      put('V', 'V').
      put('W', 'W').
      put('X', 'X').
      put('Y', 'Y').
      put('Z', 'Z').
      build();

  /** Utility class; do not instantiate. */
  private PseudoTranslator() {}

  public static String translate(String englishString) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0, len = englishString.length(); i < len; i++) {
      char c = englishString.charAt(i);
      Character replacement = ENGLISH_TO_ENGLISH_XXX.get(c);
      if (replacement != null) {
        builder.append(replacement);
      } else {
        builder.append(c);
      }
    }
    return builder.toString();
  }

  public static void translateXlf(File input, File output)
  throws IOException, ParserConfigurationException, SAXException {
    InputStream istream = new FileInputStream(input);
    Writer writer = Streams.createL10nFileWriter(output);
    translateXlf(istream, writer);
    istream.close();
    writer.close();
  }

  public static void translateXlf(InputStream input, Writer output)
  throws IOException, ParserConfigurationException, SAXException {
    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
    DefaultHandler handler = new TranslatingContentHandler(output);
    parser.parse(input, handler);
  }

  private static class TranslatingContentHandler extends DefaultHandler {

    /** Writer to which the XLF for the translated XML file is written. */
    private final Writer output;

    /**
     * A stack that represents the current chain of elements that is being
     * traversed by the parser. Maintaining context makes it possible to keep
     * track of (1) the text content of a leaf element, and (2) whether an
     * element has any content/child elements (if not, it must be a self-closing
     * tag).
     */
    private final Deque<Context> contexts;

    private TranslatingContentHandler(Writer writer) {
      this.output = new BufferedWriter(writer);
      this.contexts = Lists.newLinkedList();
    }

    /** Metadata about an element in the parse tree. */
    private static class Context {
      boolean hasContent = false;
      String textContent = null;
    }

    private void write(String str) {
      try {
        output.write(str);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void write(char[] ch, int start, int length) {
      try {
        output.write(ch, start, length);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void startDocument() throws SAXException {
      super.startDocument();
      write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    }

    @Override
    public void endDocument() throws SAXException {
      super.endDocument();
      try {
        output.flush();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    // TODO(bolinfest): Make sure namespaces are handled correctly.
    // TODO(bolinfest): Make sure escaping is handled correctly.

    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException {
      super.startElement(uri, localName, qName, attributes);

      // Update parent context to indicate it has content.
      Context parentContext = contexts.peek();
      if (parentContext != null) {
        // Will be null for the root element.
        parentContext.hasContent = true;
      }

      Context context = new Context();
      contexts.push(context);

      write("<" + qName);
      for (int i = 0; i < attributes.getLength(); i++) {
        String name = attributes.getLocalName(i);
        String value = attributes.getValue(i);
        write(" " + name + "=\"" + value + "\"");
      }
      write(">");
    }

    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {
      super.endElement(uri, localName, qName);

      Context context = contexts.pop();
      if (context.hasContent) {
        write("</" + qName + ">");
      } else {
        write("/>");
      }

      if ("source".equals(qName)) {
        // TODO(bolinfest): A <source> element may have placeholder child
        // elements rather than just plain text.
        String original = context.textContent;
        // TODO(bolinfest): This is probably unsafe if it contains XML entities.
        write("<target>" + PseudoTranslator.translate(original) + "</target>");
      }

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      super.characters(ch, start, length);
      write(ch, start, length);
      Context context = contexts.peek();
      context.hasContent = true;
      context.textContent = new String(ch, start, length);
    }

    /**
     * Ignorable whitespace is still written so that formatting is preserved.
     */
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length)
        throws SAXException {
      super.ignorableWhitespace(ch, start, length);
      write(ch, start, length);
    }
  }

  public static void main(String[] args) {
    // Convenience for seeing what the Unicode characters for our
    // "pseudo-English" locale look like.
    for (Map.Entry<Character, Character> entry : ENGLISH_TO_ENGLISH_XXX.entrySet()) {
      System.out.printf("%s: %s\n", entry.getKey(), entry.getValue());
    }
  }
}
