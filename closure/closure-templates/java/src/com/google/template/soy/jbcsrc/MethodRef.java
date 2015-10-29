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

import static com.google.common.base.Preconditions.checkState;
import static com.google.template.soy.jbcsrc.Expression.areAllCheap;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.google.template.soy.data.SanitizedContent.ContentKind;
import com.google.template.soy.data.SoyList;
import com.google.template.soy.data.SoyMap;
import com.google.template.soy.data.SoyRecord;
import com.google.template.soy.data.SoyValue;
import com.google.template.soy.data.SoyValueProvider;
import com.google.template.soy.data.UnsafeSanitizedContentOrdainer;
import com.google.template.soy.data.internal.DictImpl;
import com.google.template.soy.data.internal.ListImpl;
import com.google.template.soy.data.internal.ParamStore;
import com.google.template.soy.data.restricted.BooleanData;
import com.google.template.soy.data.restricted.FloatData;
import com.google.template.soy.data.restricted.IntegerData;
import com.google.template.soy.data.restricted.StringData;
import com.google.template.soy.jbcsrc.Expression.Feature;
import com.google.template.soy.jbcsrc.Expression.Features;
import com.google.template.soy.jbcsrc.api.AdvisingAppendable;
import com.google.template.soy.jbcsrc.api.AdvisingStringBuilder;
import com.google.template.soy.jbcsrc.api.RenderResult;
import com.google.template.soy.jbcsrc.runtime.Runtime;
import com.google.template.soy.jbcsrc.shared.CompiledTemplate;
import com.google.template.soy.jbcsrc.shared.RenderContext;
import com.google.template.soy.msgs.restricted.SoyMsg;
import com.google.template.soy.msgs.restricted.SoyMsgRawTextPart;
import com.google.template.soy.shared.internal.SharedRuntime;
import com.google.template.soy.shared.restricted.SoyJavaFunction;
import com.google.template.soy.shared.restricted.SoyJavaPrintDirective;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A reference to a method that can be called at runtime.
 */
@AutoValue abstract class MethodRef {
  static final MethodRef ADVISING_STRING_BUILDER_GET_AND_CLEAR =
      forMethod(AdvisingStringBuilder.class, "getAndClearBuffer").asNonNullable();
  
  static final MethodRef ARRAY_LIST_ADD = forMethod(ArrayList.class, "add", Object.class);
  
  static final MethodRef BOOLEAN_DATA_FOR_VALUE =
      forMethod(BooleanData.class, "forValue", boolean.class).asNonNullable();
  
  static final MethodRef BOOLEAN_VALUE = forMethod(Boolean.class, "booleanValue").asCheap();
  static final MethodRef BOOLEAN_TO_STRING =
      forMethod(Boolean.class, "toString", boolean.class).asCheap().asNonNullable();
  
  static final MethodRef COMPILED_TEMPLATE_RENDER =
      forMethod(CompiledTemplate.class, "render", AdvisingAppendable.class, RenderContext.class)
          .asNonNullable();
  
  static final MethodRef DICT_IMPL_FOR_PROVIDER_MAP =
      forMethod(DictImpl.class, "forProviderMap", Map.class).asNonNullable();
  
  static final MethodRef DOUBLE_TO_STRING =
      forMethod(Double.class, "toString", double.class).asNonNullable();
  
  static final MethodRef DOUBLE_VALUE = forMethod(Number.class, "doubleValue").asCheap();
  
  static final MethodRef EQUALS = forMethod(Object.class, "equals", Object.class);
  
  static final MethodRef FLOAT_DATA_FOR_VALUE =
      forMethod(FloatData.class, "forValue", double.class).asNonNullable();
  
  static final MethodRef IMMUTABLE_LIST_OF =
      forMethod(ImmutableList.class, "of").asCheap().asNonNullable();
  
  static final MethodRef IMMUTABLE_MAP_OF =
      forMethod(ImmutableMap.class, "of").asCheap().asNonNullable();
  
  static final MethodRef INTEGER_DATA_FOR_VALUE =
      forMethod(IntegerData.class, "forValue", long.class).asNonNullable();
  
  static final MethodRef INTEGER_TO_STRING =
      forMethod(Integer.class, "toString", int.class).asNonNullable();
  
  static final MethodRef INTS_CHECKED_CAST =
      forMethod(Ints.class, "checkedCast", long.class).asCheap();
  
  static final MethodRef LINKED_HASH_MAP_CLEAR = create(LinkedHashMap.class, "clear");
  
