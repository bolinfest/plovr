/*
 * Copyright 2015 Google Inc.
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

package com.google.template.soy.jbcsrc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.template.soy.jbcsrc.BytecodeUtils.constant;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.msgs.internal.MsgUtils.MsgPartsAndIds;
import com.google.template.soy.msgs.restricted.SoyMsgPart;
import com.google.template.soy.msgs.restricted.SoyMsgPart.Case;
import com.google.template.soy.msgs.restricted.SoyMsgPlaceholderPart;
import com.google.template.soy.msgs.restricted.SoyMsgPluralCaseSpec;
import com.google.template.soy.msgs.restricted.SoyMsgPluralPart;
import com.google.template.soy.msgs.restricted.SoyMsgPluralRemainderPart;
import com.google.template.soy.msgs.restricted.SoyMsgRawTextPart;
import com.google.template.soy.msgs.restricted.SoyMsgSelectPart;
import com.google.template.soy.soytree.CallNode;
import com.google.template.soy.soytree.MsgHtmlTagNode;
import com.google.template.soy.soytree.MsgNode;
import com.google.template.soy.soytree.MsgPlaceholderNode;
import com.google.template.soy.soytree.MsgPluralNode;
import com.google.template.soy.soytree.MsgSelectNode;
import com.google.template.soy.soytree.PrintNode;
import com.google.template.soy.soytree.RawTextNode;
import com.google.template.soy.soytree.SoyNode.ParentSoyNode;
import com.google.template.soy.soytree.SoyNode.StandaloneNode;

import org.objectweb.asm.Label;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper for compiling {@link MsgNode messages}
 */
final class MsgCompiler {

  /**
   * A helper interface that allows the MsgCompiler to interact with the SoyNodeCompiler in a 
   * limited way.
   */
  interface SoyNodeToStringCompiler {
    /**
     * Compiles the expression to a {@link String} valued expression.
     *
     * <p>If the node requires detach logic, it should use the given label as the reattach point.
     */
    Expression compileToString(ExprRootNode node, Label reattachPoint);

    /**
     * Compiles the expression to an {@code IntegerData} valued expression.
     *
     * <p>If the node requires detach logic, it should use the given label as the reattach point.
     */
    Expression compileToInt(ExprRootNode node, Label reattachPoint);

    /**
     * Compiles the print node to a {@link String} valued expression.
     * 
     * <p>If the node requires detach logic, it should use the given label as the reattach point.
     */
    Expression compileToString(PrintNode node, Label reattachPoint);

    /** 
     * Compiles the given CallNode to a statement that writes the result into the given appendable.
     * 
     * <p>The statement is guaranteed to be written to a location with a stack depth of zero.
     */
    Statement compileToBuffer(CallNode call, AppendableExpression appendable);

    /** 
     * Compiles the given MsgHtmlTagNode to a statement that writes the result into the given
     * appendable.
     * 
     * <p>The statement is guaranteed to be written to a location with a stack depth of zero.
     */
    Statement compileToBuffer(MsgHtmlTagNode htmlTagNode, AppendableExpression appendable);
  }

  private final Expression thisVar;
  private final DetachState detachState;
  private final VariableSet variables;
  private final VariableLookup variableLookup;
  private final AppendableExpression appendableExpression;
  private final SoyNodeToStringCompiler soyNodeCompiler;

  MsgCompiler(Expression thisVar,
      DetachState detachState,
      VariableSet variables,
      VariableLookup variableLookup,
      AppendableExpression appendableExpression,
      SoyNodeToStringCompiler soyNodeCompiler) {
    this.thisVar = checkNotNull(thisVar);
    this.detachState = checkNotNull(detachState);
    this.variables = checkNotNull(variables);
    this.variableLookup = checkNotNull(variableLookup);
    this.appendableExpression = checkNotNull(appendableExpression);
    this.soyNodeCompiler = checkNotNull(soyNodeCompiler);
  }

