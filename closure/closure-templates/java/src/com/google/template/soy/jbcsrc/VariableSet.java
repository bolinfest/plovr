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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.template.soy.jbcsrc.BytecodeUtils.constant;
import static com.google.template.soy.jbcsrc.StandardNames.CURRENT_CALLEE_FIELD;
import static com.google.template.soy.jbcsrc.StandardNames.CURRENT_RENDEREE_FIELD;
import static com.google.template.soy.jbcsrc.StandardNames.MSG_PLACEHOLDER_MAP_FIELD;
import static com.google.template.soy.jbcsrc.StandardNames.TEMP_BUFFER_FIELD;

import com.google.auto.value.AutoValue;
import com.google.template.soy.data.SoyValueProvider;
import com.google.template.soy.jbcsrc.VariableSet.VarKey.Kind;
import com.google.template.soy.jbcsrc.api.AdvisingStringBuilder;
import com.google.template.soy.jbcsrc.shared.CompiledTemplate;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

/**
 * A variable in this set is a SoyValue that must be saved/restored.  This means each variable has:
 * 
 * <ul>
 *     <li>A {@link FieldRef} that can be used to define the field.
 *     <li>A {@link Statement} that can be used to save the field.
 *     <li>A {@link Statement} that can be used to restore the field.
 *     <li>A {@link LocalVariable} that can be used to read the value.
 * </ul>
 */
final class VariableSet {
  enum SaveStrategy {
    /** Means that the value of the variable should be recalculated rather than saved to a field. */
    DERIVED,
    /** Means that the value of the variable should saved to a field. */
    STORE;
  }

  abstract class Scope {
    private Scope() {}

    /**
     * Creates a new 'synthetic' variable.  A synthetic variable is a variable that is
     * introduced by the compiler rather than a user defined name.
     *
     * @param name A proposed name for the variable, the actual variable name may be modified to 
     *     ensure uniqueness
     * @param initializer The expression that can be used to derive the initial value.  Note, this
     *     expression must be save to gen() more than once if {@code isDerived} is {@code true}
     * @param strategy Set this if the value of the variable is trivially derivable from other 
     *     variables already defined.
     */
    abstract Variable createSynthetic(SyntheticVarName name, Expression initializer, 
        SaveStrategy strategy);

    /**
     * Creates a new 'synthetic' variable.  A synthetic variable is a variable that is
     * introduced by the compiler rather than a user defined name.
     *
     * @param name The name of the variable, the name is assumed to be unique (enforced by the 
     *     ResolveNamesVisitor).
     * @param initializer The expression that can be used to initialize the variable
     */
    abstract Variable create(String name, Expression initializer, SaveStrategy strategy);

    /**
     * Returns a statement that should be used when exiting the scope.  This is responsible for
     * appropriately clearing fields and visiting end labels.
     */
    abstract Statement exitScope();
  }

  /**
   * A sufficiently unique identifier.
   * 
   * <p>This key will uniquely identify a currently 'active' variable, but may not be unique over
   * all possible variables.
   */
  @AutoValue abstract static class VarKey {
    enum Kind {
      /** 
       * Includes @param, @inject, {let..}, and loop vars.
       * 
       * <p>Uniqueness of local variable names is enforced by the ResolveNamesVisitor pass, we
       * just need uniqueness for the field names 
       */
      USER_DEFINED,
      /**
       * There are certain operations in which a value must be used multiple times and may have
       * expensive initialization. For example, the collection being looped over in a
       * {@code foreach} loop.  For these we generate 'synthetic' variables to efficiently reference
       * the expression.
       */
      SYNTHETIC;
    }
    static VarKey create(Kind synthetic, String proposedName) {
      return new AutoValue_VariableSet_VarKey(synthetic, proposedName);
    }

    abstract Kind kind();
    abstract String name();
  }

  /**
   * A variable that may need to be saved/restored.
   */
  abstract class Variable {
    protected final Expression initExpression;
    protected final LocalVariable local;
    private final Statement initializer;

