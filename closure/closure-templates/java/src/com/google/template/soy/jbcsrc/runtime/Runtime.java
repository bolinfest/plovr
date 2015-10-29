/*
 * Copyright 2015 Google Inc.
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

package com.google.template.soy.jbcsrc.runtime;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.data.SanitizedContent.ContentKind;
import com.google.template.soy.data.SoyMap;
import com.google.template.soy.data.SoyRecord;
import com.google.template.soy.data.SoyValue;
import com.google.template.soy.data.SoyValueProvider;
import com.google.template.soy.data.UnsafeSanitizedContentOrdainer;
import com.google.template.soy.data.restricted.IntegerData;
import com.google.template.soy.data.restricted.NullData;
import com.google.template.soy.data.restricted.StringData;
import com.google.template.soy.jbcsrc.api.AdvisingAppendable;
import com.google.template.soy.jbcsrc.api.AdvisingStringBuilder;
import com.google.template.soy.jbcsrc.api.RenderResult;
import com.google.template.soy.jbcsrc.shared.CompiledTemplate;
import com.google.template.soy.jbcsrc.shared.RenderContext;
import com.google.template.soy.msgs.restricted.SoyMsg;
import com.google.template.soy.msgs.restricted.SoyMsgPart;
import com.google.template.soy.msgs.restricted.SoyMsgPlaceholderPart;
import com.google.template.soy.msgs.restricted.SoyMsgPluralPart;
import com.google.template.soy.msgs.restricted.SoyMsgPluralRemainderPart;
import com.google.template.soy.msgs.restricted.SoyMsgRawTextPart;
import com.google.template.soy.msgs.restricted.SoyMsgSelectPart;
import com.google.template.soy.shared.restricted.SoyJavaFunction;
import com.google.template.soy.shared.restricted.SoyJavaPrintDirective;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Runtime utilities uniquely for the {@code jbcsrc} backend.
 */
public final class Runtime {
  public static final SoyValueProvider NULL_PROVIDER = new SoyValueProvider() {
    @Override public RenderResult status() {
      return RenderResult.done();
    }

    @Override public SoyValue resolve() {
      return null;
    }

    @Override public RenderResult renderAndResolve(AdvisingAppendable appendable, boolean isLast)
        throws IOException {
      appendable.append("null");
      return RenderResult.done();
    }

    @Override public String toString() {
      return "NULL_PROVIDER";
    }
  };

  public static AssertionError unexpectedStateError(int state) {
    return new AssertionError("Unexpected state requested: " + state);
  }

  public static boolean stringEqualsAsNumber(String expr, double number) {
    try {
      return Double.parseDouble(expr) == number;
    } catch (NumberFormatException nfe) {
      return false;
    }
  }

  /**
   * Helper function to translate NullData -> null when resolving a SoyValueProvider that may
   * have come from SoyValueProvider.
   */
  public static SoyValue resolveSoyValueProvider(SoyValueProvider provider) {
    SoyValue value = provider.resolve();
    if (value instanceof NullData) {
      return null;
    }
    return value;
  }

  /**
   * Helper function to make SoyRecord.getFieldProvider a non-nullable function by returning
   * {@link #NULL_PROVIDER} for missing fields.
   */
  public static SoyValueProvider getFieldProvider(SoyRecord record, String field) {
    if (record == null) {
      throw new NullPointerException("Attempted to access field '" + field + "' of null");
    }
    // TODO(lukes): ideally this would be the behavior of getFieldProvider, but Tofu relies on it
    SoyValueProvider provider = record.getFieldProvider(field);
    return provider == null | provider instanceof NullData ? NULL_PROVIDER : provider;
  }

  /**
   * Helper function to translate null -> NullData when calling SoyJavaFunctions that may expect it.
   *
   * <p>In the long run we should either fix ToFu (and all SoyJavaFunctions) to not use NullData or
   * we should introduce custom SoyFunction implementations for
   * have come from SoyValueProvider.
   */
  public static SoyValue callSoyFunction(SoyJavaFunction function, List<SoyValue> args) {
    for (int i = 0; i < args.size(); i++) {
      if (args.get(i) == null) {
        args.set(i, NullData.INSTANCE);
      }
    }
    return function.computeForJava(args);
  }

