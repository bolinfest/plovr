package org.plovr.i18n;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * {@link PseudoTranslatorTest} is a unit test for {@link PseudoTranslator}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class PseudoTranslatorTest {

  @Test
  public void testTranslate() {
    assertEquals("", PseudoTranslator.translate(""));
    assertEquals("\u03B1bc d\u03B5f", PseudoTranslator.translate("abc def"));
  }

  @Test
  public void testTranslateXlf() throws IOException,
      ParserConfigurationException, SAXException {
    String xlf = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    		"<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">\n" +
    		"  <file original=\"SoyMsgBundle\" datatype=\"x-soy-msg-bundle\" xml:space=\"preserve\" source-language=\"en\">\n" +
    		"    <body>\n" +
    		"      <trans-unit id=\"8148892645275058473\" datatype=\"html\">\n" +
    		"        <source>Personal Settings</source>\n" +
    		"        <note priority=\"1\" from=\"description\">name/email settings page</note>\n" +
    		"        <note priority=\"1\" from=\"meaning\">heading</note>\n" +
    		"      </trans-unit>\n" +
    		"    </body>\n" +
    		"  </file>\n" +
    		"</xliff>";
    InputStream input = new ByteArrayInputStream(xlf.getBytes("UTF-8"));
    Writer writer = new StringWriter();
    PseudoTranslator.translateXlf(input, writer);
    writer.flush();

    // Tried to maintain this as a UTF-8 file so that this string would be more
    // readable, but that created far more problems than it solved.
    String translatedXlf = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<xliff version=\"1.2\" xmlns=\"urn:oasis:names:tc:xliff:document:1.2\">\n" +
        "  <file original=\"SoyMsgBundle\" datatype=\"x-soy-msg-bundle\" xml:space=\"preserve\" source-language=\"en\">\n" +
        "    <body>\n" +
        "      <trans-unit id=\"8148892645275058473\" datatype=\"html\">\n" +
        "        <source>Personal Settings</source><target>P\u03B5rs\u1F79\u03B7\u03B1l S\u03B5tt\u1F76\u03B7gs</target>\n" +
        "        <note priority=\"1\" from=\"description\">name/email settings page</note>\n" +
        "        <note priority=\"1\" from=\"meaning\">heading</note>\n" +
        "      </trans-unit>\n" +
        "    </body>\n" +
        "  </file>\n" +
        "</xliff>";
    assertEquals(translatedXlf, writer.toString());
  }
}