    private Variable(Expression initExpression, LocalVariable local) {
      this.initExpression = initExpression;
      this.local = local;
      this.initializer = local.store(initExpression, local.start());
    }

    final Statement initializer() {
      return initializer;
    }

    abstract Statement save();

    abstract Statement restore();

    abstract void maybeDefineField(ClassVisitor writer);

    LocalVariable local() {
      return local;
    }
  }

  private final class FieldSavedVariable extends Variable {
    private FieldRef fieldRef;  // lazily allocated on a save/restore operation

    private FieldSavedVariable(Expression initExpression, LocalVariable local) {
      super(initExpression, local);
    }

    @Override Statement save() {
      return getField().putInstanceField(thisVar, local);
    }

    @Override Statement restore() {
      Expression fieldValue = getField().accessor(thisVar);
      return local.store(fieldValue);
    }

    private FieldRef getField() {
      if (fieldRef == null) {
        fieldRef = FieldRef.createField(owner, local.variableName(), local.resultType());
      }
      return fieldRef;
    }

    @Override void maybeDefineField(ClassVisitor writer) {
      if (fieldRef != null) {
        fieldRef.defineField(writer);
      }
    }
  }
  
  private final class DerivedVariable extends Variable {
    private DerivedVariable(Expression initExpression, LocalVariable local) {
      super(initExpression, local);
    }

    @Override Statement save() {
      return Statement.NULL_STATEMENT;
    }

    @Override Statement restore() {
      return local.store(initExpression);
    }

    @Override void maybeDefineField(ClassVisitor writer) {}
  }

  private final List<Variable> allVariables = new ArrayList<>();
  private final Deque<Map<VarKey, Variable>> frames = new ArrayDeque<>();
  private final UniqueNameGenerator fieldNames;
  private final BitSet availableSlots = new BitSet();
  private final TypeInfo owner;
  private final LocalVariable thisVar;
  // Allocated lazily
  @Nullable private FieldRef currentCalleeField; 
  // Allocated lazily
  @Nullable private FieldRef currentRendereeField; 
  // Allocated lazily
  @Nullable private FieldRef tempBufferField;
  // Allocated lazily
  @Nullable private FieldRef msgPlaceholderMapField;
  private int msgPlaceholderMapInitialSize = 0;

  /**
   * @param owner The type that is the owner of the method being generated
   * @param thisVar An expression returning the current 'this' reference
   * @param method The method being generated
   * @param fieldNames The field name set for the current class.
   */
  VariableSet(UniqueNameGenerator fieldNames, TypeInfo owner, LocalVariable thisVar,
      Method method) {
    this.fieldNames = fieldNames;
    this.fieldNames.claimName(CURRENT_CALLEE_FIELD);
    this.fieldNames.claimName(CURRENT_RENDEREE_FIELD);
    this.fieldNames.claimName(TEMP_BUFFER_FIELD);
    this.fieldNames.claimName(MSG_PLACEHOLDER_MAP_FIELD);
    this.owner = owner;
    this.thisVar = thisVar;
    availableSlots.set(0);   // for 'this'
    int from = 1;
    for (Type type : method.getArgumentTypes()) {
      int to = from + type.getSize();
      availableSlots.set(from, to);
      from = to;
    }
  }

