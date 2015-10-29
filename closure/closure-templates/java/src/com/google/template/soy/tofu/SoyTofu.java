/*
 * Copyright 2008 Google Inc.
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

package com.google.template.soy.tofu;

import com.google.common.collect.ImmutableSortedSet;
import com.google.template.soy.data.SanitizedContent;
import com.google.template.soy.data.SoyRecord;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.parseinfo.SoyTemplateInfo;
import com.google.template.soy.shared.SoyCssRenamingMap;
import com.google.template.soy.shared.SoyIdRenamingMap;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;


/**
 * SoyTofu is the public interface for a Java object that represents a compiled Soy file set.
 *
 * <p> Important: If you're a user of Soy, you should use the methods here (on a SoyTofu object
 * created by Soy), but should not create your own implementations of this interface.
 *
 */
public interface SoyTofu {


  /**
   * Gets the namespace of this SoyTofu object. The namespace is simply a convenience allowing
   * {@code newRenderer()} to be called with a partial template name (e.g. ".fooTemplate").
   * Note: The namespace may be null, in which case {@code newRenderer()} must be called with the
   * full template name.
   *
   * @return The namespace of this SoyTofu object, or null if no namespace.
   */
  String getNamespace();


  /**
   * Gets a new SoyTofu instance with a different namespace (or no namespace).
   * Note: The new SoyTofu instance will still be backed by the same compiled Soy file set.
   *
   * @param namespace The namespace for the new SoyTofu instance, or null for no namespace.
   * @return A new SoyTofu instance with a different namespace (or no namespace).
   */
  SoyTofu forNamespace(@Nullable String namespace);

  /**
   * Gets a new Renderer for a template.
   *
   * <p> The usage pattern is
   *   soyTofu.newRenderer(...).setData(...).setInjectedData(...).setMsgBundle(...).render()
   * where any of the set* parts can be omitted if it's null.
   *
   * @param templateInfo Info for the template to render.
   * @return A new renderer for the given template.
   */
  Renderer newRenderer(SoyTemplateInfo templateInfo);


  /**
   * Gets a new Renderer for a template.
   *
   * <p> The usage pattern is
   *   soyTofu.newRenderer(...).setData(...).setInjectedData(...).setMsgBundle(...).render()
   * where any of the set* parts can be omitted if it's null.
   *
   * @param templateName The name of the template to render. If this SoyTofu instance is not
   *     namespaced, then this parameter should be the full name of the template including the
   *     namespace. If this SoyTofu instance is namespaced, then this parameter should be a partial
   *     name beginning with a dot (e.g. ".fooTemplate").
   * @return A new renderer for the given template.
   */
  Renderer newRenderer(String templateName);


  /**
   * Gets the set of injected param keys used by a template (and its transitive callees).
   *
   * <p> Note: The {@code SoyTemplateInfo} object already has a method {@code getUsedIjParams()}.
   * That method should produce the same results as this method, unless the bundle of Soy files
   * included when running the SoyParseInfoGenerator is different from the bundle of Soy files
   * included when creating this SoyTofu object.
   *
   * @param templateInfo Info for the template to get injected params of.
   * @return The set of injected param keys used by the given template.
   */
  ImmutableSortedSet<String> getUsedIjParamsForTemplate(SoyTemplateInfo templateInfo);


  /**
   * Gets the set of injected param keys used by a template (and its transitive callees).
   *
   * @param templateName The name of the template to get injected params of.
   * @return The set of injected param keys used by the given template.
   */
  ImmutableSortedSet<String> getUsedIjParamsForTemplate(String templateName);


  // -----------------------------------------------------------------------------------------------
  // Renderer interface.


  /**
   * Renderer for a template.
   *
   * <p> Important: If you're a user of Soy, you should use the methods here (on a Renderer object
   * created by Soy), but should not create your own implementations of this interface.
   */
  interface Renderer {

    /**
     * Sets the data to call the template with. Can be null if the template has no parameters.
     *
     * <p> Note: If you call this method instead of {@link #setData(SoyRecord)}, your template data
     * will be converted to a {@code SoyMapData} object on each call. This may not be a big deal if
     * you only need to use the data object once. But if you need to reuse the same data object for
     * multiple calls, it's more efficient to build your own {@code SoyRecord} object and reuse it
     * with {@link #setData(SoyRecord)}.
     */
    Renderer setData(Map<String, ?> data);

    /**
     * Sets the data to call the template with. Can be null if the template has no parameters.
     */
    Renderer setData(SoyRecord data);

    /**
     * Sets the injected data to call the template with. Can be null if not used.
     *
     * <p> Note: If you call this method instead of {@link #setIjData(SoyRecord)}, the data
     * will be converted to a {@code SoyRecord} object on each call. This may not be a big deal if
     * you only need to use the data object once. But if you need to reuse the same data object for
     * multiple calls, it's more efficient to build your own {@code SoyRecord} object and reuse it
     * with {@link #setIjData(SoyRecord)}.
     */
    Renderer setIjData(Map<String, ?> ijData);