  /**
   * Compiles the given {@link MsgNode} to a statement with the given escaping directives applied.
   *
   * <p>The returned statement must be written to a location with a stack depth of zero.
   *
   * @param partsAndId The computed msg id
   * @param msg The msg node
   * @param escapingDirectives The set of escaping directives to apply.
   */
  Statement compileMessage(
      MsgPartsAndIds partsAndId, MsgNode msg, List<String> escapingDirectives) {
    Expression soyMsg =
        variableLookup
            .getRenderContext()
            .invoke(MethodRef.RENDER_CONTEXT_GET_SOY_MSG, constant(partsAndId.id));
    Statement printMsg;
    if (msg.isRawTextMsg()) {
      // Simplest case, just a static string translation
      printMsg = handleBasicTranslation(escapingDirectives, soyMsg);
    } else {
      // String translation + placeholders
      printMsg =
          handleTranslationWithPlaceholders(msg, escapingDirectives, soyMsg, partsAndId.parts);
    }
    return Statement.concat(
        printMsg.withSourceLocation(msg.getSourceLocation()),
        detachState.detachLimited(appendableExpression));
  }

  /**
   * Handles a translation consisting of a single raw text node.
   */
  private Statement handleBasicTranslation(List<String> escapingDirectives, Expression soyMsg) {
    // optimize for simple constant translations (very common)
    // this becomes: renderContext.getSoyMessge(<id>).getParts().get(o).getRawText()
    SoyExpression text = SoyExpression.forString(
        soyMsg.invoke(MethodRef.SOY_MSG_GET_PARTS)
            .invoke(MethodRef.LIST_GET, constant(0))
            .cast(SoyMsgRawTextPart.class)
            .invoke(MethodRef.SOY_MSG_RAW_TEXT_PART_GET_RAW_TEXT));
    for (String directive : escapingDirectives) {
      text = text.applyPrintDirective(variableLookup.getRenderContext(), directive);
    }
    return appendableExpression.appendString(text.coerceToString()).toStatement();
  }

  /**
   * Handles a complex message with placeholders.
   */
  private Statement handleTranslationWithPlaceholders(
      MsgNode msg,
      List<String> escapingDirectives,
      Expression soyMsg,
      ImmutableList<SoyMsgPart> parts) {
    // We need to render placeholders into a buffer and then pack them into a map to pass to
    // Runtime.renderSoyMsgWithPlaceholders.
    Expression placeholderMap = variables.getMsgPlaceholderMapField().accessor(thisVar);
    Map<String, Statement> placeholderNameToPutStatement = new LinkedHashMap<>();
    putPlaceholdersIntoMap(placeholderMap, msg, parts, placeholderNameToPutStatement);
    // sanity check
    checkState(!placeholderNameToPutStatement.isEmpty());
    variables.setMsgPlaceholderMapMinSize(placeholderNameToPutStatement.size());
    Statement populateMap = Statement.concat(placeholderNameToPutStatement.values());
    Statement clearMap = placeholderMap.invokeVoid(MethodRef.LINKED_HASH_MAP_CLEAR);
    Statement render;
    if (escapingDirectives.isEmpty()) {
      render = MethodRef.RUNTIME_RENDER_SOY_MSG_WITH_PLACEHOLDERS.invokeVoid(soyMsg,
          placeholderMap, appendableExpression);
    } else {
      // render into the handy buffer we already have!
      Statement renderToBuffer = MethodRef.RUNTIME_RENDER_SOY_MSG_WITH_PLACEHOLDERS.invokeVoid(
          soyMsg, placeholderMap, tempBuffer());
      // N.B. the type here is always 'string'
      SoyExpression value = SoyExpression.forString(
          tempBuffer().invoke(MethodRef.ADVISING_STRING_BUILDER_GET_AND_CLEAR));
      for (String directive : escapingDirectives) {
        value = value.applyPrintDirective(variableLookup.getRenderContext(), directive);
      }
      render =
          Statement.concat(
              renderToBuffer,
              appendableExpression.appendString(value.coerceToString()).toStatement());
    }
    Statement detach = detachState.detachLimited(appendableExpression);
    return Statement.concat(populateMap, render, clearMap, detach)
        .withSourceLocation(msg.getSourceLocation());
  }