  /**
   * Enters a new scope.  Variables may only be defined within a scope.
   */
  Scope enterScope() {
    final Map<VarKey, Variable> currentFrame = new LinkedHashMap<>();
    final Label scopeExit = new Label();
    frames.push(currentFrame);
    return new Scope() {
      @Override Variable createSynthetic(
          SyntheticVarName varName, Expression initExpr, SaveStrategy strategy) {
        VarKey key = VarKey.create(Kind.SYNTHETIC, varName.name());
        // synthetics are prefixed by $ by convention
        String name = fieldNames.generateName("$" + varName.name());
        return doCreate(name, new Label(), scopeExit, initExpr, key, strategy);
      }

      @Override Variable create(String name, Expression initExpr, SaveStrategy strategy) {
        VarKey key = VarKey.create(Kind.USER_DEFINED, name);
        name = fieldNames.generateName(name);
        return doCreate(name, new Label(), scopeExit, initExpr, key, strategy);
      }

      @Override Statement exitScope() {
        frames.pop();
        // Use identity semantics to make sure we visit each label at most once.  visiting a label
        // more than once tends to corrupt internal asm state.
        final Set<Label> endLabels =
            Collections.newSetFromMap(new IdentityHashMap<Label, Boolean>());
        for (Variable var : currentFrame.values()) {
          endLabels.add(var.local.end());
          availableSlots.clear(var.local.index(),
              var.local.index() + var.local.resultType().getSize());
        }
        return new Statement() {
          // TODO(lukes): we could generate null writes for when object typed fields go out of
          // scope.  This would potentially allow intermediate results to be collected sooner.
          @Override void doGen(CodeBuilder adapter) {
            for (Label label : endLabels) {
              adapter.visitLabel(label);
            }
          }
        };
      }

      private Variable doCreate(String name, Label start, Label end, Expression initExpr, 
          VarKey key, SaveStrategy strategy) {
        int index = reserveSlotFor(initExpr.resultType());
        LocalVariable local =
            LocalVariable.createLocal(name, index, initExpr.resultType(), start, end);
        Variable var;
        switch (strategy) {
          case DERIVED:
            var = new DerivedVariable(initExpr, local);
            break;
          case STORE:
            var = new FieldSavedVariable(initExpr, local);
            break;
          default:
            throw new AssertionError();
        }
        currentFrame.put(key, var);
        allVariables.add(var);
        return var;
      }
    };
  }

  /** Write a local variable table entry for every registered variable. */
  void generateTableEntries(CodeBuilder ga) {
    for (Variable var : allVariables) {
      var.local.tableEntry(ga);
    }
  }

  // TODO(lukes): consider moving all these optional 'one per template' fields to a different object
  // for management.

  /** 
   * Defines all the fields necessary for the registered variables.
   * 
   * @return a statement to initialize the fields
   */
  @CheckReturnValue Statement defineFields(ClassVisitor writer) {
    List<Statement> initializers = new ArrayList<>();
    for (Variable var : allVariables) {
      var.maybeDefineField(writer);
    }
    if (currentCalleeField != null) {
      currentCalleeField.defineField(writer);
    }
    if (currentRendereeField != null) {
      currentRendereeField.defineField(writer);
    }
    if (tempBufferField != null) {
      tempBufferField.defineField(writer);
      // If a template needs a temp buffer then we initialize it eagerly in the template constructor
      // this may be wasteful in the case that the buffer is only used on certain call paths, but
      // if it turns out to be expensive, this could always be solved by an author by refactoring
      // their templates (e.g. extract the conditional logic into another template)
      final Expression newStringBuilder = ConstructorRef.ADVISING_STRING_BUILDER.construct();
      initializers.add(new Statement() {
        @Override void doGen(CodeBuilder adapter) {
          adapter.loadThis();
          newStringBuilder.gen(adapter);
          tempBufferField.putUnchecked(adapter);
        }
      });
    }
    if (msgPlaceholderMapField != null) {
      msgPlaceholderMapField.defineField(writer);
      // same comment as above about eager initialization.
      final Expression newHashMap =
          ConstructorRef.LINKED_HASH_MAP_SIZE.construct(constant(msgPlaceholderMapInitialSize));
      initializers.add(new Statement() {
        @Override void doGen(CodeBuilder adapter) {
          adapter.loadThis();
          newHashMap.gen(adapter);
          msgPlaceholderMapField.putUnchecked(adapter);
        }
      });
    }
    return Statement.concat(initializers);
  }

  /**
   * Returns the field that holds the current callee template.
   * 
   * <p>Unlike normal variables the VariableSet doesn't maintain responsibility for saving and
   * restoring the current callee to a local.
   */
  FieldRef getCurrentCalleeField() {
    FieldRef local = currentCalleeField;
    if (local == null) {
      local = currentCalleeField = 
          FieldRef.createField(owner, CURRENT_CALLEE_FIELD, CompiledTemplate.class);
    }
    return local;
  }

