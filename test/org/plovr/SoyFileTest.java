package org.plovr;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.restricted.SoyMsg;
import com.google.template.soy.msgs.restricted.SoyMsgPart;
import com.google.template.soy.msgs.restricted.SoyMsgRawTextPart;

import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static org.junit.Assert.assertTrue;

public class SoyFileTest {

  @Test
  public void testBasicCodegen() {
    File file = new File("testdata/example/templates.soy");
    SoyFileOptions options = new SoyFileOptions.Builder()
        .setPluginModuleNames(ImmutableList.of("org.plovr.soy.function.PlovrModule"))
        .build();
    SoyFile soyFile = new SoyFile("templates.soy", file, options);
    String code = soyFile.generateCode();
    assertTrue(code.indexOf("example.templates.base") != -1);
    assertTrue(code.indexOf("The meaning of life is") != -1);
  }

  @Test
  public void testTranslation() {
    File file = new File("testdata/example/templates.soy");

    SoyMsgBundle bundle = new SoyMsgBundle() {
        @Override public String getLocaleString() {
          return "en";
        }

        @Override public SoyMsg getMsg(long msgId) {
          return new SoyMsg(msgId, "translated message", false,
                            ImmutableList.<SoyMsgPart>of(SoyMsgRawTextPart.of("translated part")));
        }

        @Override public int getNumMsgs() {
          return 1;
        }

        @Override public Iterator<SoyMsg> iterator() {
          return ImmutableList.<SoyMsg>of().iterator();
        }
    };
    SoyFileOptions options = new SoyFileOptions.Builder()
        .setPluginModuleNames(ImmutableList.of("org.plovr.soy.function.PlovrModule"))
        .setMsgBundle(bundle)
        .build();
    SoyFile soyFile = new SoyFile("templates.soy", file, options);
    String code = soyFile.generateCode();
    assertTrue(code.indexOf("The meaning of life is") == -1);
    assertTrue(code.indexOf("translated part") != -1);
  }
}
