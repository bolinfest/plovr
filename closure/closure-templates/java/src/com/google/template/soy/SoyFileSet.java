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

package com.google.template.soy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharSource;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.util.Providers;
import com.google.template.soy.SoyFileSetParser.ParseResult;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.base.internal.SoyFileKind;
import com.google.template.soy.base.internal.SoyFileSupplier;
import com.google.template.soy.base.internal.VolatileSoyFileSupplier;
import com.google.template.soy.basetree.SyntaxVersion;
import com.google.template.soy.conformance.CheckConformance;
import com.google.template.soy.conformance.ConformanceInput;
import com.google.template.soy.error.ErrorPrettyPrinter;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.ErrorReporter.Checkpoint;
import com.google.template.soy.error.SnippetFormatter;
import com.google.template.soy.incrementaldomsrc.IncrementalDomSrcMain;
import com.google.template.soy.jbcsrc.BytecodeCompiler;
import com.google.template.soy.jbcsrc.api.SoySauce;
import com.google.template.soy.jbcsrc.api.SoySauceImpl;
import com.google.template.soy.jbcsrc.shared.CompiledTemplate;
import com.google.template.soy.jbcsrc.shared.CompiledTemplates;
import com.google.template.soy.jssrc.SoyJsSrcOptions;
import com.google.template.soy.jssrc.internal.JsSrcMain;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.SoyMsgBundleHandler;
import com.google.template.soy.msgs.internal.ExtractMsgsVisitor;
import com.google.template.soy.msgs.restricted.SoyMsg;
import com.google.template.soy.msgs.restricted.SoyMsgBundleImpl;
import com.google.template.soy.parseinfo.passes.GenerateParseInfoVisitor;
import com.google.template.soy.parsepasses.contextautoesc.ContentSecurityPolicyPass;
import com.google.template.soy.parsepasses.contextautoesc.ContextualAutoescaper;
import com.google.template.soy.parsepasses.contextautoesc.DerivedTemplateUtils;
import com.google.template.soy.passes.ChangeCallsToPassAllDataVisitor;
import com.google.template.soy.passes.ClearSoyDocStringsVisitor;
import com.google.template.soy.passes.FindIjParamsVisitor;
import com.google.template.soy.passes.FindIjParamsVisitor.IjParamsInfo;
import com.google.template.soy.passes.FindTransitiveDepTemplatesVisitor;
import com.google.template.soy.passes.FindTransitiveDepTemplatesVisitor.TransitiveDepTemplatesInfo;
import com.google.template.soy.passes.PassManager;
import com.google.template.soy.pysrc.SoyPySrcOptions;
import com.google.template.soy.pysrc.internal.PySrcMain;
import com.google.template.soy.shared.SoyAstCache;
import com.google.template.soy.shared.SoyGeneralOptions;
import com.google.template.soy.shared.internal.MainEntryPointUtils;
import com.google.template.soy.shared.restricted.SoyFunction;
import com.google.template.soy.shared.restricted.SoyJavaFunction;
import com.google.template.soy.shared.restricted.SoyJavaPrintDirective;
import com.google.template.soy.shared.restricted.SoyPrintDirective;
import com.google.template.soy.sharedpasses.opti.SimplifyVisitor;
import com.google.template.soy.soytree.SoyFileNode;
import com.google.template.soy.soytree.SoyFileSetNode;
import com.google.template.soy.soytree.SoytreeUtils;
import com.google.template.soy.soytree.TemplateDelegateNode;
import com.google.template.soy.soytree.TemplateNode;
import com.google.template.soy.soytree.TemplateRegistry;
import com.google.template.soy.soytree.Visibility;
import com.google.template.soy.tofu.SoyTofu;
import com.google.template.soy.tofu.internal.BaseTofu.BaseTofuFactory;
import com.google.template.soy.types.SoyTypeRegistry;
import com.google.template.soy.xliffmsgplugin.XliffMsgPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a complete set of Soy files for compilation as one bundle. The files may depend on
 * each other but should not have dependencies outside of the set.
 *
 * <p> Note: Soy file (or resource) contents must be encoded in UTF-8.
 *
 */
public final class SoyFileSet {


  /**
   * Creates a builder with the standard set of Soy directives, functions, and types.
   *
   * <p>If you need additional directives, functions, or types, create the Builder instance using
   * Guice.  If your project doesn't otherwise use Guice, you can just use Guice.createInjector
   * with only the modules you need, similar to the implementation of this method.
   */
  @SuppressWarnings("deprecation")
  public static Builder builder() {
    Builder builder = new Builder();
    // We inject based on a plain SoyModule, rather than using GuiceInitializer, to avoid relying
    // on whatever lingering static state is around.
    builder.setFactory(
        Guice.createInjector(new SoyModule()).getInstance(SoyFileSetFactory.class));
    return builder;
  }

  /**
   * Builder for a {@code SoyFileSet}.
   */
  public static final class Builder {

    /**
     * Assisted-injection factory. This is optionally injected since many clients inject
     * SoyFileSet.Builder without installing a SoyModule, in which case we need to fall back to
     * static injection.
     */
    private SoyFileSetFactory factory;

    /**
     * The SoyFileSuppliers collected so far in added order, as a set to prevent dupes.
     */
    private final ImmutableSet.Builder<SoyFileSupplier> setBuilder;

    /** Optional AST cache. */
    private SoyAstCache cache;

    /** The general compiler options. */
    private SoyGeneralOptions lazyGeneralOptions;

    /** Type registry for this fileset only. */
    private SoyTypeRegistry localTypeRegistry;

    /**
     * Constructs a builder using a statically-injected configuration.
     *
     * @deprecated Use the static SoyFileSet.builder() method, or  inject SoyFileSet.Builder
     *     using Guice with SoyModule installed. The behavior of this builder is unpredictable and
     *     will use the Soy configuration from the most recently configured Injector containing a
     *     SoyModule, because it relies on Guice's static injection.
     */
    @Inject
    @Deprecated
    public Builder() {
      this.setBuilder = ImmutableSet.builder();
      this.cache = null;
      this.lazyGeneralOptions = null;
    }