  /**
   * Returns the field that holds the current temp buffer.
   */
  FieldRef getTempBufferField() {
    FieldRef local = tempBufferField;
    if (local == null) {
      local = tempBufferField =
          FieldRef.createFinalField(owner, TEMP_BUFFER_FIELD, AdvisingStringBuilder.class)
              .asNonNull();
    }
    return local;
  }

  /**
   * Returns the field that holds a map used for rendering msg placeholders.
   */
  FieldRef getMsgPlaceholderMapField() {
    FieldRef local = msgPlaceholderMapField;
    if (local == null) {
      local = msgPlaceholderMapField =
          FieldRef.createFinalField(owner, MSG_PLACEHOLDER_MAP_FIELD, LinkedHashMap.class)
              .asNonNull();
    }
    return local;
  }

  /**
   * Configures a minimum size for the {@link #getMsgPlaceholderMapField()}.
   */
  void setMsgPlaceholderMapMinSize(int size) {
    // we use the max of all the requested minimum sizes for the initial size
    this.msgPlaceholderMapInitialSize = Math.max(msgPlaceholderMapInitialSize, size);
  }

  /**
   * Returns the field that holds the currently rendering SoyValueProvider.
   * 
   * <p>Unlike normal variables the VariableSet doesn't maintain responsibility for saving and
   * restoring the current renderee to a local.
   */
  FieldRef getCurrentRenderee() {
    FieldRef local = currentRendereeField;
    if (local == null) {
      local = currentRendereeField = 
          FieldRef.createField(owner, CURRENT_RENDEREE_FIELD, SoyValueProvider.class);
    }
    return local;
  }

  /**
   * Looks up a user defined variable with the given name.  The variable must have been created
   * in a currently active scope.
   */
  Variable getVariable(String name) {
    VarKey varKey = VarKey.create(Kind.USER_DEFINED, name);
    return getVariable(varKey);
  }

  /**
   * Looks up a synthetic variable with the given name.  The variable must have been created
   * in a currently active scope.
   */
  Variable getVariable(SyntheticVarName name) {
    VarKey varKey = VarKey.create(Kind.SYNTHETIC, name.name());
    return getVariable(varKey);
  }

  private Variable getVariable(VarKey varKey) {
    Variable potentialMatch = null;
    for (Map<VarKey, Variable> f : frames) {
      Variable variable = f.get(varKey);
      if (variable != null) {
        if (potentialMatch == null) {
          potentialMatch = variable;
        } else {
          throw new IllegalArgumentException("Ambiguous variable: " + varKey);
        }
      }
    }
    if (potentialMatch != null) {
      return potentialMatch;
    }
    throw new IllegalArgumentException("No variable: '" + varKey + "' is bound");
  }

  /** Statements for saving and restoring local variables in class fields. */
  @AutoValue abstract static class SaveRestoreState {
    abstract Statement save();
    abstract Statement restore();
  }

  /** Returns a {@link SaveRestoreState} for the current state of the variable set. */
  SaveRestoreState saveRestoreState() {
    List<Statement> saves = new ArrayList<>();
    List<Statement> restores = new ArrayList<>();
    // Iterate backwards so that we restore variables in order of definition which will ensure that
    // derived fields work correctly.
    for (Iterator<Map<VarKey, Variable>> iterator = frames.descendingIterator();
        iterator.hasNext();) {
      Map<VarKey, Variable> frame = iterator.next();
      for (Variable var : frame.values()) {
        saves.add(var.save());
        restores.add(var.restore());
      }
    }
    return new AutoValue_VariableSet_SaveRestoreState(
        Statement.concat(saves), Statement.concat(restores));
  }

  private int reserveSlotFor(Type type) {
    int size = type.getSize();
    checkArgument(size != 0);
    int start = 0;
    while (true) {
      int nextClear = availableSlots.nextClearBit(start);
      if (size == 2 && availableSlots.get(nextClear + 1)) {
        start = nextClear + 1;
      }
      availableSlots.set(nextClear, nextClear + size);
      return nextClear;
    }
  }
}

