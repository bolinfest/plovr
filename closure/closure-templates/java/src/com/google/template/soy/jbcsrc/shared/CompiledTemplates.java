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

package com.google.template.soy.jbcsrc.shared;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.template.soy.data.SanitizedContent.ContentKind;
import com.google.template.soy.jbcsrc.shared.TemplateMetadata.DelTemplateMetadata;
import com.google.template.soy.shared.internal.DelTemplateSelector;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

/**
 * The result of template compilation.
 */
public final class CompiledTemplates {
  private final ClassLoader loader;
  private final ConcurrentHashMap<String, TemplateData> templateNameToFactory = 
      new ConcurrentHashMap<>();
  private final DelTemplateSelector<TemplateData> selector;

  public CompiledTemplates(ImmutableSet<String> delTemplateNames) {
    this(delTemplateNames, CompiledTemplates.class.getClassLoader());
  }

  /**
   * @param delTemplateNames The names of all the compiled deltemplates (the mangled names).  This
   *     is needed to construct a valid deltemplate selector.
   * @param loader The classloader that contains the classes
   */
  public CompiledTemplates(ImmutableSet<String> delTemplateNames, ClassLoader loader) {
    this.loader = checkNotNull(loader);
    // We need to build the deltemplate selector eagerly.
    DelTemplateSelector.Builder<TemplateData> builder = new DelTemplateSelector.Builder<>();
    for (String delTemplateImplName : delTemplateNames) {
      TemplateData data = getTemplateData(delTemplateImplName);
      if (!data.delTemplateName.isPresent()) {
        throw new IllegalArgumentException(
            "Expected " + delTemplateImplName + " to be a deltemplate");
      }
      String delTemplateName = data.delTemplateName.get();
      if (data.delPackage.isPresent()) {
        String delpackage = data.delPackage.get();
        TemplateData prev = builder.add(delTemplateName, delpackage, data.variant, data);
        if (prev != null) {
          throw new IllegalArgumentException(
              String.format(
                  "Found multiple deltemplates with the same name (%s) and package (%s). %s and %s",
                  delTemplateName,
                  delpackage,
                  delTemplateImplName,
                  Names.soyTemplateNameFromJavaClassName(
                      prev.factory.getClass().getDeclaringClass().getName())));
        }
      } else {
        TemplateData prev = builder.addDefault(delTemplateName, data.variant, data);
        if (prev != null) {
          throw new IllegalArgumentException(
              String.format(
                  "Found multiple default deltemplates with the same name (%s). %s and %s",
                  delTemplateName,
                  delTemplateImplName,
                  Names.soyTemplateNameFromJavaClassName(
                      prev.factory.getClass().getDeclaringClass().getName())));
        }
      }
    }
    this.selector = builder.build();
  }

  /** Returns the strict content type of the template. */
  public Optional<ContentKind> getTemplateContentKind(String name) {
    return getTemplateData(name).kind;
  }

  /**
   * Returns a factory for the given fully qualified template name.
   */
  public CompiledTemplate.Factory getTemplateFactory(String name) {
    return getTemplateData(name).factory;
  }

  /**
   * Eagerly load all the given templates.
   */
  public void loadAll(Iterable<String> templateNames) {
    for (String templateName : templateNames) {
      getTemplateData(templateName);
    }
  }

  /**
   * Returns the transitive closure of all the injected params that might be used by this template.
   */
  public ImmutableSortedSet<String> getTransitiveIjParamsForTemplate(String templateName) {
    TemplateData templateData = getTemplateData(templateName);
    ImmutableSortedSet<String> transitiveIjParams = templateData.transitiveIjParams;
    // racy-lazy init pattern.  We may calculate this more than once, but that is fine because each
    // time should calculate the same value.
    if (transitiveIjParams != null) {
      // early return, we already calculated this.
      return transitiveIjParams;
    }
    Set<TemplateData> all = new HashSet<>();
    collectTransitiveCallees(templateData, all);
    ImmutableSortedSet.Builder<String> ijs = ImmutableSortedSet.naturalOrder();
    for (TemplateData callee : all) {
      ijs.addAll(callee.injectedParams);
    }
    transitiveIjParams = ijs.build();
    // save the results
    templateData.transitiveIjParams = transitiveIjParams;
    return transitiveIjParams;
  }