    @Inject(optional = true)
    /** Assigns the factory via Guice. */
    void setFactory(SoyFileSetFactory factory) {
      // Yay, we have Guice, and SoyModule is installed! :-) Inject the factory from the relevant
      // Injector!
      this.factory = factory;
    }

    /**
     * Sets all Soy general options.
     *
     * <p>This must be called before any other setters.
     */
    public void setGeneralOptions(SoyGeneralOptions generalOptions) {
      Preconditions.checkState(lazyGeneralOptions == null,
          "Call SoyFileSet#setGeneralOptions before any other setters.");
      Preconditions.checkNotNull(generalOptions, "Non-null argument expected.");
      lazyGeneralOptions = generalOptions.clone();
    }

    /**
     * Returns and/or lazily-creates the SoyGeneralOptions for this builder.
     *
     * <p>Laziness is an important feature to ensure that setGeneralOptions can fail if options were
     * already set.  Otherwise, it'd be easy to set some options on this builder and overwrite them
     * by calling setGeneralOptions.
     */
    private SoyGeneralOptions getGeneralOptions() {
      if (lazyGeneralOptions == null) {
        lazyGeneralOptions = new SoyGeneralOptions();
      }
      return lazyGeneralOptions;
    }


    /**
     * Builds the new {@code SoyFileSet}.
     * @return The new {@code SoyFileSet}.
     */
    public SoyFileSet build() {
      if (factory == null) {
        factory = GuiceInitializer.getHackySoyFileSetFactory();
      }
      return factory.create(
          setBuilder.build().asList(), cache, getGeneralOptions(), localTypeRegistry);
    }


    /**
     * Adds an input Soy file, given a {@code CharSource} for the file content, as well as the
     * desired file path for messages.
     *
     * @param contentSource Source for the Soy file content.
     * @param soyFileKind The kind of this input Soy file.
     * @param filePath The path to the Soy file (used for messages only).
     * @return This builder.
     */
    public Builder addWithKind(
        CharSource contentSource, SoyFileKind soyFileKind, String filePath) {
      setBuilder.add(SoyFileSupplier.Factory.create(contentSource, soyFileKind, filePath));
      return this;
    }


    /**
     * Adds an input Soy file, given a {@code CharSource} for the file content, as well as the
     * desired file path for messages.
     *
     * @param contentSource Source for the Soy file content.
     * @param filePath The path to the Soy file (used for messages only).
     * @return This builder.
     */
    public Builder add(CharSource contentSource, String filePath) {
      return addWithKind(contentSource, SoyFileKind.SRC, filePath);
    }


    /**
     * Adds an input Soy file, given a {@code File}.
     *
     * @param inputFile The Soy file.
     * @param soyFileKind The kind of this input Soy file.
     * @return This builder.
     */
    public Builder addWithKind(File inputFile, SoyFileKind soyFileKind) {
      setBuilder.add(SoyFileSupplier.Factory.create(inputFile, soyFileKind));
      return this;
    }


    /**
     * Adds an input Soy file, given a {@code File}.
     *
     * @param inputFile The Soy file.
     * @return This builder.
     */
    public Builder add(File inputFile) {
      return addWithKind(inputFile, SoyFileKind.SRC);
    }


    /**
     * Adds an input Soy file that supports checking for modifications, given a {@code File}.
     *
     * <p>Note: This does nothing by itself. It should be used in conjunction with a feature that
     * actually checks for volatile files. Currently, that feature is
     * {@link #setSoyAstCache(SoyAstCache)}.
     *
     * @param inputFile The Soy file.
     * @param soyFileKind The kind of this input Soy file.
     * @return This builder.
     */
    public Builder addVolatileWithKind(File inputFile, SoyFileKind soyFileKind) {
      setBuilder.add(new VolatileSoyFileSupplier(inputFile, soyFileKind));
      return this;
    }


    /**
     * Adds an input Soy file that supports checking for modifications, given a {@code File}.
     *
     * <p>Note: This does nothing by itself. It should be used in conjunction with a feature that
     * actually checks for volatile files. Currently, that feature is
     * {@link #setSoyAstCache(SoyAstCache)}.
     *
     * @param inputFile The Soy file.
     * @return This builder.
     */
    public Builder addVolatile(File inputFile) {
      return addVolatileWithKind(inputFile, SoyFileKind.SRC);
    }


    /**
     * Adds an input Soy file, given a resource {@code URL}, as well as the desired file path for
     * messages.
     *
     * @param inputFileUrl The Soy file.
     * @param soyFileKind The kind of this input Soy file.
     * @param filePath The path to the Soy file (used for messages only).
     * @return This builder.
     */
    public Builder addWithKind(URL inputFileUrl, SoyFileKind soyFileKind, String filePath) {
      setBuilder.add(SoyFileSupplier.Factory.create(inputFileUrl, soyFileKind, filePath));
      return this;
    }


    /**
     * Adds an input Soy file, given a resource {@code URL}, as well as the desired file path for
     * messages.
     *
     * @param inputFileUrl The Soy file.
     * @param filePath The path to the Soy file (used for messages only).
     * @return This builder.
     */
    public Builder add(URL inputFileUrl, String filePath) {
      return addWithKind(inputFileUrl, SoyFileKind.SRC, filePath);
    }


    /**
     * Adds an input Soy file, given a resource {@code URL}.
     *
     * <p> Important: This function assumes that the desired file path is returned by
     * {@code inputFileUrl.toString()}. If this is not the case, please use
     * {@link #addWithKind(URL, SoyFileKind, String)} instead.
     *
     * @see #addWithKind(URL, SoyFileKind, String)
     * @param inputFileUrl The Soy file.
     * @param soyFileKind The kind of this input Soy file.
     * @return This builder.
     */
    public Builder addWithKind(URL inputFileUrl, SoyFileKind soyFileKind) {
      setBuilder.add(SoyFileSupplier.Factory.create(inputFileUrl, soyFileKind));
      return this;
    }


    /**
     * Adds an input Soy file, given a resource {@code URL}.
     *
     * <p> Important: This function assumes that the desired file path is returned by
     * {@code inputFileUrl.toString()}. If this is not the case, please use
     * {@link #add(URL, String)} instead.
     *
     * @see #add(URL, String)
     * @param inputFileUrl The Soy file.
     * @return This builder.
     */
    public Builder add(URL inputFileUrl) {
      return addWithKind(inputFileUrl, SoyFileKind.SRC);
    }


