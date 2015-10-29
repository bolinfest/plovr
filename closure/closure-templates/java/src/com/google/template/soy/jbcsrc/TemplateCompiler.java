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

import static com.google.template.soy.jbcsrc.BytecodeUtils.ADVISING_APPENDABLE_TYPE;
import static com.google.template.soy.jbcsrc.BytecodeUtils.NULLARY_INIT;
import static com.google.template.soy.jbcsrc.BytecodeUtils.OBJECT;
import static com.google.template.soy.jbcsrc.BytecodeUtils.RENDER_CONTEXT_TYPE;
import static com.google.template.soy.jbcsrc.BytecodeUtils.SOY_RECORD_TYPE;
import static com.google.template.soy.jbcsrc.FieldRef.createField;
import static com.google.template.soy.jbcsrc.FieldRef.createFinalField;
import static com.google.template.soy.jbcsrc.LocalVariable.createLocal;
import static com.google.template.soy.jbcsrc.LocalVariable.createThisVar;
import static com.google.template.soy.jbcsrc.StandardNames.IJ_FIELD;
import static com.google.template.soy.jbcsrc.StandardNames.PARAMS_FIELD;
import static com.google.template.soy.jbcsrc.StandardNames.STATE_FIELD;

import com.google.common.collect.ImmutableMap;
import com.google.template.soy.data.SoyRecord;
import com.google.template.soy.data.SoyValueProvider;
import com.google.template.soy.jbcsrc.SoyNodeCompiler.CompiledMethodBody;
import com.google.template.soy.jbcsrc.shared.CompiledTemplate;
import com.google.template.soy.jbcsrc.shared.TemplateMetadata;
import com.google.template.soy.soytree.CallParamContentNode;
import com.google.template.soy.soytree.CallParamValueNode;
import com.google.template.soy.soytree.LetContentNode;
import com.google.template.soy.soytree.LetValueNode;
import com.google.template.soy.soytree.TemplateNode;
import com.google.template.soy.soytree.defn.LocalVar;
import com.google.template.soy.soytree.defn.TemplateParam;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Compiles the top level {@link CompiledTemplate} class for a single template and all related
 * classes.
 */
final class TemplateCompiler {
  private static final String TEMPLATE_METADATA_DESCRIPTOR =
      Type.getDescriptor(TemplateMetadata.class);
  private static final TypeInfo TEMPLATE_TYPE = TypeInfo.create(CompiledTemplate.class);

  private final CompiledTemplateRegistry registry;
  private final FieldRef paramsField;
  private final FieldRef ijField;
  private final FieldRef stateField;
  private final UniqueNameGenerator fieldNames = UniqueNameGenerator.forFieldNames();
  private final ImmutableMap<String, FieldRef> paramFields;
  private final CompiledTemplateMetadata template;
  private final InnerClasses innerClasses;
  private SoyClassWriter writer;

  TemplateCompiler(CompiledTemplateRegistry registry, CompiledTemplateMetadata template) {
    this.registry = registry;
    this.template = template;
    TypeInfo ownerType = template.typeInfo();
    this.paramsField = createFinalField(ownerType, PARAMS_FIELD, SoyRecord.class).asNonNull();
    this.ijField = createFinalField(ownerType, IJ_FIELD, SoyRecord.class).asNonNull();
    this.stateField = createField(ownerType, STATE_FIELD, Type.INT_TYPE);
    this.innerClasses = new InnerClasses(ownerType);
    fieldNames.claimName(PARAMS_FIELD);
    fieldNames.claimName(IJ_FIELD);
    fieldNames.claimName(STATE_FIELD);
    ImmutableMap.Builder<String, FieldRef> builder = ImmutableMap.builder();
    for (TemplateParam param : template.node().getAllParams()) {
      String name = param.name();
      fieldNames.claimName(name);
      builder.put(name, createFinalField(ownerType, name, SoyValueProvider.class).asNonNull());
    }
    this.paramFields = builder.build();
  }

