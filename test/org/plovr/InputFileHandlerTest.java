package org.plovr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.base.Function;

/**
 * {@link InputFileHandlerTest} is a unit test for {@link InputFileHandler}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class InputFileHandlerTest {

  /**
   * Unit test for http://code.google.com/p/plovr/issues/detail?id=25.
   */
  @Test
  public void testInputNameToUriConverter() {
    String moduleUriBase = "http://plovr.org:9988/";
    String configId = "inputNameTest";
    Function<JsInput,String> converter = InputFileHandler.
        createInputNameToUriConverter(moduleUriBase, configId);

    JsInput input = createDummyJsInput("../../bar/foo.js");
    String uri = converter.apply(input);
    assertEquals(
        "../ should be replaced with $$/",
        "http://plovr.org:9988/input/inputNameTest/$$/$$/bar/foo.js",
        uri);
  }

  private static JsInput createDummyJsInput(String name) {
    return new DummyJsInput(name, "alert('ok');", null, null);
  }
}
