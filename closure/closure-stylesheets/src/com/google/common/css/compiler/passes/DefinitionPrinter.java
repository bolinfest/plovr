/*
 * Copyright 2010 Google Inc.
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

package com.google.common.css.compiler.passes;

import com.google.common.css.compiler.ast.CssCompilerPass;
import com.google.common.css.compiler.ast.CssDefinitionNode;
import com.google.common.css.compiler.ast.CssFunctionNode;
import com.google.common.css.compiler.ast.CssNode;
import com.google.common.css.compiler.ast.CssPriorityNode;
import com.google.common.css.compiler.ast.CssTree;
import com.google.common.css.compiler.ast.CssValueNode;

/**
 * Printer for definition nodes, which outputs GSS definitions so that
 * they can be re-parsed later.
 *
 * <p>This pass can only be used if {@link MapChunkAwareNodesToChunk}
 * pass has been run before. Otherwise this pass won't work.
 *
 * @param <T> type of chunk id objects
 *
 * @author dgajda@google.com (Damian Gajda)
 */
public class DefinitionPrinter<T> extends CodePrinter implements CssCompilerPass {

  private final T chunk;
  private boolean printDefinition;

  /**
   * Create a printer for all the definitions in the given chunk.
   *
   * @param subtree the subtree to be printed
   * @param chunk the selected chunk
   */
  public DefinitionPrinter(CssNode subtree, T chunk) {
    super(subtree);
    this.chunk = chunk;
  }

  /**
   * Create a printer for all the definitions in the given chunk.
   *
   * @param tree the css tree to be printed
   * @param chunk the selected chunk
   */
  public DefinitionPrinter(CssTree tree, T chunk) {
    super(tree);
    this.chunk = chunk;
  }

  @Override
  public boolean enterDefinition(CssDefinitionNode definition) {
    printDefinition = chunk.equals(definition.getChunk());
    if (printDefinition) {
      buffer.append("@def ").append(definition.getName()).append(' ');
    }
    return printDefinition;
  }

  @Override
  public void leaveDefinition(CssDefinitionNode node) {
    if (printDefinition) {
      buffer.append(";").startNewLine();
      printDefinition = false;
    }
  }

  @Override
  public boolean enterValueNode(CssValueNode node) {
    if (!printDefinition) {
      return false;
    }
    if (node instanceof CssPriorityNode) {
      buffer.deleteLastChar();
    }
    buffer.append(node);
    return true;
  }

  @Override
  public void leaveValueNode(CssValueNode node) {
    if (!printDefinition) {
      return;
    }
    buffer.append(' ');
  }

  @Override
  public boolean enterFunctionNode(CssFunctionNode node) {
    if (!printDefinition) {
      return false;
    }
    buffer.append(node.getFunctionName());
    buffer.append('(');
    return true;
  }

  @Override
  public void leaveFunctionNode(CssFunctionNode node) {
    if (!printDefinition) {
      return;
    }
    buffer.append(") ");
  }

  @Override
  public boolean enterArgumentNode(CssValueNode node) {
    if (!printDefinition) {
      return false;
    }
    // If the previous argument was a function node, then it has a trailing
    // space that needs to be removed.
    buffer.deleteLastCharIfCharIs(' ');
    buffer.append(node);
    return true;
  }

  /**
   * Returns a GSS output with all the printed definitions.
   */
  public String getDefinitionGss() {
    return getOutputBuffer();
  }

  @Override
  public void runPass() {
    resetBuffer();
    visitController.startVisit(this);
  }
}
