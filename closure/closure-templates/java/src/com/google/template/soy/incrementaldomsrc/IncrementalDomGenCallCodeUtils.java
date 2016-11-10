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

package com.google.template.soy.incrementaldomsrc;

import com.google.template.soy.incrementaldomsrc.GenIncrementalDomExprsVisitor.GenIncrementalDomExprsVisitorFactory;
import com.google.template.soy.jssrc.internal.GenCallCodeUtils;
import com.google.template.soy.jssrc.internal.JsExprTranslator;
import com.google.template.soy.jssrc.restricted.JsExpr;
import com.google.template.soy.jssrc.restricted.SoyJsSrcPrintDirective;
import com.google.template.soy.soytree.CallParamContentNode;

import java.util.Map;

import javax.inject.Inject;

/**
 * Extends {@link GenCallCodeUtils} to not wrap function arguments as sanitized content, which is
 * used to prevent re-escaping of safe content. The Incremental DOM code generation use DOM APIs for
 * creating Elements, Text and attributes rather than relying on innerHTML.
 */
final class IncrementalDomGenCallCodeUtils extends GenCallCodeUtils {

  /**
   * @param jsExprTranslator Instance of JsExprTranslator to use.
   * @param isComputableAsIncrementalDomExprsVisitor The isComputableAsIncrementalDomExprsVisitor
   *     to be used.
   * @param genIncrementalDomExprsVisitorFactory for creating an instance of
   *     GenIncrementalDomExprsVisitor.
   */
  @Inject
  IncrementalDomGenCallCodeUtils(
      Map<String, SoyJsSrcPrintDirective> soyJsSrcDirectivesMap,
      JsExprTranslator jsExprTranslator,
      IncrementalDomDelTemplateNamer incrementalDomDelTemplateNamer,
      IsComputableAsIncrementalDomExprsVisitor isComputableAsIncrementalDomExprsVisitor,
      GenIncrementalDomExprsVisitorFactory genIncrementalDomExprsVisitorFactory) {
    super(soyJsSrcDirectivesMap,
        jsExprTranslator,
        incrementalDomDelTemplateNamer,
        isComputableAsIncrementalDomExprsVisitor,
        genIncrementalDomExprsVisitorFactory);
  }

  @Override protected JsExpr maybeWrapContent(CallParamContentNode node, JsExpr valueJsExpr) {
    return valueJsExpr;
  }
}

