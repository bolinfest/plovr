package org.plovr;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.jscomp.LightweightMessageFormatter;
import com.google.javascript.jscomp.SourceExcerptProvider;

final class CompilationError {

  private final JSError jsError;
  private final SourceExcerptProvider sourceExcerptProvider;

  CompilationError(JSError jsError) {
    Preconditions.checkNotNull(jsError);
    this.jsError = jsError;
    this.sourceExcerptProvider = null;
  }

  CompilationError(JSError jsError, SourceExcerptProvider sourceExcerptProvider) {
    Preconditions.checkNotNull(jsError);
    Preconditions.checkNotNull(sourceExcerptProvider);
    this.jsError = jsError;
    this.sourceExcerptProvider = sourceExcerptProvider;
  }

    public CompilationError(ImmutableList<JSError> error, SourceExcerptProvider sourceExcerptProvider) {
    }

    String getSourceName() {
    return jsError.sourceName;
  }

  String getMessage() {
    if (jsError.getDefaultLevel() == CheckLevel.OFF) {
      // This is probably related to
      // http://code.google.com/p/closure-compiler/issues/detail?id=277
      // Please help track it down!
      // System.err.println("Why is CheckLevel OFF???");
      return jsError.description;
    }

    if (sourceExcerptProvider == null) {
      return jsError.description;
    } else {
      LightweightMessageFormatter formatter = new LightweightMessageFormatter(sourceExcerptProvider);
      return jsError.format(jsError.getDefaultLevel(), formatter);
    }
  }

  boolean isError() {
    return jsError.getDefaultLevel() == CheckLevel.ERROR;
  }

  int getLineNumber() {
    return jsError.lineNumber;
  }
}