  /**
   * Adds a {@link Statement} to {@link Map#put} every msg placeholder, plural variable and select
   * case value into {@code mapExpression}
   */
  private void putPlaceholdersIntoMap(
      Expression mapExpression,
      MsgNode originalMsg,
      Iterable<? extends SoyMsgPart> parts,
      Map<String, Statement> placeholderNameToPutStatement) {
    for (SoyMsgPart child : parts) {
      if (child instanceof SoyMsgRawTextPart || child instanceof SoyMsgPluralRemainderPart) {
        // raw text doesn't have placeholders and remainders use the same placeholder as plural they
        // are a member of.
        continue;
      }
      if (child instanceof SoyMsgPluralPart) {
        putPluralPartIntoMap(
            mapExpression, originalMsg, placeholderNameToPutStatement, (SoyMsgPluralPart) child);
      } else if (child instanceof SoyMsgSelectPart) {
        putSelectPartIntoMap(
            mapExpression, originalMsg, placeholderNameToPutStatement, (SoyMsgSelectPart) child);
      } else if (child instanceof SoyMsgPlaceholderPart) {
        putPlaceholderIntoMap(
            mapExpression,
            originalMsg,
            placeholderNameToPutStatement,
            (SoyMsgPlaceholderPart) child);
      } else {
        throw new AssertionError("unexpected child: " + child);
      }
    }
  }

  private void putSelectPartIntoMap(
      Expression mapExpression,
      MsgNode originalMsg,
      Map<String, Statement> placeholderNameToPutStatement,
      SoyMsgSelectPart select) {
    MsgSelectNode repSelectNode = originalMsg.getRepSelectNode(select.getSelectVarName());
    if (!placeholderNameToPutStatement.containsKey(select.getSelectVarName())) {
      Label reattachPoint = new Label();
      Expression value = soyNodeCompiler.compileToString(repSelectNode.getExpr(), reattachPoint);
      placeholderNameToPutStatement.put(
          select.getSelectVarName(),
          putToMap(mapExpression, select.getSelectVarName(), value).labelStart(reattachPoint));
    }
    // Recursively visit select cases
    for (Case<String> caseOrDefault : select.getCases()) {
      putPlaceholdersIntoMap(
          mapExpression, originalMsg, caseOrDefault.parts(), placeholderNameToPutStatement);
    }
  }

  private void putPluralPartIntoMap(
      Expression mapExpression,
      MsgNode originalMsg,
      Map<String, Statement> placeholderNameToPutStatement,
      SoyMsgPluralPart plural) {
    MsgPluralNode repPluralNode = originalMsg.getRepPluralNode(plural.getPluralVarName());
    if (!placeholderNameToPutStatement.containsKey(plural.getPluralVarName())) {
      Label reattachPoint = new Label();
      Expression value = soyNodeCompiler.compileToInt(repPluralNode.getExpr(), reattachPoint);
      placeholderNameToPutStatement.put(
          plural.getPluralVarName(),
          putToMap(mapExpression, plural.getPluralVarName(), value).labelStart(reattachPoint));
    }
    // Recursively visit plural cases
    for (Case<SoyMsgPluralCaseSpec> caseOrDefault : plural.getCases()) {
      putPlaceholdersIntoMap(
          mapExpression, originalMsg, caseOrDefault.parts(), placeholderNameToPutStatement);
    }
  }