  /**
   * Helper function to translate null -> NullData when calling SoyJavaPrintDirectives that may
   * expect it.
   */
  public static SoyValue applyPrintDirective(
      SoyJavaPrintDirective directive, SoyValue value, List<SoyValue> args) {
    value = value == null ? NullData.INSTANCE : value;
    for (int i = 0; i < args.size(); i++) {
      if (args.get(i) == null) {
        args.set(i, NullData.INSTANCE);
      }
    }
    return directive.applyForJava(value, args);
  }

  /**
   * Wraps a given template with a collection of escapers to apply.
   *
   * @param delegate The delegate template to render
   * @param directives The set of directives to apply
   * @param kind The content kind of the delegate template (if any)
   */
  public static CompiledTemplate applyEscapers(
      final CompiledTemplate delegate, final List<SoyJavaPrintDirective> directives,
      @Nullable final ContentKind kind) {
    return new CompiledTemplate() {
      // TODO(user): tracks adding streaming print directives which would help with this, since
      // it would allow us to eliminate this buffer which fundamentally breaks incremental rendering

      // Note: render() may be called multiple times as part of a render operation that detaches
      // halfway through.  So we need to store the buffer in a field, but we never need to reset it.
      final AdvisingStringBuilder buffer = new AdvisingStringBuilder();
      @Override public RenderResult render(AdvisingAppendable appendable, RenderContext context)
          throws IOException {
        RenderResult result = delegate.render(buffer, context);
        if (result.isDone()) {
          SoyValue resultData =
              kind == null
                  ? StringData.forValue(buffer.toString())
                  : UnsafeSanitizedContentOrdainer.ordainAsSafe(buffer.toString(), kind);
          for (SoyJavaPrintDirective directive : directives) {
            resultData = directive.applyForJava(resultData, ImmutableList.<SoyValue>of());
          }
          appendable.append(resultData.coerceToString());
        }
        return result;
      }
    };
  }

  public static SoyValueProvider getSoyListItem(List<SoyValueProvider> list, long index) {
    if (list == null) {
      throw new NullPointerException("Attempted to access list item '" + index + "' of null");
    }
    int size = list.size();
    if (index < size & index >= 0) {
      SoyValueProvider soyValueProvider = list.get((int) index);
      return soyValueProvider == null ? NULL_PROVIDER : soyValueProvider;
    }
    return NULL_PROVIDER;
  }

  public static SoyValueProvider getSoyMapItem(SoyMap soyMap, SoyValue key) {
    if (soyMap == null) {
      throw new NullPointerException("Attempted to access map item '" + key + "' of null");
    }
    SoyValueProvider soyValueProvider = soyMap.getItemProvider(key);
    return soyValueProvider == null ? NULL_PROVIDER : soyValueProvider;
  }

  /**
   * Render a 'complex' message containing with placeholders.
   */
  public static void renderSoyMsgWithPlaceholders(SoyMsg msg, Map<String, Object> placeholders,
      Appendable out) throws IOException {
    if (msg.getParts().isEmpty()) {
      // TODO(lukes): RenderVisitorAssistantForMsgs does this... but this seems like a weird case
      // investigate eliminating it
      return;
    }
    // TODO(lukes): the initial plural/select nesting structure of the SoyMsg is determined at
    // compile time (though the number of cases varies per locale).
    // We could allow the generated code to call renderSelect/renderPlural directly as a
    // microoptimization.  This could potentially allow us to eliminate the hashmap entry+boxing for
    // plural variables when rendering a direct plural tag.  ditto for selects, though that is
    // more complicated due to the fact that selects can be nested.
    SoyMsgPart firstPart = msg.getParts().get(0);
    if (firstPart instanceof SoyMsgPluralPart) {
      renderPlural(msg, (SoyMsgPluralPart) firstPart, placeholders, out);
    } else if (firstPart instanceof SoyMsgSelectPart) {
      renderSelect(msg, (SoyMsgSelectPart) firstPart, placeholders, out);
    } else {
      for (SoyMsgPart msgPart : msg.getParts()) {
        if (msgPart instanceof SoyMsgRawTextPart) {
          writeRawText((SoyMsgRawTextPart) msgPart, out);
        } else if (msgPart instanceof SoyMsgPlaceholderPart) {
          writePlaceholder((SoyMsgPlaceholderPart) msgPart, placeholders, out);
        } else {
          throw new AssertionError("unexpected part: " + msgPart);
        }
      }
    }
  }

