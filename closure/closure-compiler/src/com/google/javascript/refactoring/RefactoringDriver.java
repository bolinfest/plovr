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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DependencyOptions;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.NodeTraversal;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.rhino.Node;

import java.util.List;

/**
 * Primary driver of a refactoring. This class collects the inputs, runs the refactoring over
 * the compiled input, and then collects the suggested fixes based on the refactoring.
 *
 * @author mknichel@google.com (Mark Knichel)
 */
public final class RefactoringDriver {

  private final Scanner scanner;
  private final Compiler compiler;
  private final Node rootNode;

  private RefactoringDriver(
      Scanner scanner,
      List<SourceFile> inputs,
      List<SourceFile> externs,
      CompilerOptions compilerOptions) {
    this.scanner = scanner;
    this.compiler = createCompiler(inputs, externs, compilerOptions);
    this.rootNode = this.compiler.getRoot();
  }

  /**
   * Run the refactoring and return any suggested fixes as a result.
   */
  public List<SuggestedFix> drive() {
    JsFlumeCallback callback = new JsFlumeCallback(scanner, null);
    NodeTraversal.traverseEs6(compiler, rootNode, callback);
    List<SuggestedFix> fixes = callback.getFixes();
    fixes.addAll(scanner.processAllMatches(callback.getMatches()));
    return fixes;
  }

  public Compiler getCompiler() {
    return compiler;
  }

  private Compiler createCompiler(
      List<SourceFile> inputs, List<SourceFile> externs, CompilerOptions compilerOptions) {
    Compiler compiler = new Compiler();
    compiler.disableThreads();
    compiler.compile(externs, inputs, compilerOptions);
    return compiler;
  }

  @VisibleForTesting
  static CompilerOptions getCompilerOptions() {
    CompilerOptions options = new CompilerOptions();

    DependencyOptions deps = new DependencyOptions();
    deps.setDependencySorting(true);
    options.setDependencyOptions(deps);

    options.setIdeMode(true);
    options.setCheckSuspiciousCode(true);
    options.setCheckSymbols(true);
    options.setCheckTypes(true);
    options.setClosurePass(true);
    options.setPreserveGoogRequires(true);

    options.setWarningLevel(DiagnosticGroups.MISSING_REQUIRE, CheckLevel.ERROR);

    return options;
  }

  public static class Builder {
    private static final Function<String, SourceFile> TO_SOURCE_FILE_FN =
        new Function<String, SourceFile>() {
          @Override public SourceFile apply(String file) {
            return new SourceFile.Builder().buildFromFile(file);
          }
        };

    private final Scanner scanner;
    private final ImmutableList.Builder<SourceFile> inputs = ImmutableList.builder();
    private final ImmutableList.Builder<SourceFile> externs = ImmutableList.builder();
    private CompilerOptions compilerOptions = getCompilerOptions();

    public Builder(Scanner scanner) {
      this.scanner = scanner;
    }

    public Builder addExternsFromFile(String filename) {
      externs.add(SourceFile.fromFile(filename));
      return this;
    }

    public Builder addExternsFromCode(String code) {
      externs.add(SourceFile.fromCode("externs", code));
      return this;
    }

    public Builder addExterns(Iterable<SourceFile> externs) {
      this.externs.addAll(externs);
      return this;
    }

    public Builder addExternsFromFile(Iterable<String> externs) {
      this.externs.addAll(Lists.transform(ImmutableList.copyOf(externs), TO_SOURCE_FILE_FN));
      return this;
    }

    public Builder addInputsFromFile(String filename) {
      inputs.add(SourceFile.fromFile(filename));
      return this;
    }

    public Builder addInputsFromCode(String code) {
      return addInputsFromCode(code, "input");
    }

    public Builder addInputsFromCode(String code, String filename) {
      inputs.add(SourceFile.fromCode(filename, code));
      return this;
    }

    public Builder addInputs(Iterable<SourceFile> inputs) {
      this.inputs.addAll(inputs);
      return this;
    }

    public Builder addInputsFromFile(Iterable<String> inputs) {
      this.inputs.addAll(Lists.transform(ImmutableList.copyOf(inputs), TO_SOURCE_FILE_FN));
      return this;
    }

    public Builder withCompilerOptions(CompilerOptions compilerOptions) {
      this.compilerOptions = Preconditions.checkNotNull(compilerOptions);
      return this;
    }

    public RefactoringDriver build() {
      return new RefactoringDriver(scanner, inputs.build(), externs.build(), compilerOptions);
    }
  }
}
