package org.plovr;

import com.google.javascript.jscomp.CompilationLevel;

/**
 * {@link CompilationMode} specifies the values for the "mode" parameter.
 * 
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public enum CompilationMode {
  /** One script tag per input. */
  RAW(null),
  
  /** Whitespace-only. */
  WHITESPACE(CompilationLevel.WHITESPACE_ONLY),
  
  /** Simple optimizations. */
  SIMPLE(CompilationLevel.SIMPLE_OPTIMIZATIONS),
  
  /** Advanced optimizations. */
  ADVANCED(CompilationLevel.ADVANCED_OPTIMIZATIONS),

  ;

  private final CompilationLevel level;
  
  private CompilationMode(CompilationLevel level) {
    this.level = level;
  }
  
  CompilationLevel getCompilationLevel() {
    return level;
  }
}