  /**
   * Returns the list of classes needed to implement this template.
   *
   * <p>For each template, we generate:
   * <ul>
   *     <li>A {@link com.google.template.soy.jbcsrc.shared.CompiledTemplate.Factory}
   *     <li>A {@link CompiledTemplate}
   *     <li>A SoyAbstractCachingProvider subclass for each {@link LetValueNode} and 
   *         {@link CallParamValueNode}
   *     <li>A RenderableThunk subclass for each {@link LetContentNode} and 
   *         {@link CallParamContentNode}
   * </li>
   * 
   * <p>Note:  This will <em>not</em> generate classes for other templates, only the template
   * configured in the constructor.  But it will generate classes that <em>reference</em> the 
   * classes that are generated for other templates.  It is the callers responsibility to ensure
   * that all referenced templates are generated and available in the classloader that ultimately
   * loads the returned classes.
   */
  Iterable<ClassData> compile() {
    List<ClassData> classes = new ArrayList<>();

    // first generate the factory
    // TODO(lukes): don't generate factory if the template is private?  The factories are only
    // useful to instantiate templates for calls from java.  Soy->Soy calls should invoke 
    // constructors directly.
    new TemplateFactoryCompiler(template, innerClasses).compile();

    writer =
        SoyClassWriter.builder(template.typeInfo())
            .setAccess(Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_FINAL)
            .implementing(TEMPLATE_TYPE)
            .sourceFileName(template.node().getSourceLocation().getFileName())
            .build();
    generateTemplateMetadata();
    
    stateField.defineField(writer);
    paramsField.defineField(writer);
    ijField.defineField(writer);
    for (FieldRef field : paramFields.values()) {
      field.defineField(writer);
    }

    Statement fieldInitializers = generateRenderMethod();

    generateConstructor(fieldInitializers);
    
    innerClasses.registerAllInnerClasses(writer);
    writer.visitEnd();

    classes.add(writer.toClassData());
    classes.addAll(innerClasses.getInnerClassData());
    writer = null;
    return classes;
  }

  /** Writes a {@link TemplateMetadata} to the generated class. */
  private void generateTemplateMetadata() {
    AnnotationVisitor annotationWriter =
        writer.visitAnnotation(TEMPLATE_METADATA_DESCRIPTOR, true /* visible at runtime */);
    String kind = template.node().getContentKind() == null 
        ? ""
        : template.node().getContentKind().name(); 
    annotationWriter.visit("contentKind", kind);
    annotationWriter.visitEnd();
  }

  private Statement generateRenderMethod() {
    final Label start = new Label();
    final Label end = new Label();
    final LocalVariable thisVar = createThisVar(template.typeInfo(), start, end);
    final LocalVariable appendableVar =
        createLocal("appendable", 1, ADVISING_APPENDABLE_TYPE, start, end).asNonNullable();
    final LocalVariable contextVar =
        createLocal("context", 2, RENDER_CONTEXT_TYPE, start, end).asNonNullable();
    final VariableSet variableSet = 
        new VariableSet(fieldNames, template.typeInfo(), thisVar, template.renderMethod().method());
    TemplateNode node = template.node();
    TemplateVariables variables = 
        new TemplateVariables(variableSet, thisVar, contextVar);
    final CompiledMethodBody methodBody =
        SoyNodeCompiler.create(
            registry,
            innerClasses,
            stateField,
            thisVar,
            AppendableExpression.forLocal(appendableVar),
            variableSet,
            variables).compile(node);
    final Statement returnDone = Statement.returnExpression(MethodRef.RENDER_RESULT_DONE.invoke());
    new Statement() {
      @Override
      void doGen(CodeBuilder adapter) {
        adapter.mark(start);
        methodBody.body().gen(adapter);
        adapter.mark(end);
        returnDone.gen(adapter);

        thisVar.tableEntry(adapter);
        appendableVar.tableEntry(adapter);
        contextVar.tableEntry(adapter);
        variableSet.generateTableEntries(adapter);
      }
    }.writeIOExceptionMethod(Opcodes.ACC_PUBLIC, template.renderMethod().method(), writer);
    writer.setNumDetachStates(methodBody.numberOfDetachStates());
    return variableSet.defineFields(writer);
  }