  /** Returns an active delegate for the given name, variant and active package selector. */
  @Nullable
  CompiledTemplate.Factory selectDelTemplate(
      String delTemplateName, String variant, Predicate<String> activeDelPackageSelector) {
    TemplateData selectedTemplate =
        selector.selectTemplate(delTemplateName, variant, activeDelPackageSelector);
    return selectedTemplate == null ? null : selectedTemplate.factory;
  }

  private TemplateData getTemplateData(String name) {
    checkNotNull(name);
    TemplateData template = templateNameToFactory.get(name);
    if (template == null) {
      template = loadFactory(name, loader);
      TemplateData old = templateNameToFactory.putIfAbsent(name, template);
      if (old != null) {
        return old;
      }
    }
    return template;
  }

  private static TemplateData loadFactory(String name, ClassLoader loader) {
    // We construct the factories via reflection to bridge the gap between generated and
    // non-generated code.  However, each factory only needs to be constructed once so the
    // reflective cost isn't paid on a per render basis.
    CompiledTemplate.Factory factory;
    try {
      String factoryName = Names.javaClassNameFromSoyTemplateName(name) + "$Factory";
      Class<? extends CompiledTemplate.Factory> factoryClass =
          Class.forName(factoryName, true /* run clinit */, loader)
              .asSubclass(CompiledTemplate.Factory.class);
      factory = factoryClass.newInstance();
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("No class was compiled for template: " + name, e);
    } catch (InstantiationException | IllegalAccessException e) {
      // this should be impossible since our factories are public with a default constructor.
      // TODO(lukes): failures of bytecode verification will propagate as Errors, we should
      // consider catching them here to add information about our generated types. (e.g. add the
      // class trace and a pointer on how to file a soy bug)
      throw new AssertionError(e);
    }
    return new TemplateData(factory);
  }


  /**
   * Adds all transitively called templates to {@code visited}
   */
  private void collectTransitiveCallees(TemplateData templateData, Set<TemplateData> visited) {
    if (!visited.add(templateData)) {
      return; // avoids chasing recursive cycles
    }
    for (String callee : templateData.callees) {
      collectTransitiveCallees(getTemplateData(callee), visited);
    }
    for (String delCallee : templateData.delCallees) {
      // for {delcalls} we consider all possible targets
      for (TemplateData potentialCallee : selector.delTemplateNameToValues().get(delCallee)) {
        collectTransitiveCallees(potentialCallee, visited);
      }
    }
  }

  /** This is mostly a copy of the {@link TemplateMetadata} annotation. */
  private static final class TemplateData {
    final CompiledTemplate.Factory factory;
    final Optional<ContentKind> kind;
    final ImmutableSet<String> callees;
    final ImmutableSet<String> delCallees;
    final ImmutableSet<String> injectedParams;

    // If this is a deltemplate then delTemplateName will be present
    final Optional<String> delTemplateName;
    final Optional<String> delPackage;
    final String variant;

    // Lazily initialized by getTransitiveIjParamsForTemplate.  We initialize lazily because in
    // general this is only needed for relatively few templates.
    ImmutableSortedSet<String> transitiveIjParams;

    TemplateData(CompiledTemplate.Factory factory) {
      this.factory = factory;
      // We pull the content kind off the templatemetadata eagerly since the parsing+reflection each
      // time is expensive.
      TemplateMetadata annotation =
          factory.getClass().getDeclaringClass().getAnnotation(TemplateMetadata.class);
      String contentKind = annotation.contentKind();
      this.kind =
          contentKind.isEmpty()
              ? Optional.<ContentKind>absent()
              : Optional.of(ContentKind.valueOf(contentKind));
      this.callees = ImmutableSet.copyOf(annotation.callees());
      this.delCallees = ImmutableSet.copyOf(annotation.delCallees());
      this.injectedParams = ImmutableSet.copyOf(annotation.injectedParams());
      DelTemplateMetadata deltemplateMetadata = annotation.deltemplateMetadata();
      variant = deltemplateMetadata.variant();
      if (!deltemplateMetadata.name().isEmpty()) {
        delTemplateName = Optional.of(deltemplateMetadata.name());
        delPackage =
            deltemplateMetadata.delPackage().isEmpty()
                ? Optional.<String>absent()
                : Optional.of(deltemplateMetadata.delPackage());
      } else {
        this.delTemplateName = Optional.absent();
        this.delPackage = Optional.absent();
      }
    }
  }
}
