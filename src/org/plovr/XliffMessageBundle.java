package org.plovr;

import com.google.common.base.Preconditions;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.javascript.jscomp.GoogleJsMessageIdGenerator;
import com.google.javascript.jscomp.JsMessage;
import com.google.javascript.jscomp.MessageBundle;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.restricted.SoyMsg;
import com.google.template.soy.msgs.restricted.SoyMsgPart;
import com.google.template.soy.msgs.restricted.SoyMsgPlaceholderPart;
import com.google.template.soy.msgs.restricted.SoyMsgRawTextPart;
import com.google.template.soy.xliffmsgplugin.XliffMsgPlugin;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

public class XliffMessageBundle implements MessageBundle {

  private final Map<String, JsMessage> messages;
  private final JsMessage.IdGenerator idGenerator;

  public XliffMessageBundle(InputStream xliff, @Nullable String projectId) {
    Preconditions.checkState(!"".equals(projectId));
    this.messages = Maps.newHashMap();
    this.idGenerator = new GoogleJsMessageIdGenerator(projectId);

    SoyMsgBundle bundle;
    try {
      // TODO: Figure out charset?
      bundle = new XliffMsgPlugin().parseTranslatedMsgsFile(
        new String(ByteStreams.toByteArray(xliff), Charsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    for (SoyMsg msg : bundle) {
      JsMessage.Builder builder = new JsMessage.Builder(String.valueOf(msg.getId()));
      if (msg.getMeaning() != null) {
        builder.setMeaning(msg.getMeaning());
      }
      for (SoyMsgPart part : msg.getParts()) {
        if (part instanceof SoyMsgRawTextPart) {
          builder.appendStringPart(((SoyMsgRawTextPart)part).getRawText());
        } else if (part instanceof SoyMsgPlaceholderPart) {
          builder.appendPlaceholderReference(
            ((SoyMsgPlaceholderPart)part).getPlaceholderName());
        } else {
          Preconditions.checkState(
            false,
            "Unknown message part type: " + part.getClass().getSimpleName());
        }
      }
      messages.put(builder.getKey(), builder.build());
    }
  }

  @Override
  public JsMessage getMessage(String id) {
    return messages.get(id);
  }

  @Override
  public JsMessage.IdGenerator idGenerator() {
    return idGenerator;
  }

  @Override
  public Iterable<JsMessage> getAllMessages() {
    return Iterables.unmodifiableIterable(messages.values());
  }
}
