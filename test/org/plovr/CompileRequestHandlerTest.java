package org.plovr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CompileRequestHandlerTest {

  @Test
  public void testNeedsRecompile() throws Exception {
    DummyJsInput input = new DummyJsInput("input1.js", "console.log('hello!')");
    Config.Builder builder = Config.builderForTesting();
    builder.setId("recompileTest");
    builder.addInput(input);
    Config config = builder.build();

    CompilationServer server = new CompilationServer("plovr.org", 1234, false);
    CompileRequestHandler handler = new CompileRequestHandler(server);
    assertFalse(handler.haveInputsChangedSince(config, 100));

    input.lastModified = 50;
    assertFalse(handler.haveInputsChangedSince(config, 100));

    input.lastModified = 200;
    assertTrue(handler.haveInputsChangedSince(config, 100));
  }
}