    /**
     * Adds an input Soy file, given the file content provided as a string, as well as the desired
     * file path for messages.
     *
     * @param content The Soy file content.
     * @param soyFileKind The kind of this input Soy file.
     * @param filePath The path to the Soy file (used for messages only).
     * @return This builder.
     */
    public Builder addWithKind(CharSequence content, SoyFileKind soyFileKind, String filePath) {
      setBuilder.add(SoyFileSupplier.Factory.create(content, soyFileKind, filePath));
      return this;
    }


    /**
     * Adds an input Soy file, given the file content provided as a string, as well as the desired
     * file path for messages.
     *
     * @param content The Soy file content.
     * @param filePath The path to the Soy file (used for messages only).
     * @return This builder.
     */
    public Builder add(CharSequence content, String filePath) {
      return addWithKind(content, SoyFileKind.SRC, filePath);
    }


    /**
     * Configures to use an AST cache to speed up development time.
     *
     * <p>This is undesirable in production mode since it uses strictly more memory, and this only
     * helps if the same templates are going to be recompiled frequently.
     *
     * @param cache The cache to use, which can have a lifecycle independent of the SoyFileSet.
     *     Null indicates not to use a cache.
     * @return This builder.
     */
    public Builder setSoyAstCache(SoyAstCache cache) {
      this.cache = cache;
      return this;
    }


    /**
     * Sets the user-declared syntax version name for the Soy file bundle.
     * @param versionName The syntax version name, e.g. "1.0", "2.0", "2.3".
     */
    public Builder setDeclaredSyntaxVersionName(@Nonnull String versionName) {
      getGeneralOptions().setDeclaredSyntaxVersionName(versionName);
      return this;
    }


    /**
     * Sets whether to allow external calls (calls to undefined templates).
     *
     * @param allowExternalCalls Whether to allow external calls (calls to undefined templates).
     * @return This builder.
     */
    public Builder setAllowExternalCalls(boolean allowExternalCalls) {
      getGeneralOptions().setAllowExternalCalls(allowExternalCalls);
      return this;
    }


    /**
     * Sets whether to force strict autoescaping. Enabling will cause compile time exceptions if
     * non-strict autoescaping is used in namespaces or templates.
     *
     * @param strictAutoescapingRequired Whether strict autoescaping is required.
     * @return This builder.
     */
    public Builder setStrictAutoescapingRequired(boolean strictAutoescapingRequired) {
      getGeneralOptions().setStrictAutoescapingRequired(strictAutoescapingRequired);
      return this;
    }


    /**
     * Sets the map from compile-time global name to value.
     *
     * <p> The values can be any of the Soy primitive types: null, boolean, integer, float (Java
     * double), or string.
     *
     * @param compileTimeGlobalsMap Map from compile-time global name to value. The values can be
     *     any of the Soy primitive types: null, boolean, integer, float (Java double), or string.
     * @return This builder.
     * @throws SoySyntaxException If one of the values is not a valid Soy primitive type.
     */
    public Builder setCompileTimeGlobals(Map<String, ?> compileTimeGlobalsMap) {
      getGeneralOptions().setCompileTimeGlobals(compileTimeGlobalsMap);
      return this;
    }


    /**
     * Sets the file containing compile-time globals.
     *
     * <p> Each line of the file should have the format
     * <pre>
     *     &lt;global_name&gt; = &lt;primitive_data&gt;
     * </pre>
     * where primitive_data is a valid Soy expression literal for a primitive type (null, boolean,
     * integer, float, or string). Empty lines and lines beginning with "//" are ignored. The file
     * should be encoded in UTF-8.
     *
     * <p> If you need to generate a file in this format from Java, consider using the utility
     * {@code SoyUtils.generateCompileTimeGlobalsFile()}.
     *
     * @param compileTimeGlobalsFile The file containing compile-time globals.
     * @return This builder.
     * @throws IOException If there is an error reading the compile-time globals file.
     */
    public Builder setCompileTimeGlobals(File compileTimeGlobalsFile) throws IOException {
      getGeneralOptions().setCompileTimeGlobals(compileTimeGlobalsFile);
      return this;
    }


    /**
     * Sets the resource file containing compile-time globals.
     *
     * <p> Each line of the file should have the format
     * <pre>
     *     &lt;global_name&gt; = &lt;primitive_data&gt;
     * </pre>
     * where primitive_data is a valid Soy expression literal for a primitive type (null, boolean,
     * integer, float, or string). Empty lines and lines beginning with "//" are ignored. The file
     * should be encoded in UTF-8.
     *
     * <p> If you need to generate a file in this format from Java, consider using the utility
     * {@code SoyUtils.generateCompileTimeGlobalsFile()}.
     *
     * @param compileTimeGlobalsResource The resource containing compile-time globals.
     * @return This builder.
     * @throws IOException If there is an error reading the compile-time globals file.
     */
    public Builder setCompileTimeGlobals(URL compileTimeGlobalsResource) throws IOException {
      getGeneralOptions().setCompileTimeGlobals(compileTimeGlobalsResource);
      return this;
    }


    /**
     * Pass true to enable CSP (Content Security Policy) support which adds an extra pass that marks
     * inline scripts in templates specially so the browser can distinguish scripts written by
     * trusted template authors from scripts injected via XSS.
     * <p>
     * Scripts are marked using a per-page-render secret stored in the injected variable
     * {@code $ij.csp_nonce}.
     * Scripts in non-contextually auto-escaped templates may not be found.
     */
    public Builder setSupportContentSecurityPolicy(boolean supportContentSecurityPolicy) {
      getGeneralOptions().setSupportContentSecurityPolicy(supportContentSecurityPolicy);
      return this;
    }


    /**
     * Override the global type registry with one that is local to this file set.
     */
    public Builder setLocalTypeRegistry(SoyTypeRegistry typeRegistry) {
      localTypeRegistry = typeRegistry;
      return this;
    }
  }


  /**
   * Injectable factory for creating an instance of this class.
   */
  public interface SoyFileSetFactory {