  static final MethodRef LINKED_HASH_MAP_PUT =
      create(LinkedHashMap.class, "put", Object.class, Object.class);
  
  static final MethodRef LIST_GET = forMethod(List.class, "get", int.class).asCheap();
  
  static final MethodRef LIST_IMPL_FOR_PROVIDER_LIST =
      create(ListImpl.class, "forProviderList", List.class);
  
  static final MethodRef LIST_SIZE = forMethod(List.class, "size").asCheap();
  
  static final MethodRef LONG_TO_STRING = create(Long.class, "toString", long.class);
  
  static final MethodRef LONG_VALUE = forMethod(Number.class, "longValue").asCheap();

  static final MethodRef OBJECT_TO_STRING = create(Object.class, "toString");

  static final MethodRef ORDAIN_AS_SAFE =
      create(UnsafeSanitizedContentOrdainer.class, "ordainAsSafe", String.class, ContentKind.class);

  static final MethodRef PARAM_STORE_SET_FIELD =
      create(ParamStore.class, "setField", String.class, SoyValueProvider.class);

  static final MethodRef PRINT_STREAM_PRINTLN = create(PrintStream.class, "println");

  static final MethodRef RENDER_CONTEXT_GET_DELTEMPLATE =
      create(RenderContext.class, "getDelTemplate", String.class, String.class, boolean.class,
          SoyRecord.class, SoyRecord.class);

  static final MethodRef RENDER_CONTEXT_GET_FUNCTION =
      create(RenderContext.class, "getFunction", String.class);

  static final MethodRef RENDER_CONTEXT_GET_PRINT_DIRECTIVE =
      create(RenderContext.class, "getPrintDirective", String.class);

  static final MethodRef RENDER_CONTEXT_GET_SOY_MSG =
      create(RenderContext.class, "getSoyMsg", long.class);

  static final MethodRef RENDER_CONTEXT_RENAME_CSS_SELECTOR =
      forMethod(RenderContext.class, "renameCssSelector", String.class).asNonNullable();

  static final MethodRef RENDER_CONTEXT_RENAME_XID =
      forMethod(RenderContext.class, "renameXid", String.class).asNonNullable();

  static final MethodRef RENDER_CONTEXT_USE_PRIMARY_MSG =
      create(RenderContext.class, "usePrimaryMsg", long.class, long.class);

  static final MethodRef RENDER_RESULT_DONE =
      forMethod(RenderResult.class, "done").asCheap().asNonNullable();

  static final MethodRef RENDER_RESULT_IS_DONE = forMethod(RenderResult.class, "isDone").asCheap();

  static final MethodRef RENDER_RESULT_LIMITED =
      forMethod(RenderResult.class, "limited").asCheap().asNonNullable();

  static final MethodRef RUNTIME_APPLY_ESCAPERS =
      create(Runtime.class, "applyEscapers", CompiledTemplate.class, List.class, ContentKind.class);

  static final MethodRef RUNTIME_APPLY_PRINT_DIRECTIVE = create(Runtime.class,
      "applyPrintDirective", SoyJavaPrintDirective.class, SoyValue.class, List.class);

  static final MethodRef RUNTIME_CALL_SOY_FUNCTION =
      create(Runtime.class, "callSoyFunction", SoyJavaFunction.class, List.class);

  static final MethodRef RUNTIME_COERCE_DOUBLE_TO_BOOLEAN =
      create(Runtime.class, "coerceToBoolean", double.class);

  static final MethodRef RUNTIME_COERCE_TO_STRING =
      forMethod(Runtime.class, "coerceToString", SoyValue.class).asNonNullable();

  static final MethodRef RUNTIME_DIVIDED_BY =
      forMethod(SharedRuntime.class, "dividedBy", SoyValue.class, SoyValue.class).asNonNullable();

  static final MethodRef RUNTIME_EQUAL =
      create(SharedRuntime.class, "equal", SoyValue.class, SoyValue.class);

  static final MethodRef RUNTIME_COMPARE_STRING =
      create(SharedRuntime.class, "compareString", String.class, SoyValue.class);

  static final MethodRef RUNTIME_GET_FIELD_PROVIDER =
      create(Runtime.class, "getFieldProvider", SoyRecord.class, String.class).asNonNullable();

  static final MethodRef RUNTIME_GET_LIST_ITEM =
      create(Runtime.class, "getSoyListItem", List.class, long.class);

  static final MethodRef RUNTIME_GET_MAP_ITEM =
      forMethod(Runtime.class, "getSoyMapItem", SoyMap.class, SoyValue.class);

