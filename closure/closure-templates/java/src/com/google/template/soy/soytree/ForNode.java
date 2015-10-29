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

import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.SoyError;
import com.google.template.soy.exprparse.ExpressionParser;
import com.google.template.soy.exprtree.ExprNode;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.exprtree.VarRefNode;
import com.google.template.soy.soytree.SoyNode.ConditionalBlockNode;
import com.google.template.soy.soytree.SoyNode.ExprHolderNode;
import com.google.template.soy.soytree.SoyNode.LocalVarBlockNode;
import com.google.template.soy.soytree.SoyNode.LoopNode;
import com.google.template.soy.soytree.SoyNode.StandaloneNode;
import com.google.template.soy.soytree.SoyNode.StatementNode;
import com.google.template.soy.soytree.defn.LocalVar;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node representing a 'for' statement.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
public final class ForNode extends AbstractBlockCommandNode
    implements StandaloneNode, StatementNode, ConditionalBlockNode, LoopNode, ExprHolderNode,
    LocalVarBlockNode {

  /**
   * The arguments to a {@code range(...)} expression in a {@code {for ...}} loop statement.
   */
  @AutoValue public abstract static class RangeArgs {
    static final RangeArgs ERROR = create(
        Optional.<ExprRootNode>absent(),
        new ExprRootNode(VarRefNode.ERROR),
        Optional.<ExprRootNode>absent());

    static RangeArgs create(Optional<ExprRootNode> start,
        ExprRootNode limit, Optional<ExprRootNode> increment) {
      return new AutoValue_ForNode_RangeArgs(start, limit, increment);
    }

    RangeArgs() {}

    /**
     * The expression for the iteration start point.
     *
     * <p>This is optional, the default beginning of iteration is {@code 0} if this is not set.
     */
    public abstract Optional<ExprRootNode> start();

    /**
     * The expression for the iteration end point.  This is interpreted as an exclusive limit.
     */
    public abstract ExprRootNode limit();

    /**
     * The expression for the iteration increment.
     *
     * <p>This is optional, the default increment {@code 1} if this is not set.
     */
    public abstract Optional<ExprRootNode> increment();

    private RangeArgs copy(CopyState copyState) {
      return create(
          start().isPresent()
              ? Optional.of(start().get().copy(copyState))
              : Optional.<ExprRootNode>absent(),
          limit().copy(copyState),
          increment().isPresent()
              ? Optional.of(increment().get().copy(copyState))
              : Optional.<ExprRootNode>absent());
    }
  }

  private static final SoyError INVALID_COMMAND_TEXT
      = SoyError.of("Invalid ''for'' command text");
  private static final SoyError INVALID_RANGE_SPECIFICATION
      = SoyError.of("Invalid range specification");

  /** Regex pattern for the command text. */
  // 2 capturing groups: local var name, arguments to range()
  private static final Pattern COMMAND_TEXT_PATTERN =
      Pattern.compile("( [$] \\w+ ) \\s+ in \\s+ range[(] \\s* (.*) \\s* [)]",
                      Pattern.COMMENTS | Pattern.DOTALL);


  /** The Local variable for this loop. */
  private final LocalVar var;

  /** The parsed range args. */
  private final RangeArgs rangeArgs;

  /**
   * @param id The id for this node.
   * @param commandText The command text.
   * @param sourceLocation The source location for the {@code for }node.
   * @param errorReporter For reporting errors.
   */
  public ForNode(
      int id,
      String commandText,
      SourceLocation sourceLocation,
      ErrorReporter errorReporter) {
    super(id, sourceLocation, "for", commandText);

    Matcher matcher = COMMAND_TEXT_PATTERN.matcher(commandText);
    if (!matcher.matches()) {
      errorReporter.report(sourceLocation, INVALID_COMMAND_TEXT);
    }

    String varName = parseVarName(
        matcher.group(1), sourceLocation, errorReporter);
    List<ExprNode> rangeArgs = parseRangeArgs(
        matcher.group(2), sourceLocation, errorReporter);

    if (rangeArgs.size() > 3) {
      errorReporter.report(sourceLocation, INVALID_RANGE_SPECIFICATION);
      this.rangeArgs = RangeArgs.ERROR;
    } else if (rangeArgs.isEmpty()) {
      this.rangeArgs = RangeArgs.ERROR;
    } else {
      // OK, now interpret the args
      // If there are 2 or more args, then the first is the 'start' value
      ExprNode start = rangeArgs.size() >= 2 ? rangeArgs.get(0) : null;

      // If there are 3 args, then the last one is the increment.
      ExprNode increment = rangeArgs.size() == 3 ? rangeArgs.get(2) : null;

      // the limit is the first item if there is only one arg, otherwise it is the second arg
      ExprNode limit = rangeArgs.get(rangeArgs.size() == 1 ? 0 : 1);
      this.rangeArgs = RangeArgs.create(
          start == null
              ? Optional.<ExprRootNode>absent()
              : Optional.of(new ExprRootNode(start)),
          new ExprRootNode(limit),
          increment == null
              ? Optional.<ExprRootNode>absent()
              : Optional.of(new ExprRootNode(increment)));
    }

    var = new LocalVar(varName, this, null);
  }

  private static String parseVarName(
      String input, SourceLocation sourceLocation, ErrorReporter errorReporter) {
    return new ExpressionParser(input, sourceLocation, errorReporter)
        .parseVariable()
        .getName();
  }

  private static List<ExprNode> parseRangeArgs(
      String input, SourceLocation sourceLocation, ErrorReporter errorReporter) {
    return new ExpressionParser(input, sourceLocation, errorReporter)
        .parseExpressionList();
  }


  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  private ForNode(ForNode orig, CopyState copyState) {
    super(orig, copyState);
    this.var = new LocalVar(orig.var, this);
    this.rangeArgs = orig.rangeArgs.copy(copyState);
  }


  @Override public Kind getKind() {
    return Kind.FOR_NODE;
  }


  @Override public final LocalVar getVar() {
    return var;
  }


  @Override public final String getVarName() {
    return var.name();
  }


  /** Returns the parsed range args. */
  public RangeArgs getRangeArgs() {
    return rangeArgs;
  }


  @Override public List<ExprUnion> getAllExprUnions() {
    return ExprUnion.createList(
        ImmutableList.copyOf(
            Iterables.concat(
                rangeArgs.start().asSet(),
                ImmutableList.of(rangeArgs.limit()),
                rangeArgs.increment().asSet())));
  }


  @Override public BlockNode getParent() {
    return (BlockNode) super.getParent();
  }


  @Override public ForNode copy(CopyState copyState) {
    return new ForNode(this, copyState);
  }

}