  /**
   * Render a {@code {select}} part of a message.  Most of the complexity is handled by
   * {@link SoyMsgSelectPart#lookupCase} all this needs to do is apply the placeholders to all the
   * children.
   */
  private static void renderSelect(SoyMsg msg, SoyMsgSelectPart firstPart,
      Map<String, Object> placeholders, Appendable out) throws IOException {
    String selectCase = getSelectCase(placeholders, firstPart.getSelectVarName());
    for (SoyMsgPart casePart : firstPart.lookupCase(selectCase)) {
      if (casePart instanceof SoyMsgSelectPart) {
        renderSelect(msg, (SoyMsgSelectPart) casePart, placeholders, out);
      } else if (casePart instanceof SoyMsgPluralPart) {
        renderPlural(msg, (SoyMsgPluralPart) casePart, placeholders, out);
      } else if (casePart instanceof SoyMsgPlaceholderPart) {
        writePlaceholder((SoyMsgPlaceholderPart) casePart, placeholders, out);
      } else if (casePart instanceof SoyMsgRawTextPart) {
        writeRawText((SoyMsgRawTextPart) casePart, out);
      } else {
        // select cannot directly contain remainder nodes.
        throw new AssertionError("unexpected part: " + casePart);
      }
    }
  }

  /**
   * Render a {@code {plural}} part of a message.  Most of the complexity is handled by
   * {@link SoyMsgPluralPart#lookupCase} all this needs to do is apply the placeholders to all the
   * children.
   */
  private static void renderPlural(SoyMsg msg, SoyMsgPluralPart plural,
      Map<String, Object> placeholders, Appendable out) throws IOException {
    int pluralValue = getPlural(placeholders, plural.getPluralVarName());
    for (SoyMsgPart casePart : plural.lookupCase(pluralValue, msg.getLocale())) {
      if (casePart instanceof SoyMsgPlaceholderPart) {
        writePlaceholder((SoyMsgPlaceholderPart) casePart, placeholders, out);

      } else if (casePart instanceof SoyMsgRawTextPart) {
        writeRawText((SoyMsgRawTextPart) casePart, out);

      } else if (casePart instanceof SoyMsgPluralRemainderPart) {
        out.append(String.valueOf(pluralValue - plural.getOffset()));

      } else {
        // Plural parts will not have nested plural/select parts.  So, this is an error.
        throw new AssertionError("unexpected part: " + casePart);
      }
    }
  }

  /** Returns the select case variable value. */
  private static String getSelectCase(Map<String, Object> placeholders, String selectVarName) {
    String selectCase = (String) placeholders.get(selectVarName);
    if (selectCase == null) {
      throw new IllegalArgumentException(
          "No value provided for select: '" + selectVarName + "'");
    }
    return selectCase;
  }

  /** Returns the plural case variable value. */
  private static int getPlural(Map<String, Object> placeholders, String pluralVarName) {
    IntegerData pluralValue = (IntegerData) placeholders.get(pluralVarName);
    if (pluralValue == null) {
      throw new IllegalArgumentException(
          "No value provided for plural: '" + pluralVarName + "'");
    }
    return pluralValue.integerValue();
  }

  /** Append the placeholder to the output stream. */
  private static void writePlaceholder(SoyMsgPlaceholderPart placeholder,
      Map<String, Object> placeholders, Appendable out) throws IOException {
    String placeholderName = placeholder.getPlaceholderName();
    String str = (String) placeholders.get(placeholderName);
    if (str == null) {
      throw new IllegalArgumentException(
          "No value provided for placeholder: '" + placeholderName + "'");
    }
    out.append(str);
  }

  /** Append the raw text segment to the output stream. */
  private static void writeRawText(SoyMsgRawTextPart msgPart, Appendable out) throws IOException {
    out.append(msgPart.getRawText());
  }

  private static final AdvisingAppendable LOGGER = new AdvisingAppendable() {
    @Override public boolean softLimitReached() {
      return false;
    }

    @Override public AdvisingAppendable append(char c) throws IOException {
      System.out.append(c);
      return this;
    }

    @Override public AdvisingAppendable append(CharSequence csq, int start, int end) {
      System.out.append(csq, start, end);
      return this;
    }

    @Override
    public AdvisingAppendable append(CharSequence csq) {
      System.out.append(csq);
      return this;
    }
  };

  public static AdvisingAppendable logger() {
    return LOGGER;
  }

  public static boolean coerceToBoolean(double v) {
    // only NaN is != to itself so this ensures that v is not NaN and not == 0.0
    return v != 0.0 & v == v;
  }

  public static String coerceToString(@Nullable SoyValue v) {
    return v == null ? "null" : v.coerceToString();
  }
}