  static final MethodRef RUNTIME_LESS_THAN =
      forMethod(SharedRuntime.class, "lessThan", SoyValue.class, SoyValue.class).asNonNullable();

  static final MethodRef RUNTIME_LESS_THAN_OR_EQUAL =
      forMethod(SharedRuntime.class, "lessThanOrEqual", SoyValue.class, SoyValue.class)
          .asNonNullable();

  static final MethodRef RUNTIME_LOGGER =
      forMethod(Runtime.class, "logger").asCheap().asNonNullable();

  static final MethodRef RUNTIME_MINUS =
      forMethod(SharedRuntime.class, "minus", SoyValue.class, SoyValue.class).asNonNullable();

  static final MethodRef RUNTIME_NEGATIVE =
      forMethod(SharedRuntime.class, "negative", SoyValue.class).asNonNullable();

  static final MethodRef RUNTIME_PLUS =
      forMethod(SharedRuntime.class, "plus", SoyValue.class, SoyValue.class).asNonNullable();

  static final MethodRef RUNTIME_RENDER_SOY_MSG_WITH_PLACEHOLDERS = create(
      Runtime.class, "renderSoyMsgWithPlaceholders", SoyMsg.class, Map.class, Appendable.class);

  static final MethodRef RUNTIME_STRING_EQUALS_AS_NUMBER =
      forMethod(Runtime.class, "stringEqualsAsNumber", String.class, double.class).asNonNullable();

  static final MethodRef RUNTIME_TIMES =
      forMethod(SharedRuntime.class, "times", SoyValue.class, SoyValue.class).asNonNullable();

  static final MethodRef RUNTIME_UNEXPECTED_STATE_ERROR =
      forMethod(Runtime.class, "unexpectedStateError", int.class).asNonNullable();

  static final MethodRef SOY_LIST_AS_JAVA_LIST =
      forMethod(SoyList.class, "asJavaList").asNonNullable();
  static final MethodRef SOY_MSG_GET_PARTS =
      forMethod(SoyMsg.class, "getParts").asCheap().asNonNullable();

  static final MethodRef SOY_MSG_RAW_TEXT_PART_GET_RAW_TEXT =
      forMethod(SoyMsgRawTextPart.class, "getRawText").asCheap().asNonNullable();

  static final MethodRef SOY_VALUE_COERCE_TO_BOOLEAN =
      forMethod(SoyValue.class, "coerceToBoolean").asCheap();

  static final MethodRef SOY_VALUE_BOOLEAN_VALUE =
      forMethod(SoyValue.class, "booleanValue").asCheap();

  static final MethodRef SOY_VALUE_FLOAT_VALUE = forMethod(SoyValue.class, "floatValue").asCheap();

  static final MethodRef SOY_VALUE_LONG_VALUE = forMethod(SoyValue.class, "longValue").asCheap();

  static final MethodRef SOY_VALUE_NUMBER_VALUE =
      forMethod(SoyValue.class, "numberValue").asNonNullable();

  static final MethodRef SOY_VALUE_PROVIDER_RENDER_AND_RESOLVE =
      forMethod(SoyValueProvider.class, "renderAndResolve", AdvisingAppendable.class, boolean.class)
          .asNonNullable();

  static final MethodRef SOY_VALUE_PROVIDER_RESOLVE =
      create(Runtime.class, "resolveSoyValueProvider", SoyValueProvider.class);
  static final MethodRef SOY_VALUE_PROVIDER_STATUS =
      forMethod(SoyValueProvider.class, "status").asNonNullable();

  static final MethodRef SOY_VALUE_STRING_VALUE =
      forMethod(SoyValue.class, "stringValue").asNonNullable().asCheap();

  static final MethodRef STRING_CONCAT =
      forMethod(String.class, "concat", String.class).asNonNullable();

  static final MethodRef STRING_DATA_FOR_VALUE =
      forMethod(StringData.class, "forValue", String.class).asNonNullable().asCheap();

  static final MethodRef STRING_IS_EMPTY = create(String.class, "isEmpty");
  static final MethodRef STRING_VALUE_OF =
      forMethod(String.class, "valueOf", Object.class).asNonNullable();

  static MethodRef forMethod(Class<?> clazz, String methodName, Class<?> ...params) {
    return create(clazz, methodName, params);
  }