    /**
     * @param soyFileSuppliers The suppliers for the input Soy files.
     * @param cache Optional (nullable) AST cache for faster recompile times.
     * @param options The general compiler options.
     */
    SoyFileSet create(
        List<SoyFileSupplier> soyFileSuppliers,
        SoyAstCache cache,
        SoyGeneralOptions options,
        @Assisted("localTypeRegistry") SoyTypeRegistry localTypeRegistry);
  }


  /** Default SoyMsgBundleHandler uses the XLIFF message plugin. */
  private static final Provider<SoyMsgBundleHandler> DEFAULT_SOY_MSG_BUNDLE_HANDLER_PROVIDER =
      Providers.of(new SoyMsgBundleHandler(new XliffMsgPlugin()));


  /** Provider for getting an instance of SoyMsgBundleHandler. */
  private Provider<SoyMsgBundleHandler> msgBundleHandlerProvider;

  /** Factory for creating an instance of BaseTofu. */
  private final BaseTofuFactory baseTofuFactory;

  /** Factory for creating an instance of BaseTofu. */
  private final SoySauceImpl.Factory soyTemplatesFactory;

  /** Provider for getting an instance of JsSrcMain. */
  private final Provider<JsSrcMain> jsSrcMainProvider;

  /** Provider for getting an instance of IncrementalDomSrcMain. */
  private final Provider<IncrementalDomSrcMain> incrementalDomSrcMainProvider;

  /** Provider for getting an instance of PySrcMain. */
  private final Provider<PySrcMain> pySrcMainProvider;

  /** The instance of ContextualAutoescaper to use. */
  private final ContextualAutoescaper contextualAutoescaper;

  /** The instance of SimplifyVisitor to use. */
  private final SimplifyVisitor simplifyVisitor;

  /** The type registry for resolving type names. */
  private final SoyTypeRegistry typeRegistry;

  /** The suppliers for the input Soy files. */
  private final List<SoyFileSupplier> soyFileSuppliers;

  /** Optional soy tree cache for faster recompile times. */
  private final SoyAstCache cache;

  /** The general compiler options. */
  private final SoyGeneralOptions generalOptions;

  private CheckConformance checkConformance;

  /** For private use by pruneTranslatedMsgs(). */
  private ImmutableSet<Long> memoizedExtractedMsgIdsForPruning;

  private final ImmutableMap<String, ? extends SoyFunction> soyFunctionMap;
  private final ImmutableMap<String, ? extends SoyPrintDirective> printDirectives;

  /** For reporting errors during parsing. */
  private final ErrorReporter errorReporter;


  /**
   * @param baseTofuFactory Factory for creating an instance of BaseTofu.
   * @param jsSrcMainProvider Provider for getting an instance of JsSrcMain.
   * @param incrementalDomSrcMainProvider Provider for getting an instance of IncrementalDomSrcMain.
   * @param pySrcMainProvider Provider for getting an instance of PySrcMain.
   * @param contextualAutoescaper The instance of ContextualAutoescaper to use.
   * @param simplifyVisitor The instance of SimplifyVisitor to use.
   * @param typeRegistry The type registry to resolve parameter type names.
   * @param soyFileSuppliers The suppliers for the input Soy files.
   * @param generalOptions The general compiler options.
   * @param localTypeRegistry If non-null, use this local type registry instead
   *        of the typeRegistry param which is a global singleton.
   *        (Unfortunately because of the way assisted injection works, we need
   *        the global and local registries to be separate parameters).
   */
  @Inject
  SoyFileSet(
      BaseTofuFactory baseTofuFactory,
      SoySauceImpl.Factory soyTemplatesFactory,
      Provider<JsSrcMain> jsSrcMainProvider,
      Provider<IncrementalDomSrcMain> incrementalDomSrcMainProvider,
      Provider<PySrcMain> pySrcMainProvider,
      ContextualAutoescaper contextualAutoescaper,
      SimplifyVisitor simplifyVisitor,
      SoyTypeRegistry typeRegistry,
      ImmutableMap<String, ? extends SoyFunction> soyFunctionMap,
      ImmutableMap<String, ? extends SoyPrintDirective> printDirectives,
      ErrorReporter errorReporter,
      @Assisted List<SoyFileSupplier> soyFileSuppliers,
      @Assisted SoyGeneralOptions generalOptions,
      @Assisted @Nullable SoyAstCache cache,
      @Assisted("localTypeRegistry") @Nullable SoyTypeRegistry localTypeRegistry) {

    // Default value is optionally replaced using method injection.
    this.msgBundleHandlerProvider = DEFAULT_SOY_MSG_BUNDLE_HANDLER_PROVIDER;
    this.soyTemplatesFactory = soyTemplatesFactory;
    this.baseTofuFactory = baseTofuFactory;
    this.jsSrcMainProvider = jsSrcMainProvider;
    this.incrementalDomSrcMainProvider = incrementalDomSrcMainProvider;
    this.pySrcMainProvider = pySrcMainProvider;
    this.contextualAutoescaper = contextualAutoescaper;
    this.simplifyVisitor = simplifyVisitor;

    Preconditions.checkArgument(
        !soyFileSuppliers.isEmpty(), "Must have non-zero number of input Soy files.");
    this.typeRegistry = localTypeRegistry != null ? localTypeRegistry : typeRegistry;
    this.soyFileSuppliers = soyFileSuppliers;
    this.cache = cache;
    this.generalOptions = generalOptions.clone();
    this.soyFunctionMap = soyFunctionMap;
    this.printDirectives = printDirectives;
    this.errorReporter = errorReporter;
  }


  /** @param msgBundleHandlerProvider Provider for getting an instance of SoyMsgBundleHandler. */
  @Inject(optional = true)
  void setMsgBundleHandlerProvider(Provider<SoyMsgBundleHandler> msgBundleHandlerProvider) {
    this.msgBundleHandlerProvider = msgBundleHandlerProvider;
  }

  @Inject(optional = true)
  void setCheckConformance(CheckConformance checkConformance) {
    this.checkConformance = checkConformance;
  }

  /** Returns the list of suppliers for the input Soy files. For testing use only! */
  @VisibleForTesting List<SoyFileSupplier> getSoyFileSuppliersForTesting() {
    return soyFileSuppliers;
  }

  /** Returns the general compiler options. For testing use only! */
  @VisibleForTesting SoyGeneralOptions getOptionsForTesting() {
    return generalOptions;
  }

