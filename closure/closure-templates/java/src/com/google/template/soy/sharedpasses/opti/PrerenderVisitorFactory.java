/*
 * Copyright 2011 Google Inc.
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

package com.google.template.soy.sharedpasses.opti;

import com.google.common.collect.ImmutableMap;
import com.google.template.soy.shared.restricted.SoyJavaPrintDirective;
import com.google.template.soy.soytree.TemplateRegistry;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * A factory for creating PrerenderVisitor objects.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
@Singleton
public final class PrerenderVisitorFactory {


  /** Map of all SoyJavaPrintDirectives (name to directive). */
  private final ImmutableMap<String, ? extends SoyJavaPrintDirective> soyJavaDirectivesMap;

  /** Factory for creating an instance of PreevalVisitor. */
  private final PreevalVisitorFactory preevalVisitorFactory;

  @Inject
  public PrerenderVisitorFactory(
      ImmutableMap<String, ? extends SoyJavaPrintDirective> soyJavaDirectivesMap,
      PreevalVisitorFactory preevalVisitorFactory) {
    this.soyJavaDirectivesMap = soyJavaDirectivesMap;
    this.preevalVisitorFactory = preevalVisitorFactory;
  }


  /**
   * Creates a PrerenderVisitor.
   *
   * @param outputBuf The Appendable to append the output to.
   * @param templateRegistry A registry of all templates.
   * @return The newly created PrerenderVisitor instance.
   */
  public PrerenderVisitor create(
      Appendable outputBuf, TemplateRegistry templateRegistry) {
    return new PrerenderVisitor(
        soyJavaDirectivesMap, preevalVisitorFactory, outputBuf, templateRegistry);
  }
}
