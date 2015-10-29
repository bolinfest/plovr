/*
 * Copyright 2009 Google Inc.
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

package com.google.template.soy.jssrc.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.template.soy.jssrc.SoyJsSrcOptions;
import com.google.template.soy.jssrc.internal.GenJsExprsVisitor.GenJsExprsVisitorFactory;
import com.google.template.soy.jssrc.internal.TranslateToJsExprVisitor.TranslateToJsExprVisitorFactory;
import com.google.template.soy.jssrc.restricted.SoyJsSrcFunction;
import com.google.template.soy.jssrc.restricted.SoyJsSrcPrintDirective;
import com.google.template.soy.jssrc.restricted.SoyLibraryAssistedJsSrcFunction;
import com.google.template.soy.jssrc.restricted.SoyLibraryAssistedJsSrcPrintDirective;
import com.google.template.soy.passes.SharedPassesModule;
import com.google.template.soy.shared.internal.ApiCallScope;
import com.google.template.soy.shared.internal.FunctionAdapters;
import com.google.template.soy.shared.internal.GuiceSimpleScope;
import com.google.template.soy.shared.internal.SharedModule;
import com.google.template.soy.shared.restricted.SoyFunction;
import com.google.template.soy.shared.restricted.SoyPrintDirective;

import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;


/**
 * Guice module for the JS Source backend.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
public class JsSrcModule extends AbstractModule {


  @Override protected void configure() {

    // Install requisite modules.
    install(new SharedModule());
    install(new SharedPassesModule());

    // Bindings for when explicit dependencies are required.
    bind(JsSrcMain.class);
    bind(GenJsCodeVisitor.class);
    bind(OptimizeBidiCodeGenVisitor.class);
    bind(CanInitOutputVarVisitor.class);
    bind(GenCallCodeUtils.class);
    bind(IsComputableAsJsExprsVisitor.class);
    bind(JsExprTranslator.class);
    bind(GenDirectivePluginRequiresVisitor.class);

    // Bind providers of factories (created via assisted inject).
    install(new FactoryModuleBuilder().build(GenJsExprsVisitorFactory.class));
    install(new FactoryModuleBuilder().build(TranslateToJsExprVisitorFactory.class));

    // Bind unscoped providers for parameters in ApiCallScope (these throw exceptions).
    bind(SoyJsSrcOptions.class)
        .toProvider(GuiceSimpleScope.<SoyJsSrcOptions>getUnscopedProvider())
        .in(ApiCallScope.class);
  }


  /**
   * Builds and provides the map of SoyJsSrcFunctions (name to function).
   * @param soyFunctionsSet The installed set of SoyFunctions (from Guice Multibinder). Each
   *     SoyFunction may or may not implement SoyJsSrcFunction.
   */
  @Provides
  @Singleton
  Map<String, SoyJsSrcFunction> provideSoyJsSrcFunctionsMap(Set<SoyFunction> soyFunctionsSet) {

    return FunctionAdapters.buildSpecificSoyFunctionsMap(soyFunctionsSet, SoyJsSrcFunction.class);
  }


  /**
   * Builds and provides the map of SoyLibraryAssistedJsSrcFunctions (name to function).
   * @param soyFunctionsSet The installed set of SoyFunctions (from Guice Multibinder). Each
   *     SoyFunction may or may not implement SoyLibraryAssistedJsSrcFunction.
   */
  @Provides
  @Singleton
  Map<String, SoyLibraryAssistedJsSrcFunction> provideSoyLibraryAssistedJsSrcFunctionsMap(
      Set<SoyFunction> soyFunctionsSet) {

    return FunctionAdapters.buildSpecificSoyFunctionsMap(
        soyFunctionsSet, SoyLibraryAssistedJsSrcFunction.class);
  }


  /**
   * Builds and provides the map of SoyJsSrcDirectives (name to directive).
   * @param soyDirectivesSet The installed set of SoyPrintDirectives (from Guice Multibinder). Each
   *     SoyDirective may or may not implement SoyJsSrcPrintDirective.
   */
  @Provides
  @Singleton
  Map<String, SoyJsSrcPrintDirective> provideSoyJsSrcDirectivesMap(
      Set<SoyPrintDirective> soyDirectivesSet) {

    return FunctionAdapters.buildSpecificSoyDirectivesMap(
        soyDirectivesSet, SoyJsSrcPrintDirective.class);
  }


  /**
   * Builds and provides the map of SoyLibraryAssistedJsSrcDirectives (name to directive).
   * @param soyDirectivesSet The installed set of SoyPrintDirectives (from Guice Multibinder). Each
   *     SoyDirective may or may not implement SoyLibraryAssistedJsSrcDirectives.
   */
  @Provides
  @Singleton
  Map<String, SoyLibraryAssistedJsSrcPrintDirective> provideSoyLibraryAssistedJsSrcDirectivesMap(
      Set<SoyPrintDirective> soyDirectivesSet) {

    return FunctionAdapters.buildSpecificSoyDirectivesMap(
        soyDirectivesSet, SoyLibraryAssistedJsSrcPrintDirective.class);
  }

}
