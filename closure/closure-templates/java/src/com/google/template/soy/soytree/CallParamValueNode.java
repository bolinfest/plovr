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

package com.google.template.soy.soytree;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.ErrorReporter.Checkpoint;
import com.google.template.soy.error.ExplodingErrorReporter;
import com.google.template.soy.error.SoyError;
import com.google.template.soy.soytree.SoyNode.ExprHolderNode;

import java.util.List;

/**
 * Node representing a 'param' with a value expression.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
public final class CallParamValueNode extends CallParamNode implements ExprHolderNode {

  private static final SoyError SELF_ENDING_TAG_WITHOUT_VALUE
      = SoyError.of("A ''param'' tag should be self-ending (with a trailing ''/'') if and only if "
          + "it also contains a value (invalid tag is '{'param {0} /'}').");
  private static final SoyError SELF_ENDING_TAG_WITH_KIND_ATTRIBUTE
      = SoyError.of("The ''kind'' attribute is not allowed on self-ending ''param'' tags "
          + "(invalid tag is '{'param {0} /'}').");

  /** The param key. */
  private final String key;

  /** The parsed expression for the param value. */
  private final ExprUnion valueExprUnion;

  private CallParamValueNode(
      int id,
      SourceLocation sourceLocation,
      String key,
      ExprUnion valueExprUnion,
      String commandText) {
    super(id, sourceLocation, commandText);
    this.key = Preconditions.checkNotNull(key);
    this.valueExprUnion = Preconditions.checkNotNull(valueExprUnion);
  }


  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  private CallParamValueNode(CallParamValueNode orig, CopyState copyState) {
    super(orig, copyState);
    this.key = orig.key;
    this.valueExprUnion = (orig.valueExprUnion != null)
        ? orig.valueExprUnion.copy(copyState)
        : null;
  }


  @Override public Kind getKind() {
    return Kind.CALL_PARAM_VALUE_NODE;
  }


  @Override public String getKey() {
    return key;
  }


  /** Returns the expression text for the param value. */
  public String getValueExprText() {
    return valueExprUnion.getExprText();
  }


  /** Returns the parsed expression for the param value. */
  public ExprUnion getValueExprUnion() {
    return valueExprUnion;
  }


  @Override public String getTagString() {
    return buildTagStringHelper(true);
  }


  @Override public List<ExprUnion> getAllExprUnions() {
    return ImmutableList.of(valueExprUnion);
  }


  @Override public CallParamValueNode copy(CopyState copyState) {
    return new CallParamValueNode(this, copyState);
  }

  public static final class Builder extends CallParamNode.Builder {

    private static CallParamValueNode error() {
      return new Builder(-1, "error: error", SourceLocation.UNKNOWN)
          .build(ExplodingErrorReporter.get()); // guaranteed to build
    }

    public Builder(int id, String commandText, SourceLocation sourceLocation) {
      super(id, commandText, sourceLocation);
    }

    public CallParamValueNode build(ErrorReporter errorReporter) {
      Checkpoint checkpoint = errorReporter.checkpoint();
      CommandTextParseResult parseResult = parseCommandTextHelper(errorReporter);

      if (parseResult.valueExprUnion == null) {
        errorReporter.report(sourceLocation, SELF_ENDING_TAG_WITHOUT_VALUE, commandText);
      }

      if (parseResult.contentKind != null) {
        errorReporter.report(sourceLocation, SELF_ENDING_TAG_WITH_KIND_ATTRIBUTE, commandText);
      }

      if (errorReporter.errorsSince(checkpoint)) {
        return error();
      }

      CallParamValueNode node = new CallParamValueNode(
          id, sourceLocation, parseResult.key, parseResult.valueExprUnion, commandText);
      return node;
    }
  }
}
