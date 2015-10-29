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

import com.google.common.base.Optional;
import com.google.template.soy.base.SourceLocation;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A type that can produce bytecode.
 */
abstract class BytecodeProducer {
  /**
   * This bit tracks whether or not the current thread is generating code.
   *
   * <p>This is used to enforce an invariant that creation of {@link BytecodeProducer} instances
   * should not occur during code generation.  This is because BytecodeProducer instances tend to
   * trigger verification checks and mutate mutable data structures as part of their initialization.
   * Accidentally delaying this work until code generation time is an easy mistake to make and it
   * may cause undefined behavior.
   *
   * <p>TODO(lukes): this thread local is a little magical, consider introducing an explicit
   * 'compilation state' or 'compiler' object in which this phase information could be stored.
   */
  private static final ThreadLocal<Boolean> isGenerating =
      Flags.DEBUG
          ? new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
              return false;
            }
          }
          : null;

  // Optional because there will be many situations where having a source location is not possible
  // e.g. NULL_STATEMENT, exprnodes (currently).  In general we should attempt to associate source
  // locations whenever we have one.
  private final Optional<SourceLocation> location;

  BytecodeProducer() {
    this(Optional.<SourceLocation>absent());
  }

  // when UNKNOWN source locations go away, so can this use of isKnown
  @SuppressWarnings("deprecation")
  BytecodeProducer(SourceLocation location) {
    this(location.isKnown() ? Optional.of(location) : Optional.<SourceLocation>absent());
  }

  private BytecodeProducer(Optional<SourceLocation> location) {
    if (Flags.DEBUG && isGenerating.get()) {
      throw new IllegalStateException(
          "All bytecode producers should be created prior to code generation beginning.");
    }
    this.location = location;
  }

  /** Writes the bytecode to the adapter. */
  final void gen(CodeBuilder adapter) {
    boolean shouldClearIsGeneratingBit = false;
    if (Flags.DEBUG && !isGenerating.get()) {
      isGenerating.set(true);
      shouldClearIsGeneratingBit = true;
    }
    try {
      if (location.isPresent()) {
        // These add entries to the line number tables that are associated with the current method.
        // The line number table is just a mapping of of bytecode offset (aka 'pc') to line number,
        // http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.12
        // It is used by the JVM to add source data to stack traces and by debuggers to highlight
        // source files.
        Label start = new Label();
        adapter.mark(start);
        adapter.visitLineNumber(location.get().getLineNumber(), start);
      }

      doGen(adapter);

      if (location.isPresent()) {
        Label end = new Label();
        adapter.mark(end);
        adapter.visitLineNumber(location.get().getEndLine(), end);
      }
    } finally {
      if (shouldClearIsGeneratingBit) {
        isGenerating.set(false);
      }
    }
  }

  abstract void doGen(CodeBuilder adapter);

  /**
   * Returns a human readable string for the code that this {@link BytecodeProducer} generates.
   */
  final String trace() {
    // TODO(lukes): textifier has support for custom label names by overriding appendLabel.
    // Consider trying to make use of (using the Label.info field? adding a custom NamedLabel
    // sub type?)
    Textifier textifier = new Textifier(Opcodes.ASM5) {
      {
        // reset tab sizes.  Since we don't care about formatting class names or method signatures
        // (only code). We only need to set the tab2,tab3 and ltab settings (tab is for class
        // members).
        this.tab = null;  // trigger an error if used.
        this.tab2 = "  ";  // tab setting for instructions
        this.tab3 = "";  // tab setting for switch cases
        this.ltab = "";  // tab setting for labels
      }
    };
    gen(new CodeBuilder(new TraceMethodVisitor(textifier), 0, "trace", "()V"));
    StringWriter writer = new StringWriter();
    textifier.print(new PrintWriter(writer));
    return writer.toString();  // Note textifier always adds a trailing newline
  }
}
