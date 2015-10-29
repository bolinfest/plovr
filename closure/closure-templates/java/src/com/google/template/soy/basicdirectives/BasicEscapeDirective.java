/*
 * Copyright 2010 Google Inc.
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

package com.google.template.soy.basicdirectives;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableSet;
import com.google.template.soy.data.SoyValue;
import com.google.template.soy.data.restricted.StringData;
import com.google.template.soy.jssrc.restricted.JsExpr;
import com.google.template.soy.jssrc.restricted.SoyJsSrcPrintDirective;
import com.google.template.soy.pysrc.restricted.PyExpr;
import com.google.template.soy.pysrc.restricted.SoyPySrcPrintDirective;
import com.google.template.soy.shared.restricted.Sanitizers;
import com.google.template.soy.shared.restricted.SoyJavaPrintDirective;
import com.google.template.soy.shared.restricted.SoyPurePrintDirective;

import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

/**
 * An escaping directive that is backed by {@link Sanitizers} in java, {@code soyutils.js} or
 * the closure equivalent in JavaScript, and {@code sanitize.py} in Python.
 * See {@link com.google.template.soy.jssrc.internal.GenerateSoyUtilsEscapingDirectiveCode} which
 * creates the JS code that backs escaping directives, and
 * {@link com.google.template.soy.pysrc.internal.GeneratePySanitizeEscapingDirectiveCode} which
 * creates the Python backing code.
 *
 */
public abstract class BasicEscapeDirective
    implements SoyJavaPrintDirective, SoyJsSrcPrintDirective, SoyPySrcPrintDirective {


  private static final ImmutableSet<Integer> VALID_ARGS_SIZES = ImmutableSet.of(0);

  /** The directive name, including the leading vertical bar ("|"). */
  private final String name;


  /**
   * @param name E.g. {@code |escapeUri}.
   */
  public BasicEscapeDirective(String name) {
    this.name = name;
  }


  /**
   * Performs the actual escaping.
   */
  protected abstract String escape(SoyValue value);

  /**
   * The name of the Soy directive that this instance implements.
   */
  @Override public final String getName() {
    return name;
  }

  @Override public final Set<Integer> getValidArgsSizes() {
    return VALID_ARGS_SIZES;
  }

  @Override public final boolean shouldCancelAutoescape() {
    return true;
  }

  @Override public SoyValue applyForJava(SoyValue value, List<SoyValue> args) {
    return StringData.forValue(escape(value));
  }

  @Override public JsExpr applyForJsSrc(JsExpr value, List<JsExpr> args) {
    return new JsExpr(
        "soy.$$" + name.substring(1) + "(" + value.getText() + ")", Integer.MAX_VALUE);
  }

  @Override public PyExpr applyForPySrc(PyExpr value, List<PyExpr> args) {
    String pyFnName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name.substring(1));
    return new PyExpr("sanitize." + pyFnName + "(" + value.getText() + ")", Integer.MAX_VALUE);
  }


  // -----------------------------------------------------------------------------------------------
  // Concrete subclasses.


  /**
   * Implements the |escapeCssString directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class EscapeCssString extends BasicEscapeDirective {

    EscapeCssString() {
      super("|escapeCssString");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.escapeCssString(value);
    }
  }


  /**
   * Implements the |filterCssValue directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class FilterCssValue extends BasicEscapeDirective {

    FilterCssValue() {
      super("|filterCssValue");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.filterCssValue(value);
    }
  }


  /**
   * Implements the |normalizeHtml directive. This escapes the same as escapeHtml except does not
   * escape attributes.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class NormalizeHtml extends BasicEscapeDirective {

    NormalizeHtml() {
      super("|normalizeHtml");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.normalizeHtml(value);
    }
  }


  /**
   * Implements the |escapeHtmlRcdata directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class EscapeHtmlRcdata extends BasicEscapeDirective {

    EscapeHtmlRcdata() {
      super("|escapeHtmlRcdata");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.escapeHtmlRcdata(value);
    }
  }


  /**
   * Implements the |escapeHtmlAttribute directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class EscapeHtmlAttribute extends BasicEscapeDirective {

    EscapeHtmlAttribute() {
      super("|escapeHtmlAttribute");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.escapeHtmlAttribute(value);
    }
  }


  /**
   * Implements the |escapeHtmlAttributeNospace directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class EscapeHtmlAttributeNospace extends BasicEscapeDirective {

    EscapeHtmlAttributeNospace() {
      super("|escapeHtmlAttributeNospace");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.escapeHtmlAttributeNospace(value);
    }
  }


  /**
   * Implements the |filterHtmlAttributes directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class FilterHtmlAttributes extends BasicEscapeDirective {

    FilterHtmlAttributes() {
      super("|filterHtmlAttributes");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.filterHtmlAttributes(value);
    }
  }


  /**
   * Implements the |filterHtmlElementName directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class FilterHtmlElementName extends BasicEscapeDirective {

    FilterHtmlElementName() {
      super("|filterHtmlElementName");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.filterHtmlElementName(value);
    }
  }


  /**
   * Implements the |escapeJsRegex directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class EscapeJsRegex extends BasicEscapeDirective {

    EscapeJsRegex() {
      super("|escapeJsRegex");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.escapeJsRegex(value);
    }
  }


  /**
   * Implements the |escapeJsString directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class EscapeJsString extends BasicEscapeDirective {

    EscapeJsString() {
      super("|escapeJsString");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.escapeJsString(value);
    }
  }


  /**
   * Implements the |escapeJsValue directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class EscapeJsValue extends BasicEscapeDirective {

    EscapeJsValue() {
      super("|escapeJsValue");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.escapeJsValue(value);
    }
  }


  /**
   * Implements the |filterNormalizeUri directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class FilterNormalizeUri extends BasicEscapeDirective {

    FilterNormalizeUri() {
      super("|filterNormalizeUri");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.filterNormalizeUri(value);
    }
  }


  /**
   * Implements the |filterNormalizeMediaUri directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class FilterNormalizeMediaUri extends BasicEscapeDirective {

    FilterNormalizeMediaUri() {
      super("|filterNormalizeMediaUri");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.filterNormalizeMediaUri(value);
    }
  }

  /**
   * Implements the |filterTrustedResourceUri directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class FilterTrustedResourceUri extends BasicEscapeDirective {

    FilterTrustedResourceUri() {
      super("|filterTrustedResourceUri");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.filterTrustedResourceUri(value);
    }
  }


  /**
   * Implements the |normalizeUri directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class NormalizeUri extends BasicEscapeDirective {

    NormalizeUri() {
      super("|normalizeUri");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.normalizeUri(value);
    }
  }


  /**
   * Implements the |escapeUri directive.
   */
  @Singleton
  @SoyPurePrintDirective
  static final class EscapeUri extends BasicEscapeDirective {

    EscapeUri() {
      super("|escapeUri");
    }

    @Override protected String escape(SoyValue value) {
      return Sanitizers.escapeUri(value);
    }
  }
}
