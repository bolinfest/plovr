/*
 * Copyright 2012 Google Inc.
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

package com.google.template.soy.passes;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.template.soy.base.internal.IdGenerator;
import com.google.template.soy.error.ErrorReporter;
import com.google.template.soy.error.ErrorReporter.Checkpoint;
import com.google.template.soy.error.SoyError;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.soytree.AbstractSoyNodeVisitor;
import com.google.template.soy.soytree.MsgNode;
import com.google.template.soy.soytree.MsgPluralNode;
import com.google.template.soy.soytree.MsgSelectCaseNode;
import com.google.template.soy.soytree.MsgSelectDefaultNode;
import com.google.template.soy.soytree.MsgSelectNode;
import com.google.template.soy.soytree.MsgSubstUnitBaseVarNameUtils;
import com.google.template.soy.soytree.SoyNode;
import com.google.template.soy.soytree.SoyNode.ParentSoyNode;
import com.google.template.soy.soytree.SoyNode.StandaloneNode;
import com.google.template.soy.soytree.SoytreeUtils;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Visitor for rewriting 'msg' nodes with 'genders' attribute into 'msg' nodes with one or more
 * levels of 'select'.
 *
 */
final class RewriteGenderMsgsVisitor extends AbstractSoyNodeVisitor<Void> {

  private static final SoyError GENDER_AND_SELECT_NOT_ALLOWED = SoyError.of(
      "Cannot mix ''genders'' attribute with ''select'' command in the same message.");
  private static final SoyError MORE_THAN_TWO_GENDER_EXPRS = SoyError.of(
      "In a msg with ''plural'', the ''genders'' attribute can contain at most 2 expressions "
      + "(otherwise, combinatorial explosion would cause a gigantic generated message).");

  /** Fallback base select var name. */
  public static final String FALLBACK_BASE_SELECT_VAR_NAME = "GENDER";
  private final ErrorReporter errorReporter;


  /** Node id generator for the Soy tree being visited. */
  private IdGenerator nodeIdGen;


  /**
   * Constructs a rewriter using the same node ID generator as the tree.
   *
   * @param nodeIdGen The same node ID generator used to generate the existing tree nodes.
   */
  public RewriteGenderMsgsVisitor(IdGenerator nodeIdGen, ErrorReporter errorReporter) {
    this.errorReporter = errorReporter;
    this.nodeIdGen = Preconditions.checkNotNull(nodeIdGen);
  }


  // -----------------------------------------------------------------------------------------------
  // Implementations for specific nodes.


  @Override protected void visitMsgNode(MsgNode msg) {

    List<ExprRootNode> genderExprs = msg.getAndRemoveGenderExprs();
    if (genderExprs == null) {
      return;  // not a msg that this pass should rewrite
    }

    // Check that 'genders' attribute and 'select' command are not used together.
    if (msg.getChild(0) instanceof MsgSelectNode) {
      errorReporter.report(
          msg.getChild(0).getSourceLocation(), GENDER_AND_SELECT_NOT_ALLOWED);
    }

    // If plural msg, check that there are max 2 genders.
    if (msg.getChild(0) instanceof MsgPluralNode && genderExprs.size() > 2) {
      errorReporter.report(msg.getSourceLocation(), MORE_THAN_TWO_GENDER_EXPRS);
    }

    // ------ Do the rewrite. ------

    // Note: We process the genders in reverse order so that the first listed gender will end up
    // being the outermost 'select' level.
    genderExprs = Lists.reverse(genderExprs);

    Checkpoint checkpoint = errorReporter.checkpoint();
    List<String> baseSelectVarNames = MsgSubstUnitBaseVarNameUtils.genNoncollidingBaseNamesForExprs(
        ExprRootNode.unwrap(genderExprs), FALLBACK_BASE_SELECT_VAR_NAME, errorReporter);
    if (errorReporter.errorsSince(checkpoint)) {
      return; // To prevent an IndexOutOfBoundsException below.
    }

    for (int i = 0; i < genderExprs.size(); i++) {
      ExprRootNode genderExpr = genderExprs.get(i);
      String baseSelectVarName = baseSelectVarNames.get(i);

      // Check whether the generated base name would be the same (both for the old naive algorithm
      // and the new algorithm). If so, then there's no need to specify the baseSelectVarName.
      if (MsgSubstUnitBaseVarNameUtils.genNaiveBaseNameForExpr(
          genderExpr.getRoot(), FALLBACK_BASE_SELECT_VAR_NAME)
              .equals(baseSelectVarName)
          && MsgSubstUnitBaseVarNameUtils.genShortestBaseNameForExpr(
              genderExpr.getRoot(), FALLBACK_BASE_SELECT_VAR_NAME)
              .equals(baseSelectVarName)) {
        baseSelectVarName = null;
      }

      splitMsgForGender(msg, genderExpr, baseSelectVarName);
    }
  }


  /**
   * Helper to split a msg for gender, by adding a 'select' node and cloning the msg's contents
   * into all 3 cases of the 'select' node ('female'/'male'/default).
   * @param msg The message to split.
   * @param genderExpr The expression for the gender value.
   * @param baseSelectVarName The base select var name to use, or null if it should be generated
   *     from the gender expression.
   */
  private void splitMsgForGender(
      MsgNode msg, ExprRootNode genderExpr, @Nullable String baseSelectVarName) {

    List<StandaloneNode> origChildren = ImmutableList.copyOf(msg.getChildren());
    msg.clearChildren();

    MsgSelectCaseNode femaleCase
        = new MsgSelectCaseNode.Builder(nodeIdGen.genId(), "'female'", msg.getSourceLocation())
            .build(errorReporter);
    femaleCase.addChildren(SoytreeUtils.cloneListWithNewIds(origChildren, nodeIdGen));
    MsgSelectCaseNode maleCase
        = new MsgSelectCaseNode.Builder(nodeIdGen.genId(), "'male'", msg.getSourceLocation())
            .build(errorReporter);
    maleCase.addChildren(SoytreeUtils.cloneListWithNewIds(origChildren, nodeIdGen));
    MsgSelectDefaultNode defaultCase
        = new MsgSelectDefaultNode(nodeIdGen.genId(), msg.getSourceLocation());
    defaultCase.addChildren(SoytreeUtils.cloneListWithNewIds(origChildren, nodeIdGen));

    MsgSelectNode selectNode = new MsgSelectNode(
        nodeIdGen.genId(), msg.getSourceLocation(), genderExpr, baseSelectVarName);
    selectNode.addChild(femaleCase);
    selectNode.addChild(maleCase);
    selectNode.addChild(defaultCase);

    msg.addChild(selectNode);
  }


  // -----------------------------------------------------------------------------------------------
  // Fallback implementation.


  @Override protected void visitSoyNode(SoyNode node) {
    if (node instanceof ParentSoyNode<?>) {
      visitChildrenAllowingConcurrentModification((ParentSoyNode<?>) node);
    }
  }

}