  /** TODO(user): workaround for {@link SoyJsSrcResource}. Remove. */
  ErrorReporter getErrorReporter() {
    return errorReporter;
  }

  /**
   * Generates Java classes containing parse info (param names, template names, meta info). There
   * will be one Java class per Soy file.
   *
   * @param javaPackage The Java package for the generated classes.
   * @param javaClassNameSource Source of the generated class names. Must be one of "filename",
   *     "namespace", or "generic".
   * @return A map from generated file name (of the form "<*>SoyInfo.java") to generated file
   *     content.
   */
  ParseInfo generateParseInfo(String javaPackage, String javaClassNameSource) {

    // TODO(lukes): see if we can enforce that globals are provided at compile time here. given that
    // types have to be, this should be possible.  Currently it is disabled for backwards
    // compatibility
    ParseResult result =
        parse(
            SyntaxVersion.V2_0,
            true /* allow unknown globals */,
            true /* allow unknown functions */,
            typeRegistry,
            soyFunctionMap);

    // Do renaming of package-relative class names.
    ImmutableMap<String, String> parseInfo =
        new GenerateParseInfoVisitor(javaPackage, javaClassNameSource, result.registry())
            .exec(result.fileSet());
    return new ParseInfo(result(), parseInfo);
  }

  static final class ParseInfo {
    final CompilationResult result;
    final ImmutableMap<String, String> generatedFiles;

    ParseInfo(CompilationResult result, ImmutableMap<String, String> generatedFiles) {
      this.result = result;
      this.generatedFiles = generatedFiles;
    }
  }

  /**
   * Extracts all messages from this Soy file set into a SoyMsgBundle (which can then be turned
   * into an extracted messages file with the help of a SoyMsgBundleHandler).
   *
   * @return A SoyMsgBundle containing all the extracted messages (locale "en").
   * @throws SoySyntaxException If a syntax error is found.
   */
  public SoyMsgBundle extractMsgs() throws SoySyntaxException {
    SoyFileSetNode soyTree =
        parse(
                SyntaxVersion.V1_0,
                true /* allow unknown globals */,
                true /* allow unknown functions */,
                SoyTypeRegistry.DEFAULT_UNKNOWN,
                soyFunctionMap)
            .fileSet();
    return new ExtractMsgsVisitor().exec(soyTree);
  }


  /**
   * Prunes messages from a given message bundle, keeping only messages used in this Soy file set.
   *
   * <p> Important: Do not use directly. This is subject to change and your code will break.
   *
   * <p> Note: This method memoizes intermediate results to improve efficiency in the case that it
   * is called multiple times (which is a common case). Thus, this method will not work correctly if
   * the underlying Soy files are modified between calls to this method.
   *
   * @param origTransMsgBundle The message bundle to prune.
   * @return The pruned message bundle.
   * TODO(brndn): Instead of throwing, should return a structure with a list of errors that callers
   * can inspect.
   */
  public SoyMsgBundle pruneTranslatedMsgs(SoyMsgBundle origTransMsgBundle)
      throws SoySyntaxException {

    // ------ Extract msgs from all the templates reachable from public templates. ------
    // Note: In the future, instead of using all public templates as the root set, we can allow the
    // user to provide a root set.

    if (memoizedExtractedMsgIdsForPruning == null) {
      ParseResult result =
          parse(
              SyntaxVersion.V1_0,
              true /* allow unknown globals */,
              true /* allow unknown functions */,
              SoyTypeRegistry.DEFAULT_UNKNOWN,
              soyFunctionMap);

      SoyFileSetNode soyTree = result.fileSet();
      TemplateRegistry registry = result.registry();

      List<TemplateNode> allPublicTemplates = Lists.newArrayList();
      for (SoyFileNode soyFile : soyTree.getChildren()) {
        for (TemplateNode template : soyFile.getChildren()) {
          if (template.getVisibility() == Visibility.PUBLIC) {
            allPublicTemplates.add(template);
          }
        }
      }
      Map<TemplateNode, TransitiveDepTemplatesInfo> depsInfoMap =
          new FindTransitiveDepTemplatesVisitor(registry)
              .execOnMultipleTemplates(allPublicTemplates);
      TransitiveDepTemplatesInfo mergedDepsInfo =
          TransitiveDepTemplatesInfo.merge(depsInfoMap.values());

      SoyMsgBundle extractedMsgBundle = new ExtractMsgsVisitor()
          .execOnMultipleNodes(mergedDepsInfo.depTemplateSet);

      ImmutableSet.Builder<Long> extractedMsgIdsBuilder = ImmutableSet.builder();
      for (SoyMsg extractedMsg : extractedMsgBundle) {
        extractedMsgIdsBuilder.add(extractedMsg.getId());
      }
      memoizedExtractedMsgIdsForPruning = extractedMsgIdsBuilder.build();
    }

    // ------ Prune. ------

    ImmutableList.Builder<SoyMsg> prunedTransMsgsBuilder = ImmutableList.builder();
    for (SoyMsg transMsg : origTransMsgBundle) {
      if (memoizedExtractedMsgIdsForPruning.contains(transMsg.getId())) {
        prunedTransMsgsBuilder.add(transMsg);
      }
    }
    return new SoyMsgBundleImpl(
        origTransMsgBundle.getLocaleString(), prunedTransMsgsBuilder.build());
  }

  /**
   * Compiles this Soy file set into a Java object (type {@code SoyTofu}) capable of rendering the
   * compiled templates.
   *
   * @return The resulting {@code SoyTofu} object.
   * @throws SoySyntaxException If a syntax error is found.
   */
  public SoyTofu compileToTofu() throws SoySyntaxException {
    ServerCompilationPrimitives primitives = compileForServerRendering();
    return doCompileToTofu(primitives);
  }

  /** Helper method to compile SoyTofu from {@link ServerCompilationPrimitives} */
  private SoyTofu doCompileToTofu(ServerCompilationPrimitives primitives) {
    return baseTofuFactory.create(
        primitives.registry,
        primitives.transitiveIjs,
        primitives.printDirectives);
  }