  private void putPlaceholderIntoMap(
      Expression mapExpression,
      MsgNode originalMsg,
      Map<String, Statement> placeholderNameToPutStatement,
      SoyMsgPlaceholderPart placeholder)
      throws AssertionError {
    MsgPlaceholderNode repPlaceholderNode =
        originalMsg.getRepPlaceholderNode(placeholder.getPlaceholderName());
    String placeholderName = placeholder.getPlaceholderName();
    if (!placeholderNameToPutStatement.containsKey(placeholderName)) {
      StandaloneNode initialNode = repPlaceholderNode.getChild(0);
      Statement putEntyInMap;
      if (initialNode instanceof MsgHtmlTagNode) {
        putEntyInMap =
            addHtmlTagNodeToPlaceholderMap(
                mapExpression, placeholderName, (MsgHtmlTagNode) initialNode);
      } else if (initialNode instanceof CallNode) {
        putEntyInMap =
            addCallNodeToPlaceholderMap(mapExpression, placeholderName, (CallNode) initialNode);
      } else if (initialNode instanceof PrintNode) {
        putEntyInMap =
            addPrintNodeToPlaceholderMap(mapExpression, placeholderName, (PrintNode) initialNode);
      } else {
        // the AST for MsgNodes guarantee that these are the only options
        throw new AssertionError();
      }
      placeholderNameToPutStatement.put(placeholder.getPlaceholderName(), putEntyInMap);
    }
  }

  
  /**
   * Returns a statement that adds the content of the node to the map.
   *
   * @param mapExpression The map to put the new entry in
   * @param mapKey The map key
   * @param htmlTagNode The node
   */
  private Statement addHtmlTagNodeToPlaceholderMap(
      Expression mapExpression, String mapKey, MsgHtmlTagNode htmlTagNode) {
    Optional<String> rawText = tryGetRawTextContent(htmlTagNode);
    if (rawText.isPresent()) {
      return mapExpression
          .invoke(MethodRef.LINKED_HASH_MAP_PUT, constant(mapKey), constant(rawText.get()))
          .toStatement();
    } else {
      Statement renderIntoBuffer = soyNodeCompiler.compileToBuffer(htmlTagNode, tempBuffer());
      Statement putBuffer = putBufferIntoMapForPlaceholder(mapExpression, mapKey);
      return Statement.concat(renderIntoBuffer, putBuffer);
    }
  }

  /**
   * Returns a statement that adds the content rendered by the call to the map.
   * 
   * @param mapExpression The map to put the new entry in
   * @param mapKey The map key
   * @param callNode The node
   */
  private Statement addCallNodeToPlaceholderMap(
      Expression mapExpression, String mapKey, CallNode callNode) {
    Statement renderIntoBuffer = soyNodeCompiler.compileToBuffer(callNode, tempBuffer());
    Statement putBuffer = putBufferIntoMapForPlaceholder(mapExpression, mapKey);
    return Statement.concat(renderIntoBuffer, putBuffer);
  }

  /**
   * Returns a statement that adds the content rendered by the call to the map.
   *
   * @param mapExpression The map to put the new entry in
   * @param mapKey The map key
   * @param printNode The node
   */
  private Statement addPrintNodeToPlaceholderMap(
      Expression mapExpression, String mapKey, PrintNode printNode) {
    // This is much like the escaping path of visitPrintNode but somewhat simpler because our
    // ultimate target is a string rather than putting bytes on the output stream.
    Label reattachPoint = new Label();
    Expression compileToString = soyNodeCompiler.compileToString(printNode, reattachPoint);
    return putToMap(mapExpression, mapKey, compileToString).labelStart(reattachPoint);
  }

  private Statement putToMap(Expression mapExpression, String mapKey, Expression valueExpression) {
    return mapExpression
        .invoke(MethodRef.LINKED_HASH_MAP_PUT,
            constant(mapKey),
            valueExpression)
        .toStatement();
  }

  private AppendableExpression tempBuffer() {
    return AppendableExpression.forStringBuilder(variables.getTempBufferField().accessor(thisVar));
  }

  private Statement putBufferIntoMapForPlaceholder(Expression mapExpression, String mapKey) {
    return mapExpression
        .invoke(MethodRef.LINKED_HASH_MAP_PUT,
            constant(mapKey),
            tempBuffer().invoke(MethodRef.ADVISING_STRING_BUILDER_GET_AND_CLEAR))
        .toStatement();
  }

  private Optional<String> tryGetRawTextContent(ParentSoyNode<?> initialNode) {
    if (initialNode.numChildren() == 1 && initialNode.getChild(0) instanceof RawTextNode) {
      return Optional.of(((RawTextNode) initialNode.getChild(0)).getRawText());
    }
    return Optional.absent();
  }
}
