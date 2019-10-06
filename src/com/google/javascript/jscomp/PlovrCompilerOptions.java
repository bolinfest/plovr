package com.google.javascript.jscomp;

import com.google.common.collect.Multimap;
import com.google.javascript.jscomp.CustomPassExecutionTime;
import java.nio.charset.Charset;

/**
 * {@link PlovrCompilerOptions} is a subclass of {@link CompilerOptions} with
 * customizations for plovr. The need for the subclass is twofold:
 * <ol>
 *   <li>It contains compiler options that are specific to plovr, such as
 *       {@code globalScopeName} (though admittedly, {@code globalScopeName}
 *       will go away soon in favor of the Compiler's built-in
 *       {@link RescopeGlobalSymbols} pass).
 *   <li>It provides public accessors to properties that are ordinarily
 *       package-private, which is helpful during unit-testing. (See
 *       {@link org.plovr.ConfigTest}).
 * </ol>
 * Originally, these modifications were contained in plovr's local fork of
 * {@link CompilerOptions}, but this often created merge conflicts when syncing
 * plovr's Closure Compiler fork.
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
@SuppressWarnings("serial")
public class PlovrCompilerOptions extends CompilerOptions {
  /** Expand the visibility of this method from package-private to public. */
  @Override
  public Charset getOutputCharset() {
    return super.getOutputCharset();
  }

  private boolean treatWarningsAsErrors = false;

  /**
   * If treatWarningsAsErrors is set to true, future calls to setWarningLevel
   * will always interpret WARNING as ERROR.
   */
  public void setTreatWarningsAsErrors(boolean value) {
    treatWarningsAsErrors = value;
  }

  @Override public void setWarningLevel(DiagnosticGroup group, CheckLevel level) {
    boolean escalateToError = treatWarningsAsErrors && level == CheckLevel.WARNING;
    super.setWarningLevel(group, escalateToError ? CheckLevel.ERROR : level);
  }

  public void setCustomPasses(Multimap<CustomPassExecutionTime, CompilerPass> passes) {
    this.customPasses = passes;
  }

  public Multimap<CustomPassExecutionTime, CompilerPass> getCustomPasses() {
    return this.customPasses;
  }
}