  /**
   * This is an <em>extremely experimental API</em> and subject to change.  Not all features of soy
   * are implemented in this new backend and the features that are implemented are not necessarily
   * correct!
   *
   * <p>See com/google/template/soy/jbcsrc/README.md for background on this new backend.
   *
   * <p>Compiles this Soy file set into a set of java classes implementing the
   * {@link CompiledTemplate} interface.
   *
   * @return A set of compiled templates
   * @throws SoySyntaxException If a syntax error is found.
   */
  public SoySauce compileTemplates() throws SoySyntaxException {
    ServerCompilationPrimitives primitives = compileForServerRendering();
    return doCompileSoySauce(primitives);
  }

  /** Helper method to compile SoySauce from {@link ServerCompilationPrimitives} */
  private SoySauce doCompileSoySauce(ServerCompilationPrimitives primitives) {
    Optional<CompiledTemplates> templates =
        BytecodeCompiler.compile(
            primitives.registry,
            // if there is an AST cache, assume we are in 'dev mode' and trigger lazy compilation.
            cache != null,
            errorReporter);

    ((ErrorReporterImpl) errorReporter).throwIfErrorsPresent();

    // SoySauce has no need for SoyFunctions that are not SoyJavaFunctions
    // (it generates Java source code implementing BuiltinFunctions).
    // Filter them out.
    ImmutableMap.Builder<String, SoyJavaFunction> soyJavaFunctions = ImmutableMap.builder();
    for (Map.Entry<String, ? extends SoyFunction> entry : primitives.soyFunctions.entrySet()) {
      SoyFunction function = entry.getValue();
      if (function instanceof SoyJavaFunction) {
        soyJavaFunctions.put(entry.getKey(), (SoyJavaFunction) function);
      }
    }

    // SoySauce has no need for SoyPrintDirectives that are not SoyJavaPrintDirectives.
    // Filter them out.
    ImmutableMap.Builder<String, SoyJavaPrintDirective> soyJavaPrintDirectives =
        ImmutableMap.builder();
    for (Map.Entry<String, ? extends SoyPrintDirective> entry :
        primitives.printDirectives.entrySet()) {
      SoyPrintDirective printDirective = entry.getValue();
      if (printDirective instanceof SoyJavaPrintDirective) {
        soyJavaPrintDirectives.put(entry.getKey(), (SoyJavaPrintDirective) printDirective);
      }
    }

    return soyTemplatesFactory.create(
        templates.get(),
        primitives.registry,
        new ExtractMsgsVisitor().exec(primitives.soyTree),
        soyJavaFunctions.build(),
        soyJavaPrintDirectives.build(),
        primitives.transitiveIjs);
  }

  /**
   * A tuple of the outputs of shared compiler passes that are needed to produce SoyTofu or
   * SoySauce.
   */
  private static final class ServerCompilationPrimitives {
    final SoyFileSetNode soyTree;
    final ImmutableMap<String, ImmutableSortedSet<String>> transitiveIjs;
    final ImmutableMap<String, ? extends SoyFunction> soyFunctions;
    final ImmutableMap<String, ? extends SoyPrintDirective> printDirectives;
    final TemplateRegistry registry;

    ServerCompilationPrimitives(
        SoyFileSetNode soyTree,
        TemplateRegistry registry,
        ImmutableMap<String, ImmutableSortedSet<String>> transitiveIjs,
        ImmutableMap<String, ? extends SoyFunction> soyFunctions,
        ImmutableMap<String, ? extends SoyPrintDirective> printDirectives) {
      this.soyTree = soyTree;
      this.registry = registry;
      this.transitiveIjs = transitiveIjs;
      this.soyFunctions = soyFunctions;
      this.printDirectives = printDirectives;
    }
  }

  private ServerCompilationPrimitives compileForServerRendering() {
    ParseResult result = parse(SyntaxVersion.V2_0);
    ((ErrorReporterImpl) errorReporter).throwIfErrorsPresent();

    SoyFileSetNode soyTree = result.fileSet();
    TemplateRegistry registry = result.registry();
    registry = runMiddleendPasses(registry, soyTree);

    // Clear the SoyDoc strings because they use unnecessary memory, unless we have a cache, in
    // which case it is pointless.
    if (cache == null) {
      new ClearSoyDocStringsVisitor().exec(soyTree);
    }

    ((ErrorReporterImpl) errorReporter).throwIfErrorsPresent();
    ImmutableMap<String, ImmutableSortedSet<String>> transitiveIjs =
        getTransitiveIjs(soyTree, registry);
    return new ServerCompilationPrimitives(
        soyTree, registry, transitiveIjs, soyFunctionMap, printDirectives);
  }

  private ImmutableMap<String, ImmutableSortedSet<String>> getTransitiveIjs(
      SoyFileSetNode soyTree, TemplateRegistry registry) {
    ImmutableMap<TemplateNode, IjParamsInfo> templateToIjParamsInfoMap =
        new FindIjParamsVisitor(registry)
            .execOnAllTemplates(soyTree);
    ImmutableMap.Builder<String, ImmutableSortedSet<String>> templateToTransitiveIjParams =
        ImmutableMap.builder();
    for (Map.Entry<TemplateNode, IjParamsInfo> entry : templateToIjParamsInfoMap.entrySet()) {
      templateToTransitiveIjParams.put(
          entry.getKey().getTemplateName(), entry.getValue().ijParamSet);
    }
    return templateToTransitiveIjParams.build();
  }

  /**
   * Compiles this Soy file set into JS source code files and returns these JS files as a list of
   * strings, one per file.
   *
   * @param jsSrcOptions The compilation options for the JS Src output target.
   * @param msgBundle The bundle of translated messages, or null to use the messages from the Soy
   *     source.
   * @return A list of strings where each string represents the JS source code that belongs in one
   *     JS file. The generated JS files correspond one-to-one to the original Soy source files.
   * @throws SoySyntaxException If a syntax error is found.
   * TODO(brndn): Instead of throwing, should return a structure with a list of errors that callers
   * can inspect.
   */
  @SuppressWarnings("deprecation")
  public List<String> compileToJsSrc(SoyJsSrcOptions jsSrcOptions, @Nullable SoyMsgBundle msgBundle)
      throws SoySyntaxException {

    // Synchronize old and new ways to declare syntax version V1.
    if (jsSrcOptions.shouldAllowDeprecatedSyntax()) {
      generalOptions.setDeclaredSyntaxVersionName("1.0");
    }
    // JS has traditionally allowed unknown globals, as a way for soy to reference normal js enums
    // and constants.  For consistency/reusability of templates it would be nice to not allow that
    // but the cat is out of the bag.
    ParseResult parseResult =
        parse(
            SyntaxVersion.V2_0,
            true /* allow unknown globals */,
            false /* allow unknown functions */,
            typeRegistry,
            soyFunctionMap);
    TemplateRegistry registry = parseResult.registry();
    registry = runMiddleendPasses(registry, parseResult.fileSet());
    // TODO(lukes): pass the template registry to jsSrcMain
    return jsSrcMainProvider.get().genJsSrc(parseResult.fileSet(), jsSrcOptions, msgBundle);
  }