  /** 
   * Generate a public constructor that assigns our final field and checks for missing required 
   * params.
   * 
   * <p>This constructor is called by the generate factory classes.
   *
   * @param fieldInitializers additional statements to initialize fields (other than params)
   */
  private void generateConstructor(Statement fieldInitializers) {
    final Label start = new Label();
    final Label end = new Label();
    final LocalVariable thisVar = createThisVar(template.typeInfo(), start, end);
    final LocalVariable paramsVar = createLocal("params", 1, SOY_RECORD_TYPE, start, end);
    final LocalVariable ijVar = createLocal("ij", 2, SOY_RECORD_TYPE, start, end);
    final List<Statement> assignments = new ArrayList<>();
    assignments.add(fieldInitializers);  // for other fields needed by the compiler.
    assignments.add(paramsField.putInstanceField(thisVar, paramsVar));
    assignments.add(ijField.putInstanceField(thisVar, ijVar));
    for (final TemplateParam param : template.node().getAllParams()) {
      Expression paramProvider = getParam(paramsVar, ijVar, param);
      assignments.add(paramFields.get(param.name()).putInstanceField(thisVar, paramProvider));
    }
    Statement constructorBody =
        new Statement() {
          @Override
          void doGen(CodeBuilder ga) {
            ga.mark(start);
            // call super()
            thisVar.gen(ga);
            ga.invokeConstructor(OBJECT.type(), NULLARY_INIT);
            for (Statement assignment : assignments) {
              assignment.gen(ga);
            }
            ga.visitInsn(Opcodes.RETURN);
            ga.visitLabel(end);
            thisVar.tableEntry(ga);
            paramsVar.tableEntry(ga);
            ijVar.tableEntry(ga);
          }
        };
    constructorBody.writeMethod(Opcodes.ACC_PUBLIC, template.constructor().method(), writer);
  }

  /**
   * Returns an expression that fetches the given param from the params record or the ij record and
   * enforces the {@link TemplateParam#isRequired()} flag, throwing SoyDataException if a required
   * parameter is missing. 
   */
  private static Expression getParam(
      LocalVariable paramsVar, LocalVariable ijVar, TemplateParam param) {
    Expression fieldName = BytecodeUtils.constant(param.name());
    Expression record = param.isInjected() ? ijVar : paramsVar;
    // NOTE: for compatibility with Tofu and jssrc we do not check for missing required parameters
    // here instead they will just turn into null.  Existing templates depend on this.
    return MethodRef.RUNTIME_GET_FIELD_PROVIDER.invoke(record, fieldName);
  }

  private final class TemplateVariables implements VariableLookup {
    private final VariableSet variableSet;
    private final Expression thisRef;
    private final Expression renderContext;

    TemplateVariables(VariableSet variableSet, Expression thisRef, Expression renderContext) {
      this.variableSet = variableSet;
      this.thisRef = thisRef;
      this.renderContext = renderContext;
    }

    @Override public Expression getParam(TemplateParam param) {
      return paramFields.get(param.name()).accessor(thisRef);
    }

    @Override public Expression getLocal(LocalVar local) {
      return variableSet.getVariable(local.name()).local();
    }

    @Override public Expression getLocal(SyntheticVarName varName) {
      return variableSet.getVariable(varName).local();
    }

    @Override public Expression getRenderContext() {
      return renderContext;
    }

    @Override public Expression getParamsRecord() {
      return paramsField.accessor(thisRef);
    }

    @Override public Expression getIjRecord() {
      return ijField.accessor(thisRef);
    }
  }
}