  static MethodRef create(Class<?> clazz, String methodName, Class<?> ...params) {
    java.lang.reflect.Method m;
    try {
      // Ensure that the method exists and is public.
      m = clazz.getMethod(methodName, params);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return create(m);
  }

  static MethodRef create(java.lang.reflect.Method method) {
    Class<?> clazz = method.getDeclaringClass();
    TypeInfo ownerType = TypeInfo.create(method.getDeclaringClass());
    boolean isStatic = Modifier.isStatic(method.getModifiers());
    ImmutableList<Type> argTypes;
    if (isStatic) {
      argTypes = ImmutableList.copyOf(Type.getArgumentTypes(method));
    } else {
      // for instance methods the first 'argument' is always an instance of the class.
      argTypes = ImmutableList.<Type>builder()
          .add(ownerType.type())
          .add(Type.getArgumentTypes(method))
          .build();
    }
    return new AutoValue_MethodRef(
        clazz.isInterface() 
            ? Opcodes.INVOKEINTERFACE 
            : isStatic 
                ? Opcodes.INVOKESTATIC 
                : Opcodes.INVOKEVIRTUAL, 
        ownerType,
        org.objectweb.asm.commons.Method.getMethod(method),
        Type.getType(method.getReturnType()),
        argTypes,
        Features.of());
  }

  static MethodRef createInstanceMethod(TypeInfo owner, Method method) {
    return new AutoValue_MethodRef(
        Opcodes.INVOKEVIRTUAL, 
        owner,
        method,
        method.getReturnType(),
        ImmutableList.<Type>builder()
            .add(owner.type())
            .add(method.getArgumentTypes())
            .build(),
        Features.of());
  }

  /**
   * The opcode to use to invoke the method.  Will be one of {@link Opcodes#INVOKEINTERFACE},
   * {@link Opcodes#INVOKESTATIC} or {@link Opcodes#INVOKEVIRTUAL}.
   */
  abstract int opcode();
  
  /** The 'internal name' of the type that owns the method. */
  abstract TypeInfo owner();
  
  abstract org.objectweb.asm.commons.Method method();
  abstract Type returnType();
  abstract ImmutableList<Type> argTypes();
  abstract Features features();
  
  // TODO(lukes): consider different names.  'invocation'? invoke() makes it sounds like we are 
  // actually calling the method rather than generating an expression that will output code that
  // will invoke the method.
  Statement invokeVoid(final Expression ...args) {
    return invokeVoid(Arrays.asList(args));
  }

  Statement invokeVoid(final Iterable<? extends Expression> args) {
    checkState(Type.VOID_TYPE.equals(returnType()), "Method return type is not void.");
    Expression.checkTypes(argTypes(), args);
    return new Statement() {
      @Override void doGen(CodeBuilder adapter) {
        doInvoke(adapter, args);
      }
    };
  }
  
  Expression invoke(final Expression ...args) {
    return invoke(Arrays.asList(args));
  }

  Expression invoke(final Iterable<? extends Expression> args) {
    // void methods violate the expression contract of pushing a result onto the runtime stack.
    checkState(!Type.VOID_TYPE.equals(returnType()), 
        "Cannot produce an expression from a void method.");
    Expression.checkTypes(argTypes(), args);
    Features features = features();
    if (!areAllCheap(args)) {
      features = features.minus(Feature.CHEAP);
    }
    return new Expression(returnType(), features) {
      @Override void doGen(CodeBuilder mv) {
        doInvoke(mv, args);
      }
    };
  }

  MethodRef asCheap() {
    return withFeature(Feature.CHEAP);
  }
  
  MethodRef asNonNullable() {
    return withFeature(Feature.NON_NULLABLE);
  }

  private MethodRef withFeature(Feature feature) {
    if (features().has(feature)) {
      return this;
    }
    return new AutoValue_MethodRef(
        opcode(), 
        owner(),
        method(),
        returnType(),
        argTypes(),
        features().plus(feature));
  }
  
  /**
   * Writes an invoke instruction for this method to the given adapter.  Useful when the expression
   * is not useful for representing operations.  For example, explicit dup operations are awkward
   * in the Expression api. 
   */
  void invokeUnchecked(CodeBuilder mv) {
    mv.visitMethodInsn(
        opcode(),
        owner().internalName(),
        method().getName(),
        method().getDescriptor(),
        // This is for whether the methods owner is an interface.  This is mostly to handle java8
        // default methods on interfaces.  We don't care about those currently, but ASM requires 
        // this.
        opcode() == Opcodes.INVOKEINTERFACE);
  }

  private void doInvoke(CodeBuilder mv, Iterable<? extends Expression> args) {
    for (Expression arg : args) {
      arg.gen(mv);
    }
    invokeUnchecked(mv);
  }
}