  /**
   * Compiles this Soy file set into JS source code files and writes these JS files to disk.
   *
   * @param outputPathFormat The format string defining how to build the output file path
   *     corresponding to an input file path.
   * @param inputFilePathPrefix The prefix prepended to all input file paths (can be empty string).
   * @param jsSrcOptions The compilation options for the JS Src output target.
   * @param locales The list of locales. Can be an empty list if not applicable.
   * @param messageFilePathFormat The message file path format, or null if not applicable.
   * @throws SoySyntaxException If a syntax error is found.
   * @throws IOException If there is an error in opening/reading a message file or opening/writing
   *     an output JS file.
   */
  @SuppressWarnings("deprecation")
  CompilationResult compileToJsSrcFiles(
      String outputPathFormat, String inputFilePathPrefix, SoyJsSrcOptions jsSrcOptions,
      List<String> locales, @Nullable String messageFilePathFormat)
      throws SoySyntaxException, IOException {

    // Synchronize old and new ways to declare syntax version V1.
    if (jsSrcOptions.shouldAllowDeprecatedSyntax()) {
      generalOptions.setDeclaredSyntaxVersionName("1.0");
    }

    Checkpoint checkpoint = errorReporter.checkpoint();

    // Allow unknown globals for backwards compatibility
    ParseResult result =
        parse(
            SyntaxVersion.V2_0,
            true /* allow unknown globals */,
            false /* allow unknown functions */,
            typeRegistry,
            soyFunctionMap);
    if (errorReporter.errorsSince(checkpoint)) {
      return failure();
    }

    SoyFileSetNode soyTree = result.fileSet();
    TemplateRegistry registry = result.registry();
    registry = runMiddleendPasses(registry, soyTree);
    // TODO(lukes): pass the template registry to jsSrcMain
    if (errorReporter.errorsSince(checkpoint)) {
      return failure();
    }

    if (locales.isEmpty()) {
      // Not generating localized JS.
      jsSrcMainProvider.get().genJsFiles(
          soyTree, jsSrcOptions, null, null, outputPathFormat, inputFilePathPrefix);

    } else {
      // Generating localized JS.
      for (String locale : locales) {

        SoyFileSetNode soyTreeClone = SoytreeUtils.cloneNode(soyTree);

        String msgFilePath = MainEntryPointUtils.buildFilePath(
            messageFilePathFormat, locale, null, inputFilePathPrefix);

        SoyMsgBundle msgBundle =
            msgBundleHandlerProvider.get().createFromFile(new File(msgFilePath));
        if (msgBundle.getLocaleString() == null) {
          // TODO: Remove this check (but make sure no projects depend on this behavior).
          // There was an error reading the message file. We continue processing only if the locale
          // begins with "en", because falling back to the Soy source will probably be fine.
          if (!locale.startsWith("en")) {
            throw new IOException("Error opening or reading message file " + msgFilePath);
          }
        }

        jsSrcMainProvider.get().genJsFiles(
            soyTreeClone, jsSrcOptions, locale, msgBundle, outputPathFormat, inputFilePathPrefix);
      }
    }
    return result();
  }

  /**
   * Compiles this Soy file set into JS source code files and writes these JS files to disk.
   *
   * @param outputPathFormat The format string defining how to build the output file path
   *     corresponding to an input file path.
   * @param jsSrcOptions The compilation options for the JS Src output target.
   * @throws SoySyntaxException If a syntax error is found.
   * @throws IOException If there is an error in opening/reading a message file or opening/writing
   *     an output JS file.
   */
  @SuppressWarnings("deprecation")
  CompilationResult compileToIncrementalDomSrcFiles(
      String outputPathFormat,
      SoyJsSrcOptions jsSrcOptions)
      throws SoySyntaxException, IOException {

    SyntaxVersion declaredSyntaxVersion =
        generalOptions.getDeclaredSyntaxVersion(SyntaxVersion.V2_0);

    Preconditions.checkState(
        declaredSyntaxVersion.num >= SyntaxVersion.V2_0.num,
        "Incremental DOM code generation only supports syntax version of V2 or higher.");

    Checkpoint checkpoint = errorReporter.checkpoint();
    ParseResult result = parse(SyntaxVersion.V2_0);

    if (errorReporter.errorsSince(checkpoint)) {
      return failure();
    }
    SoyFileSetNode soyTree = result.fileSet();
    new ChangeCallsToPassAllDataVisitor().exec(soyTree);
    simplifyVisitor.exec(soyTree);

    if (errorReporter.errorsSince(checkpoint)) {
      return failure();
    }

    incrementalDomSrcMainProvider.get().genJsFiles(soyTree, jsSrcOptions, outputPathFormat);

    return result();
  }

  /**
   * Compiles this Soy file set into Python source code files and writes these Python files to
   * disk.
   *
   * @param outputPathFormat The format string defining how to build the output file path
   *     corresponding to an input file path.
   * @param inputFilePathPrefix The prefix prepended to all input file paths (can be empty string).
   * @param pySrcOptions The compilation options for the Python Src output target.
   * @throws SoySyntaxException If a syntax error is found.
   * @throws IOException If there is an error in opening/reading a message file or opening/writing
   *     an output JS file.
   */
  CompilationResult compileToPySrcFiles(
      String outputPathFormat, String inputFilePathPrefix, SoyPySrcOptions pySrcOptions)
      throws SoySyntaxException, IOException {


    Checkpoint checkpoint = errorReporter.checkpoint();
    ParseResult result = parse(SyntaxVersion.V2_0);
    if (errorReporter.errorsSince(checkpoint)) {
      return failure();
    }
    SoyFileSetNode soyTree = result.fileSet();

    TemplateRegistry registry = result.registry();
    registry = runMiddleendPasses(registry, result.fileSet());
    // TODO(lukes): pass the template registry to pySrcMain
    if (errorReporter.errorsSince(checkpoint)) {
      return failure();
    }

    pySrcMainProvider.get().genPyFiles(
        soyTree, pySrcOptions, outputPathFormat, inputFilePathPrefix);

    return result();
  }

