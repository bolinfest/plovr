package org.plovr.soy.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * {@link RequestHandlerSelectorTest} is a unit test for
 * {@link RequestHandlerSelector}.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class RequestHandlerSelectorTest {

  @Test
  public void testGetFileExtension() {
    assertEquals(null, RequestHandlerSelector.getFileExtension(null));

    assertEquals(".png", RequestHandlerSelector.getFileExtension("logo.png"));
    assertEquals(".css", RequestHandlerSelector.getFileExtension("common.css"));

    assertEquals(".png", RequestHandlerSelector.getFileExtension("directory/logo.png"));
    assertEquals(".css", RequestHandlerSelector.getFileExtension("directory/common.css"));

    assertEquals(".png", RequestHandlerSelector.getFileExtension("directory/foo.logo.png"));
    assertEquals(".css", RequestHandlerSelector.getFileExtension("directory/foo.common.css"));
  }
}
