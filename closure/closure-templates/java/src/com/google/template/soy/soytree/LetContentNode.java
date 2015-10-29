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

package com.google.template.soy.soytree;

import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.basetree.MixinParentNode;
import com.google.template.soy.data.SanitizedContent.ContentKind;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.ErrorReporter.Checkpoint;
import com.google.template.soy.error.ExplodingErrorReporter;
import com.google.template.soy.error.SoyError;
import com.google.template.soy.soytree.SoyNode.RenderUnitNode;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Node representing a 'let' statement with content.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
public final class LetContentNode extends LetNode implements RenderUnitNode {

  public static final SoyError NON_SELF_ENDING_WITH_VALUE
      = SoyError.of("A ''let'' tag should contain a value if and only if it is also self-ending "
          + "(with a trailing ''/'').");

  /** The mixin object that implements the ParentNode functionality. */
  private final MixinParentNode<StandaloneNode> parentMixin;

  /** The let node's content kind, or null if no 'kind' attribute was present. */
  @Nullable private final ContentKind contentKind;


  private LetContentNode(
      int id,
      SourceLocation sourceLocation,
      String localVarName,
      String commandText,
      ContentKind contentKind) {
    super(id, sourceLocation, localVarName, commandText);
    this.contentKind = contentKind;
    parentMixin = new MixinParentNode<>(this);
  }


  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  private LetContentNode(LetContentNode orig, CopyState copyState) {
    super(orig, copyState);
    this.parentMixin = new MixinParentNode<>(orig.parentMixin, this, copyState);
    this.contentKind = orig.contentKind;
  }


  @Override public Kind getKind() {
    return Kind.LET_CONTENT_NODE;
  }


  /**
   * Return The local variable name (without preceding '$').
   */
  @Override public final String getVarName() {
    return var.name();
  }


  @Override @Nullable public ContentKind getContentKind() {
    return contentKind;
  }


  // -----------------------------------------------------------------------------------------------
  // ParentSoyNode stuff.
  // Note: Most concrete nodes simply inherit this functionality from AbstractParentCommandNode or
  // AbstractParentSoyNode. But this class need to include its own MixinParentNode field because
  // it needs to subclass LetNode (and Java doesn't allow multiple inheritance).


  @Override public String toSourceString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getTagString());
    appendSourceStringForChildren(sb);
    sb.append("{/").append(getCommandName()).append("}");
    return sb.toString();
  }

  @Override public int numChildren() {
    return parentMixin.numChildren();
  }

  @Override public StandaloneNode getChild(int index) {
    return parentMixin.getChild(index);
  }

  @Override public int getChildIndex(StandaloneNode child) {
    return parentMixin.getChildIndex(child);
  }

  @Override public List<StandaloneNode> getChildren() {
    return parentMixin.getChildren();
  }

  @Override public void addChild(StandaloneNode child) {
    parentMixin.addChild(child);
  }

  @Override public void addChild(int index, StandaloneNode child) {
    parentMixin.addChild(index, child);
  }

  @Override public void removeChild(int index) {
    parentMixin.removeChild(index);
  }

  @Override public void removeChild(StandaloneNode child) {
    parentMixin.removeChild(child);
  }

  @Override public void replaceChild(int index, StandaloneNode newChild) {
    parentMixin.replaceChild(index, newChild);
  }

  @Override public void replaceChild(StandaloneNode currChild, StandaloneNode newChild) {
    parentMixin.replaceChild(currChild, newChild);
  }

  @Override public void clearChildren() {
    parentMixin.clearChildren();
  }

  @Override public void addChildren(List<? extends StandaloneNode> children) {
    parentMixin.addChildren(children);
  }

  @Override public void addChildren(int index, List<? extends StandaloneNode> children) {
    parentMixin.addChildren(index, children);
  }

  @Override public void appendSourceStringForChildren(StringBuilder sb) {
    parentMixin.appendSourceStringForChildren(sb);
  }

  @Override public void appendTreeStringForChildren(StringBuilder sb, int indent) {
    parentMixin.appendTreeStringForChildren(sb, indent);
  }

  @Override public String toTreeString(int indent) {
    return parentMixin.toTreeString(indent);
  }

  @Override public LetContentNode copy(CopyState copyState) {
    return new LetContentNode(this, copyState);
  }

  /**
   * Builder for {@link LetContentNode}.
   */
  public static final class Builder {

    private static LetContentNode error() {
      return new LetContentNode.Builder(-1, "$error", SourceLocation.UNKNOWN)
          .build(ExplodingErrorReporter.get()); // guaranteed to be valid
    }

    private final int id;
    private final String commandText;
    private final SourceLocation sourceLocation;

    /**
     * @param id The node's id.
     * @param commandText The node's command text.
     * @param sourceLocation The node's source location.
     */
    public Builder(int id, String commandText, SourceLocation sourceLocation) {
      this.id = id;
      this.commandText = commandText;
      this.sourceLocation = sourceLocation;
    }

    /**
     * Returns a new {@link LetContentNode} built from the builder's state. If the builder's state
     * is invalid, errors are reported to the {@code errorManager} and {Builder#error} is returned.
     */
    public LetContentNode build(ErrorReporter errorReporter) {
      Checkpoint checkpoint = errorReporter.checkpoint();
      CommandTextParseResult parseResult
          = parseCommandTextHelper(commandText, errorReporter, sourceLocation);

      if (parseResult.valueExpr != null) {
        errorReporter.report(sourceLocation, NON_SELF_ENDING_WITH_VALUE);
      }

      if (errorReporter.errorsSince(checkpoint)) {
        return error();
      }

      return new LetContentNode(
          id, sourceLocation, parseResult.localVarName, commandText, parseResult.contentKind);
    }
  }
}
