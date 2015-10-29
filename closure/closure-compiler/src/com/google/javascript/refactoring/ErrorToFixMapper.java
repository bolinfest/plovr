/*
 * Copyright 2014 The Closure Compiler Authors.
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
package com.google.javascript.refactoring;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.jscomp.JSError;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps a JSError to a SuggestedFix.
 */
public final class ErrorToFixMapper {
  private ErrorToFixMapper() {} // All static

  private static final Pattern DID_YOU_MEAN = Pattern.compile(".*Did you mean (.*)\\?");
  private static final Pattern MISSING_REQUIRE =
      Pattern.compile("'([^']+)' used but not goog\\.require'd");
  private static final Pattern EXTRA_REQUIRE =
      Pattern.compile("'([^']+)' goog\\.require'd but not used");

  public static List<SuggestedFix> getFixesForJsError(JSError error, AbstractCompiler compiler) {
    SuggestedFix fix = getFixForJsError(error, compiler);
    if (fix != null) {
      return ImmutableList.of(fix);
    }
    switch (error.getType().key) {
      case "JSC_IMPLICITLY_NULLABLE_JSDOC":
        return getFixesForImplicitlyNullableJsDoc(error);
      default:
        return ImmutableList.of();
    }
  }

  public static SuggestedFix getFixForJsError(JSError error, AbstractCompiler compiler) {
    switch (error.getType().key) {
      case "JSC_DEBUGGER_STATEMENT_PRESENT":
        return getFixForDebuggerStatement(error);
      case "JSC_INEXISTENT_PROPERTY":
        return getFixForInexistentProperty(error);
      case "JSC_MISSING_REQUIRE_WARNING":
        return getFixForMissingRequire(error, compiler);
      case "JSC_DUPLICATE_REQUIRE_WARNING":
      case "JSC_EXTRA_REQUIRE_WARNING":
        return getFixForExtraRequire(error, compiler);
      case "JSC_UNNECESSARY_CAST":
        return getFixForUnnecessaryCast(error, compiler);
      default:
        return null;
    }
  }

  private static List<SuggestedFix> getFixesForImplicitlyNullableJsDoc(JSError error) {
    SuggestedFix qmark = new SuggestedFix.Builder()
        .setOriginalMatchedNode(error.node)
        .insertBefore(error.node, "?")
        .setDescription("Make nullability explicit")
        .build();
    SuggestedFix bang = new SuggestedFix.Builder()
        .setOriginalMatchedNode(error.node)
        .insertBefore(error.node, "!")
        .setDescription("Make type non-nullable")
        .build();
    return ImmutableList.of(qmark, bang);
  }

  private static SuggestedFix getFixForDebuggerStatement(JSError error) {
    return new SuggestedFix.Builder()
        .setOriginalMatchedNode(error.node)
        .delete(error.node).build();
  }

  private static SuggestedFix getFixForInexistentProperty(JSError error) {
    Matcher m = DID_YOU_MEAN.matcher(error.description);
    if (m.matches()) {
      String suggestedPropName = m.group(1);
      return new SuggestedFix.Builder()
          .setOriginalMatchedNode(error.node)
          .rename(error.node, suggestedPropName).build();
    }
    return null;
  }

  private static SuggestedFix getFixForMissingRequire(JSError error, AbstractCompiler compiler) {
    Matcher regexMatcher = MISSING_REQUIRE.matcher(error.description);
    Preconditions.checkState(regexMatcher.matches(),
        "Unexpected error description: %s", error.description);
    String namespaceToRequire = regexMatcher.group(1);
    NodeMetadata metadata = new NodeMetadata(compiler);
    Match match = new Match(error.node, metadata);
    return new SuggestedFix.Builder()
        .setOriginalMatchedNode(error.node)
        .addGoogRequire(match, namespaceToRequire)
        .build();
  }

  private static SuggestedFix getFixForExtraRequire(JSError error, AbstractCompiler compiler) {
    Matcher regexMatcher = EXTRA_REQUIRE.matcher(error.description);
    Preconditions.checkState(regexMatcher.matches(),
        "Unexpected error description: %s", error.description);
    String namespace = regexMatcher.group(1);
    NodeMetadata metadata = new NodeMetadata(compiler);
    Match match = new Match(error.node, metadata);
    return new SuggestedFix.Builder()
        .setOriginalMatchedNode(error.node)
        .removeGoogRequire(match, namespace)
        .build();
  }

  private static SuggestedFix getFixForUnnecessaryCast(JSError error, AbstractCompiler compiler) {
    return new SuggestedFix.Builder()
        .setOriginalMatchedNode(error.node)
        .removeCast(error.node, compiler).build();
  }
}