  private CompilationResult result() {
    ErrorReporterImpl impl = (ErrorReporterImpl) errorReporter;
    return new CompilationResult(
        impl.getErrors(), new ErrorPrettyPrinter(new SnippetFormatter(soyFileSuppliers)));
  }

  private CompilationResult failure() {
    ImmutableCollection<? extends SoySyntaxException> errors
        = ((ErrorReporterImpl) errorReporter).getErrors();
    Preconditions.checkState(!errors.isEmpty());
    return new CompilationResult(
        errors, new ErrorPrettyPrinter(new SnippetFormatter(soyFileSuppliers)));
  }

  // Parse the current file set with the given default syntax version.
  private ParseResult parse(SyntaxVersion defaultVersion) {
    return parse(
        defaultVersion,
        false /* allow unknown globals */,
        false /* allow unknown functions */,
        typeRegistry,
        soyFunctionMap);
  }

  /**
   * A parse method that allows disabling certain features.  All callers should prefer the
   * {@link #parse(SyntaxVersion)} overload whenever possible.
   *
   * @param defaultVersion The default declared syntax version
   * @param allowUknownGlobals Whether to allow unknown globals
   * @param typeRegistry The type registry to use
   * @param soyFunctionMap The map of Soy functions to use
   */
  private ParseResult parse(
      SyntaxVersion defaultVersion,
      boolean allowUknownGlobals,
      boolean allowUknownFunctions,
      SoyTypeRegistry typeRegistry,
      ImmutableMap<String, ? extends SoyFunction> soyFunctionMap) {
    SyntaxVersion declaredSyntaxVersion = generalOptions.getDeclaredSyntaxVersion(defaultVersion);
    PassManager.Builder builder =
        new PassManager.Builder()
            .setTypeRegistry(typeRegistry)
            .setGeneralOptions(generalOptions)
            .setDeclaredSyntaxVersion(declaredSyntaxVersion)
            .setSoyFunctionMap(soyFunctionMap)
            .setErrorReporter(errorReporter);
    if (allowUknownGlobals) {
      builder.allowUnknownGlobals();
    }
    if (allowUknownFunctions) {
      builder.allowUnknownFunctions();
    }
    return new SoyFileSetParser(
            typeRegistry,
            cache,
            soyFileSuppliers,
            builder.build(),
            errorReporter)
        .parse();
  }

  // TODO(gboyer): There are several fields on this class that end up saving around some state, and
  // thus are not safe to be used by multiple threads at once. Here, we add synchronized as a
  // stop-gap. However, given that most users of SoyFileSet use it once and throw it away, that
  // might be a better precondition.
  /**
   * Runs middleend passes on the given Soy tree.
   *
   * @param soyTree The Soy tree to run middleend passes on.
   * @return A new TemplateRegistry.  The contextual autoescaper occasionally adds new templates and
   *     so the TemplateRegistry needs to be recreated.
   * @throws SoySyntaxException If a syntax error is found.
   */
  @CheckReturnValue
  private synchronized TemplateRegistry runMiddleendPasses(
      TemplateRegistry registry, SoyFileSetNode soyTree) throws SoySyntaxException {

    Checkpoint checkpoint = errorReporter.checkpoint();
    // Run contextual escaping after CSS and substitutions have been done.
    doContextualEscaping(soyTree);
    if (errorReporter.errorsSince(checkpoint)) {
      // Further passes that rely on sliced raw text nodes, such as conformance and CSP, can't
      // proceed if contextual escaping failed.
      return registry;
    }
    // contextual autoescaping may actually add new templates to the tree so we need to reconstruct
    // the registry
    registry = new TemplateRegistry(soyTree, errorReporter);
    if (checkConformance != null) {
      checkConformance.check(ConformanceInput.create(
          soyTree, contextualAutoescaper.getSlicedRawTextNodes()));
    }

    // Add print directives that mark inline-scripts as safe to run.
    if (generalOptions.supportContentSecurityPolicy()) {
      ContentSecurityPolicyPass.blessAuthorSpecifiedScripts(
          contextualAutoescaper.getSlicedRawTextNodes());
    }

    // Attempt to simplify the tree.
    new ChangeCallsToPassAllDataVisitor().exec(soyTree);
    simplifyVisitor.exec(soyTree);
    return registry;
  }


  private void doContextualEscaping(SoyFileSetNode soyTree)
      throws SoySyntaxException {
    List<TemplateNode> extraTemplates = contextualAutoescaper.rewrite(soyTree);
    // TODO: Run the redundant template remover here and rename after CL 16642341 is in.
    if (!extraTemplates.isEmpty()) {
      // TODO: pull out somewhere else.  Ideally do the merge as part of the redundant template
      // removal.
      Map<String, SoyFileNode> containingFile = Maps.newHashMap();
      for (SoyFileNode fileNode : soyTree.getChildren()) {
        for (TemplateNode templateNode : fileNode.getChildren()) {
          String name =
              templateNode instanceof TemplateDelegateNode
                  ? ((TemplateDelegateNode) templateNode).getDelTemplateName()
                  : templateNode.getTemplateName();
          containingFile.put(DerivedTemplateUtils.getBaseName(name), fileNode);
        }
      }
      for (TemplateNode extraTemplate : extraTemplates) {
        String name =
            extraTemplate instanceof TemplateDelegateNode
                ? ((TemplateDelegateNode) extraTemplate).getDelTemplateName()
                : extraTemplate.getTemplateName();
        containingFile.get(DerivedTemplateUtils.getBaseName(name)).addChild(extraTemplate);
      }
    }
  }
}