    /**
     * Sets the injected data to call the template with. Can be null if not used.
     */
    Renderer setIjData(SoyRecord ijData);

    /**
     * Sets the set of active delegate package names.
     */
    Renderer setActiveDelegatePackageNames(Set<String> activeDelegatePackageNames);

    /**
     * Sets the bundle of translated messages, or null to use the messages from the Soy source.
     */
    Renderer setMsgBundle(SoyMsgBundle msgBundle);

    /**
     * Sets the ID renaming map.
     */
    Renderer setIdRenamingMap(SoyIdRenamingMap idRenamingMap);

    /**
     * Sets the CSS renaming map.
     */
    Renderer setCssRenamingMap(SoyCssRenamingMap cssRenamingMap);

    /**
     * Sets the expected content kind.
     *
     * <p>An attempt to render a non-strict template or a strict template with a different kind
     * will fail if this has been called.
     */
    Renderer setContentKind(SanitizedContent.ContentKind contentKind);

    /**
     * Renders the template using the data, injected data, and message bundle previously set.
     *
     * <p>Checks the content kind of the template. Non-strict and kind="html" templates are
     * allowed, unless setContentKind was called. The goal is to prevent accidental rendering
     * of unescaped kind="text" in contexts where that could XSS.
     *
     * @throws SoyTofuException if an error occurs during rendering.
     */
    String render();

    /**
     * Renders the strict-mode template as a SanitizedContent object, which can be used as an input
     * to another Soy template, or used to verify that the output type is correct.
     *
     * <p>This returns a SanitizedContent object corresponding to the kind="..." attribute of the
     * template. The expected content kind must be set beforehand, unless HTML is expected, to
     * avoid an exception.
     *
     * @throws IllegalArgumentException If the template is non-strict, or the kind doesn't match
     *     the expected kind (from setContentKind, or the default of HTML).
     * @throws SoyTofuException if an error occurs during rendering.
     */
    SanitizedContent renderStrict();

    /**
     * Renders the template using the data, injected data, and message bundle previously set
     * into the given Appendable.
     *
     * <p>Checks the content kind of the template. Non-strict and kind="html" templates are
     * allowed, unless setContentKind was called. The goal is to prevent accidental rendering
     * of unescaped kind="text" in contexts where that could XSS.
     *
     * @throws SoyTofuException if an error occurs during rendering.
     */
    SanitizedContent.ContentKind render(Appendable out);
  }


  // -----------------------------------------------------------------------------------------------
  // Old render methods.


  /**
   * Renders a template.
   *
   * @param templateInfo Info for the template to render.
   * @param data The data to call the template with. Can be null if the template has no parameters.
   * @param msgBundle The bundle of translated messages, or null to use the messages from the
   *     Soy source.
   * @return The rendered text.
   * @deprecated Use {@link #newRenderer(SoyTemplateInfo)}.
   */
  @Deprecated
  String render(
      SoyTemplateInfo templateInfo, @Nullable SoyRecord data, @Nullable SoyMsgBundle msgBundle);


  /**
   * Renders a template.
   *
   * <p> Note: If you call this method instead of {@link #render(String, SoyRecord, SoyMsgBundle)},
   * your template data will be converted to a {@code SoyRecord} object on each call. This may not
   * be a big deal if you only need to use the data object once. But if you need to reuse the same
   * data object for multiple calls, it's more efficient to build your own {@code SoyRecord} object
   * and reuse it with {@link #render(String, SoyRecord, SoyMsgBundle)}.
   *
   * @param templateName The name of the template to render. If this SoyTofu instance is namespaced,
   *     then this parameter should be a partial name beginning with a dot (e.g. ".fooTemplate").
   * @param data The data to call the template with. Can be null if the template has no parameters.
   * @param msgBundle The bundle of translated messages, or null to use the messages from the
   *     Soy source.
   * @return The rendered text.
   * @deprecated Use {@link #newRenderer(String)}.
   */
  @Deprecated
  String render(
      String templateName, @Nullable Map<String, ?> data, @Nullable SoyMsgBundle msgBundle);


  /**
   * Renders a template.
   *
   * @param templateName The name of the template to render. If this SoyTofu instance is namespaced,
   *     then this parameter should be a partial name beginning with a dot (e.g. ".fooTemplate").
   * @param data The data to call the template with. Can be null if the template has no parameters.
   * @param msgBundle The bundle of translated messages, or null to use the messages from the
   *     Soy source.
   * @return The rendered text.
   * @deprecated Use {@link #newRenderer(String)}.
   */
  @Deprecated
  String render(
      String templateName, @Nullable SoyRecord data, @Nullable SoyMsgBundle msgBundle);

}
