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

package com.google.template.soy.msgs.internal;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.template.soy.msgs.SoyMsgBundle;
import com.google.template.soy.msgs.internal.MsgUtils.MsgPartsAndIds;
import com.google.template.soy.msgs.restricted.SoyMsg;
import com.google.template.soy.msgs.restricted.SoyMsgBundleImpl;
import com.google.template.soy.soytree.AbstractSoyNodeVisitor;
import com.google.template.soy.soytree.MsgNode;
import com.google.template.soy.soytree.SoyFileNode;
import com.google.template.soy.soytree.SoyFileSetNode;
import com.google.template.soy.soytree.SoyNode;
import com.google.template.soy.soytree.SoyNode.ParentSoyNode;

import java.util.List;

/**
 * Visitor for extracting messages from a Soy parse tree.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 * <p> {@link #exec} should be called on a full parse tree. All messages will be extracted and
 * returned in a {@code SoyMsgBundle} (locale "en").
 *
 */
public final class ExtractMsgsVisitor extends AbstractSoyNodeVisitor<SoyMsgBundle> {


  /** List of messages collected during the pass. */
  private List<SoyMsg> msgs;

  /** Current Soy file path (during a pass). */
  private String currentSource;

  /**
   * Returns a SoyMsgBundle containing all messages extracted from the given SoyFileSetNode or
   * SoyFileNode (locale string is null).
   */
  @Override public SoyMsgBundle exec(SoyNode node) {

    Preconditions.checkArgument(node instanceof SoyFileSetNode || node instanceof SoyFileNode);

    msgs = Lists.newArrayList();
    currentSource = null;
    visit(node);
    currentSource = null;
    return new SoyMsgBundleImpl(null, msgs);
  }


  /**
   * Returns a SoyMsgBundle containing all messages extracted from the given nodes (locale string is
   * null).
   */
  public SoyMsgBundle execOnMultipleNodes(Iterable<? extends SoyNode> nodes) {

    msgs = Lists.newArrayList();
    for (SoyNode node : nodes) {
      if (node instanceof SoyFileSetNode || node instanceof SoyFileNode) {
        currentSource = null;
      } else {
        currentSource = node.getNearestAncestor(SoyFileNode.class).getFilePath();
      }
      visit(node);
      currentSource = null;
    }
    return new SoyMsgBundleImpl(null, msgs);
  }


  // -----------------------------------------------------------------------------------------------
  // Implementations for specific nodes.


  @Override protected void visitSoyFileNode(SoyFileNode node) {

    currentSource = node.getFilePath();
    visitChildren(node);
    currentSource = null;
  }


  @Override protected void visitMsgNode(MsgNode node) {

    MsgPartsAndIds msgPartsAndIds = MsgUtils.buildMsgPartsAndComputeMsgIdForDualFormat(node);
    msgs.add(new SoyMsg(
        msgPartsAndIds.id, -1L, null, node.getMeaning(), node.getDesc(), node.isHidden(),
        node.getContentType(), currentSource, node.isPlrselMsg(), msgPartsAndIds.parts));
  }


  // -----------------------------------------------------------------------------------------------
  // Fallback implementation.


  @Override protected void visitSoyNode(SoyNode node) {
    if (node instanceof ParentSoyNode<?>) {
      visitChildren((ParentSoyNode<?>) node);
    }
  }

}
