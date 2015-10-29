/*
 * Copyright 2013 The Closure Compiler Authors.
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

package com.google.javascript.jscomp.newtypes;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.javascript.jscomp.CodingConvention;
import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author blickly@google.com (Ben Lickly)
 * @author dimvar@google.com (Dimitris Vardoulakis)
 */
public final class JSTypeCreatorFromJSDoc {
  public static final DiagnosticType INVALID_GENERICS_INSTANTIATION =
      DiagnosticType.warning(
        "JSC_NTI_INVALID_GENERICS_INSTANTIATION",
        "Invalid generics instantiation for {0}.\n"
        + "Expected {1} type argument(s), but found {2}.");

  public static final DiagnosticType BAD_JSDOC_ANNOTATION =
      DiagnosticType.warning(
        "JSC_NTI_BAD_JSDOC_ANNOTATION",
        "Bad JSDoc annotation. {0}");

  public static final DiagnosticType EXTENDS_NON_OBJECT =
      DiagnosticType.warning(
          "JSC_NTI_EXTENDS_NON_OBJECT",
          "{0} extends non-object type {1}.\n");

  public static final DiagnosticType EXTENDS_NOT_ON_CTOR_OR_INTERF =
      DiagnosticType.warning(
          "JSC_NTI_EXTENDS_NOT_ON_CTOR_OR_INTERF",
          "@extends used without @constructor or @interface for {0}.\n");

  public static final DiagnosticType INHERITANCE_CYCLE =
      DiagnosticType.warning(
          "JSC_NTI_INHERITANCE_CYCLE",
          "Cycle detected in inheritance chain of type {0}");

  public static final DiagnosticType DICT_IMPLEMENTS_INTERF =
      DiagnosticType.warning(
          "JSC_NTI_DICT_IMPLEMENTS_INTERF",
          "Class {0} is a dict. Dicts can't implement interfaces.");

  public static final DiagnosticType IMPLEMENTS_WITHOUT_CONSTRUCTOR =
      DiagnosticType.warning(
          "JSC_NTI_IMPLEMENTS_WITHOUT_CONSTRUCTOR",
          "@implements used without @constructor or @interface for {0}");

  public static final DiagnosticType CONFLICTING_SHAPE_TYPE =
      DiagnosticType.disabled(
          "JSC_NTI_CONFLICTING_SHAPE_TYPE",
          "{1} cannot extend this type; {0}s can only extend {0}s");

  public static final DiagnosticType CONFLICTING_EXTENDED_TYPE =
      DiagnosticType.warning(
          "JSC_NTI_CONFLICTING_EXTENDED_TYPE",
          "{1} cannot extend this type; {0}s can only extend {0}s");

  public static final DiagnosticType CONFLICTING_IMPLEMENTED_TYPE =
    DiagnosticType.warning(
        "JSC_NTI_CONFLICTING_IMPLEMENTED_TYPE",
        "{0} cannot implement this type; "
        + "an interface can only extend, but not implement interfaces");

  public static final DiagnosticType UNION_IS_UNINHABITABLE =
    DiagnosticType.warning(
        "JSC_NTI_UNION_IS_UNINHABITABLE",
        "Union of {0} with {1} would create an impossible type.");

  private final CodingConvention convention;

  // Used to communicate state between methods when resolving enum types
  private int howmanyTypeVars = 0;

  /** Exception for when unrecognized type names are encountered */
  public static class UnknownTypeException extends Exception {
    UnknownTypeException(String cause) {
      super(cause);
    }
  }

  private Set<JSError> warnings = new LinkedHashSet<>();
  // Unknown type names indexed by JSDoc AST node at which they were found.
  private Map<Node, String> unknownTypeNames = new LinkedHashMap<>();

  public JSTypeCreatorFromJSDoc(CodingConvention convention) {
    this.qmarkFunctionDeclared = new FunctionAndSlotType(
        null, FunctionTypeBuilder.qmarkFunctionBuilder().buildDeclaration());
    this.convention = convention;
  }

  private FunctionAndSlotType qmarkFunctionDeclared;
  private static final boolean NULLABLE_TYPES_BY_DEFAULT = true;

  public JSType maybeMakeNullable(JSType t) {
    if (NULLABLE_TYPES_BY_DEFAULT) {
      return JSType.join(JSType.NULL, t);
    }
    return t;
  }

  public JSType getDeclaredTypeOfNode(JSDocInfo jsdoc, RawNominalType ownerType,
      DeclaredTypeRegistry registry) {
    return getDeclaredTypeOfNode(jsdoc, registry, ownerType == null
        ? ImmutableList.<String>of() : ownerType.getTypeParameters());
  }

  private JSType getDeclaredTypeOfNode(JSDocInfo jsdoc,
      DeclaredTypeRegistry registry, ImmutableList<String> typeParameters) {
    if (jsdoc == null) {
      return null;
    }
    return getTypeFromJSTypeExpression(
        jsdoc.getType(), registry, typeParameters);
  }

  public Set<JSError> getWarnings() {
    return warnings;
  }

  public Map<Node, String> getUnknownTypesMap() {
    return unknownTypeNames;
  }

  private JSType getTypeFromJSTypeExpression(JSTypeExpression expr,
      DeclaredTypeRegistry registry, ImmutableList<String> typeParameters) {
    if (expr == null) {
      return null;
    }
    return getTypeFromComment(expr.getRoot(), registry, typeParameters);
  }

  // Very similar to JSTypeRegistry#createFromTypeNodesInternal
  // n is a jsdoc node, not an AST node; the same class (Node) is used for both
  private JSType getTypeFromComment(Node n, DeclaredTypeRegistry registry,
      ImmutableList<String> typeParameters) {
    try {
      return getTypeFromCommentHelper(n, registry, typeParameters);
    } catch (UnknownTypeException e) {
      return JSType.UNKNOWN;
    }
  }

  private JSType getMaybeTypeFromComment(Node n, DeclaredTypeRegistry registry,
      ImmutableList<String> typeParameters) {
    try {
      return getTypeFromCommentHelper(n, registry, typeParameters);
    } catch (UnknownTypeException e) {
      return null;
    }
  }

  private JSType getTypeFromCommentHelper(Node n, DeclaredTypeRegistry registry,
      ImmutableList<String> typeParameters) throws UnknownTypeException {
    Preconditions.checkNotNull(n);
    if (typeParameters == null) {
      typeParameters = ImmutableList.of();
    }
    switch (n.getType()) {
      case Token.LC:
        return getRecordTypeHelper(n, registry, typeParameters);
      case Token.EMPTY: // for function types that don't declare a return type
        return JSType.UNKNOWN;
      case Token.VOID:
        return JSType.UNDEFINED;
      case Token.LB:
        warn("The [] type syntax is no longer supported." +
             " Please use Array.<T> instead.", n);
        return JSType.UNKNOWN;
      case Token.STRING:
        return getNamedTypeHelper(n, registry, typeParameters);
      case Token.PIPE: {
        // The way JSType.join works, Subtype|Supertype is equal to Supertype,
        // so when programmers write un-normalized unions, we normalize them
        // silently. We may also want to warn.
        JSType union = JSType.BOTTOM;
        for (Node child = n.getFirstChild(); child != null;
             child = child.getNext()) {
          // TODO(dimvar): When the union has many things, we join and throw
          // away types, except the result of the last join. Very inefficient.
          // Consider optimizing.
          JSType nextType =
              getTypeFromCommentHelper(child, registry, typeParameters);
          if (nextType.isUnknown()) {
            warn("This union type is equivalent to '?'.", n);
            return JSType.UNKNOWN;
          }
          JSType nextUnion = JSType.join(union, nextType);
          if (nextUnion.isBottom()) {
            warnings.add(JSError.make(n, UNION_IS_UNINHABITABLE,
                    nextType.toString(), union.toString()));
            return JSType.UNKNOWN;
          }
          union = nextUnion;
        }
        return union;
      }
      case Token.BANG: {
        JSType nullableType = getTypeFromCommentHelper(
            n.getFirstChild(), registry, typeParameters);
        if (nullableType.isTypeVariable()) {
          warn("Cannot use ! to restrict type variable type.\n"
              + "Prefer to make type argument non-nullable and add "
              + "null explicitly where needed (e.g. through ?T or T|null)", n);
        }
        return nullableType.removeType(JSType.NULL);
      }
      case Token.QMARK: {
        Node child = n.getFirstChild();
        if (child == null) {
          return JSType.UNKNOWN;
        } else {
          return JSType.join(JSType.NULL,
              getTypeFromCommentHelper(child, registry, typeParameters));
        }
      }
      case Token.STAR:
        return JSType.TOP;
      case Token.FUNCTION:
        return getFunTypeHelper(n, registry, typeParameters);
      default:
        throw new IllegalArgumentException("Unsupported type exp: " +
            Token.name(n.getType()) + " " + n.toStringTree());
    }
  }

  private JSType getRecordTypeHelper(Node n, DeclaredTypeRegistry registry,
      ImmutableList<String> typeParameters)
      throws UnknownTypeException {
    Map<String, Property> props = new LinkedHashMap<>();
    for (Node propNode = n.getFirstChild().getFirstChild();
         propNode != null;
         propNode = propNode.getNext()) {
      boolean isPropDeclared = propNode.getType() == Token.COLON;
      Node propNameNode = isPropDeclared ? propNode.getFirstChild() : propNode;
      String propName = propNameNode.getString();
      if (propName.startsWith("'") || propName.startsWith("\"")) {
        propName = propName.substring(1, propName.length() - 1);
      }
      JSType propType = !isPropDeclared
          ? JSType.UNKNOWN
          : getTypeFromCommentHelper(propNode.getLastChild(), registry, typeParameters);
      Property prop;
      if (!propType.isUnknown() && !propType.isTop()
          && JSType.UNDEFINED.isSubtypeOf(propType)) {
        prop = Property.makeOptional(null, propType, propType);
      } else {
        prop = Property.make(propType, propType);
      }
      props.put(propName, prop);
    }
    return JSType.fromObjectType(ObjectType.fromProperties(props));
  }

  private JSType getNamedTypeHelper(Node n, DeclaredTypeRegistry registry,
      ImmutableList<String> outerTypeParameters)
      throws UnknownTypeException {
    String typeName = n.getString();
    switch (typeName) {
      case "boolean":
        return JSType.BOOLEAN;
      case "null":
        return JSType.NULL;
      case "number":
        return JSType.NUMBER;
      case "string":
        return JSType.STRING;
      case "undefined":
      case "void":
        return JSType.UNDEFINED;
      case "Function":
        return maybeMakeNullable(registry.getCommonTypes().qmarkFunction());
      case "Object":
        // We don't generally handle parameterized Object<...>, but we want to
        // at least not warn about inexistent properties on it, so we type it
        // as @dict.
        return maybeMakeNullable(n.hasChildren() ? JSType.TOP_DICT : JSType.TOP_OBJECT);
      default:
        return lookupTypeByName(typeName, n, registry, outerTypeParameters);
    }
  }

  private JSType lookupTypeByName(String name,
      Node n, DeclaredTypeRegistry registry, ImmutableList<String> outerTypeParameters)
      throws UnknownTypeException {
    if (outerTypeParameters.contains(name)) {
      return JSType.fromTypeVar(name);
    }
    Declaration decl = registry.getDeclaration(QualifiedName.fromQualifiedString(name), true);
    if (decl == null) {
      unknownTypeNames.put(n, name);
      throw new UnknownTypeException("Unhandled type: " + name);
    }
    // It's either a typedef, an enum, a type variable or a nominal type
    if (decl.getTypedef() != null) {
      return getTypedefType(decl.getTypedef(), registry);
    }
    if (decl.getEnum() != null) {
      return getEnumPropType(decl.getEnum(), registry);
    }
    if (decl.isTypeVar()) {
      howmanyTypeVars++;
      return decl.getTypeOfSimpleDecl();
    }
    if (decl.getNominal() != null) {
      return getNominalTypeHelper(decl.getNominal(), n, registry, outerTypeParameters);
    }
    return JSType.UNKNOWN;
  }

  private JSType getTypedefType(Typedef td, DeclaredTypeRegistry registry) {
    resolveTypedef(td, registry);
    return td.getType();
  }

  public void resolveTypedef(Typedef td, DeclaredTypeRegistry registry) {
    Preconditions.checkState(td != null, "getTypedef should only be " +
        "called when we know that the typedef is defined");
    if (td.isResolved()) {
      return;
    }
    JSTypeExpression texp = td.getTypeExpr();
    JSType tdType;
    if (texp == null) {
      warn("Circular type definitions are not allowed.",
          td.getTypeExprForErrorReporting().getRoot());
      tdType = JSType.UNKNOWN;
    } else {
      tdType = getTypeFromJSTypeExpression(texp, registry, null);
    }
    td.resolveTypedef(tdType);
  }

  private JSType getEnumPropType(EnumType e, DeclaredTypeRegistry registry) {
    resolveEnum(e, registry);
    return e.getPropType();
  }

  public void resolveEnum(EnumType e, DeclaredTypeRegistry registry) {
    Preconditions.checkState(e != null, "getEnum should only be " +
        "called when we know that the enum is defined");
    if (e.isResolved()) {
      return;
    }
    JSTypeExpression texp = e.getTypeExpr();
    JSType enumeratedType;
    if (texp == null) {
      warn("Circular type definitions are not allowed.",
          e.getTypeExprForErrorReporting().getRoot());
      enumeratedType = JSType.UNKNOWN;
    } else {
      int numTypeVars = howmanyTypeVars;
      enumeratedType = getTypeFromJSTypeExpression(texp, registry, null);
      if (howmanyTypeVars > numTypeVars) {
        warn("An enum type cannot include type variables.", texp.getRoot());
        enumeratedType = JSType.UNKNOWN;
        howmanyTypeVars = numTypeVars;
      } else if (enumeratedType.isTop()) {
        warn("An enum type cannot be *. " +
            "Use ? if you do not want the elements checked.",
            texp.getRoot());
        enumeratedType = JSType.UNKNOWN;
      } else if (enumeratedType.isUnion()) {
        warn("An enum type cannot be a union type.", texp.getRoot());
        enumeratedType = JSType.UNKNOWN;
      }
    }
    e.resolveEnum(enumeratedType);
  }

  private JSType getNominalTypeHelper(RawNominalType rawType, Node n,
      DeclaredTypeRegistry registry, ImmutableList<String> outerTypeParameters)
      throws UnknownTypeException {
    NominalType uninstantiated = rawType.getAsNominalType();
    if (!rawType.isGeneric() && !n.hasChildren()) {
      return rawType.getInstanceWithNullability(NULLABLE_TYPES_BY_DEFAULT);
    }
    ImmutableList.Builder<JSType> typeList = ImmutableList.builder();
    if (n.hasChildren()) {
      // Compute instantiation of polymorphic class/interface.
      Preconditions.checkState(n.getFirstChild().isBlock());
      for (Node child : n.getFirstChild().children()) {
        typeList.add(
            getTypeFromCommentHelper(child, registry, outerTypeParameters));
      }
    }
    ImmutableList<JSType> typeArguments = typeList.build();
    ImmutableList<String> typeParameters = rawType.getTypeParameters();
    int typeArgsSize = typeArguments.size();
    int typeParamsSize = typeParameters.size();
    if (typeArgsSize != typeParamsSize) {
      // We used to also warn when (typeArgsSize < typeParamsSize), but it
      // happens so often that we stopped. Array, Object and goog.Promise are
      // common culprits, but many other types as well.
      if (typeArgsSize > typeParamsSize) {
        warnings.add(JSError.make(
            n, INVALID_GENERICS_INSTANTIATION,
            uninstantiated.getName(), String.valueOf(typeParamsSize),
            String.valueOf(typeArgsSize)));
      }
      return maybeMakeNullable(JSType.fromObjectType(ObjectType.fromNominalType(
              uninstantiated.instantiateGenerics(
                  fixLengthOfTypeList(typeParameters.size(), typeArguments)))));
    }
    return maybeMakeNullable(JSType.fromObjectType(ObjectType.fromNominalType(
            uninstantiated.instantiateGenerics(typeArguments))));
  }

  private static List<JSType> fixLengthOfTypeList(
      int desiredLength, List<JSType> typeList) {
    int length = typeList.size();
    if (length == desiredLength) {
      return typeList;
    }
    ImmutableList.Builder<JSType> builder = ImmutableList.builder();
    for (int i = 0; i < desiredLength; i++) {
      builder.add(i < length ? typeList.get(i) : JSType.UNKNOWN);
    }
    return builder.build();
  }

  // Don't confuse with getFunTypeFromAtTypeJsdoc; the function below computes a
  // type that doesn't have an associated AST node.
  private JSType getFunTypeHelper(Node jsdocNode, DeclaredTypeRegistry registry,
      ImmutableList<String> typeParameters) throws UnknownTypeException {
    FunctionTypeBuilder builder = new FunctionTypeBuilder();
    fillInFunTypeBuilder(jsdocNode, null, registry, typeParameters, builder);
    return registry.getCommonTypes().fromFunctionType(builder.buildFunction());
  }

  private void fillInFunTypeBuilder(
      Node jsdocNode, RawNominalType ownerType, DeclaredTypeRegistry registry,
      ImmutableList<String> typeParameters, FunctionTypeBuilder builder)
      throws UnknownTypeException {
    Node child = jsdocNode.getFirstChild();
    NominalType builtinObject = registry.getCommonTypes().getObjectType();
    if (child.getType() == Token.THIS) {
      if (ownerType == null) {
        NominalType nt = getNominalType(child.getFirstChild(), registry, typeParameters);
        builder.addReceiverType(nt == null ? builtinObject : nt);
      }
      child = child.getNext();
    } else if (child.getType() == Token.NEW) {
      NominalType nt = getNominalType(child.getFirstChild(), registry, typeParameters);
      builder.addNominalType(nt == null ? builtinObject : nt);
      child = child.getNext();
    }
    if (child.getType() == Token.PARAM_LIST) {
      for (Node arg = child.getFirstChild(); arg != null; arg = arg.getNext()) {
        try {
          switch (arg.getType()) {
            case Token.EQUALS:
              builder.addOptFormal(getTypeFromCommentHelper(
                  arg.getFirstChild(), registry, typeParameters));
              break;
            case Token.ELLIPSIS:
              Node restNode = arg.getFirstChild();
              builder.addRestFormals(restNode == null ? JSType.UNKNOWN :
                  getTypeFromCommentHelper(restNode, registry, typeParameters));
              break;
            default:
              builder.addReqFormal(
                  getTypeFromCommentHelper(arg, registry, typeParameters));
              break;
          }
        } catch (FunctionTypeBuilder.WrongParameterOrderException e) {
          warn("Wrong parameter order: required parameters are first, " +
              "then optional, then varargs", jsdocNode);
          builder.addPlaceholderFormal();
        }
      }
      child = child.getNext();
    }
    builder.addRetType(
        getTypeFromCommentHelper(child, registry, typeParameters));
  }

  // May return null;
  private NominalType getNominalType(Node n,
      DeclaredTypeRegistry registry, ImmutableList<String> typeParameters) {
    return getTypeFromComment(n, registry, typeParameters)
        .removeType(JSType.NULL).getNominalTypeIfSingletonObj();
  }

  private ImmutableSet<NominalType> getImplementedInterfaces(
      JSDocInfo jsdoc, DeclaredTypeRegistry registry,
      ImmutableList<String> typeParameters) {
    return getInterfacesHelper(jsdoc, registry, typeParameters, true);
  }

  private ImmutableSet<NominalType> getExtendedInterfaces(
      JSDocInfo jsdoc, DeclaredTypeRegistry registry,
      ImmutableList<String> typeParameters) {
    return getInterfacesHelper(jsdoc, registry, typeParameters, false);
  }

  private ImmutableSet<NominalType> getInterfacesHelper(
      JSDocInfo jsdoc, DeclaredTypeRegistry registry,
      ImmutableList<String> typeParameters, boolean implementedIntfs) {
    ImmutableSet.Builder<NominalType> builder = ImmutableSet.builder();
    for (JSTypeExpression texp : (implementedIntfs ?
          jsdoc.getImplementedInterfaces() :
          jsdoc.getExtendedInterfaces())) {
      Node expRoot = texp.getRoot();
      JSType interfaceType =
          getMaybeTypeFromComment(expRoot, registry, typeParameters);
      if (interfaceType != null) {
        NominalType nt = interfaceType.getNominalTypeIfSingletonObj();
        if (nt != null && nt.isInterface()) {
          builder.add(nt);
        } else {
          String errorMsg = implementedIntfs ?
              "Cannot implement non-interface" :
              "Cannot extend non-interface";
          warn(errorMsg, expRoot);
        }
      }
    }
    return builder.build();
  }

  public static class FunctionAndSlotType {
    public JSType slotType;
    public DeclaredFunctionType functionType;

    public FunctionAndSlotType(JSType slotType, DeclaredFunctionType functionType) {
      this.slotType = slotType;
      this.functionType = functionType;
    }
  }

  /**
   * Consumes either a "classic" function jsdoc with @param, @return, etc,
   * or a jsdoc with @type {function ...} and finds the types of the formal
   * parameters and the return value. It returns a builder because the callers
   * of this function must separately handle @constructor, @interface, etc.
   *
   * constructorType is non-null iff this function is a constructor or
   * interface declaration.
   */
  public FunctionAndSlotType getFunctionType(
      JSDocInfo jsdoc, String functionName, Node declNode,
      RawNominalType constructorType, RawNominalType ownerType,
      DeclaredTypeRegistry registry) {
    FunctionTypeBuilder builder = new FunctionTypeBuilder();
    if (ownerType != null) {
      builder.addReceiverType(ownerType.getAsNominalType());
    }
    try {
      if (jsdoc != null && jsdoc.getType() != null) {
        JSType simpleType = getDeclaredTypeOfNode(jsdoc, ownerType, registry);
        if (simpleType.isUnknown() || simpleType.isTop()) {
          return qmarkFunctionDeclared;
        }
        FunctionType funType = simpleType.getFunType();
        if (funType != null) {
          JSType slotType = simpleType.isFunctionType() ? null : simpleType;
          DeclaredFunctionType declType = funType.toDeclaredFunctionType();
          if (ownerType != null) {
            declType = declType.withReceiverType(ownerType.getAsNominalType());
          }
          return new FunctionAndSlotType(slotType, declType);
        } else {
          warn("The function is annotated with a non-function jsdoc. " +
              "Ignoring jsdoc.", declNode);
          jsdoc = null;
        }
      }
      DeclaredFunctionType declType = getFunTypeFromTypicalFunctionJsdoc(
          jsdoc, functionName, declNode,
          constructorType, ownerType, registry, builder, false);
      return new FunctionAndSlotType(null, declType);
    } catch (FunctionTypeBuilder.WrongParameterOrderException e) {
      warn("Wrong parameter order: required parameters are first, " +
          "then optional, then varargs. Ignoring jsdoc.", declNode);
      return qmarkFunctionDeclared;
    }
  }

  private static class ParamIterator {
    /** The parameter names from the JSDocInfo. Only set if 'params' is null. */
    Iterator<String> paramNames;

    /**
     * The PARAM_LIST node containing the function parameters. Only set if
     * 'paramNames' is null.
     */
    Node params;

    int index = -1;

    ParamIterator(Node params, JSDocInfo jsdoc) {
      Preconditions.checkArgument(params != null || jsdoc != null);
      if (params != null) {
        this.params = params;
        this.paramNames = null;
      } else {
        this.params = null;
        this.paramNames = jsdoc.getParameterNames().iterator();
      }
    }

    boolean hasNext() {
      if (paramNames != null) {
        return paramNames.hasNext();
      }
      return index + 1 < params.getChildCount();
    }

    String nextString() {
      if (paramNames != null) {
        return paramNames.next();
      }
      index++;
      return params.getChildAtIndex(index).getString();
    }

    Node getNode() {
      if (paramNames != null) {
        return null;
      }
      return params.getChildAtIndex(index);
    }
  }

  private DeclaredFunctionType getFunTypeFromTypicalFunctionJsdoc(
      JSDocInfo jsdoc, String functionName, Node funNode,
      RawNominalType constructorType, RawNominalType ownerType,
      DeclaredTypeRegistry registry, FunctionTypeBuilder builder,
      boolean ignoreJsdoc /* for when the jsdoc is malformed */) {
    Preconditions.checkArgument(!ignoreJsdoc || jsdoc == null);
    Preconditions.checkArgument(!ignoreJsdoc || funNode.isFunction());
    ImmutableList.Builder<String> typeParamsBuilder = new ImmutableList.Builder<>();;
    ImmutableList<String> typeParameters = ImmutableList.of();
    Node parent = funNode.getParent();

    // TODO(dimvar): need more @template warnings
    // - warn for multiple @template annotations
    // - warn for @template annotation w/out usage

    if (jsdoc != null) {
      typeParamsBuilder.addAll(jsdoc.getTemplateTypeNames());
      // We don't properly support the type transformation language; we treat
      // its type variables as ordinary type variables.
      typeParamsBuilder.addAll(jsdoc.getTypeTransformations().keySet());
      typeParameters = typeParamsBuilder.build();
      if (!typeParameters.isEmpty()) {
        if (parent.isSetterDef() || parent.isGetterDef()) {
          ignoreJsdoc = true;
          jsdoc = null;
          warn("@template can't be used with getters/setters", funNode);
        } else {
          builder.addTypeParameters(typeParameters);
        }
      }
    }
    if (ownerType != null) {
      typeParamsBuilder.addAll(ownerType.getTypeParameters());
      typeParameters = typeParamsBuilder.build();
    }

    fillInFormalParameterTypes(
        jsdoc, funNode, typeParameters, registry, builder, ignoreJsdoc);
    fillInReturnType(
        jsdoc, funNode, parent, typeParameters, registry, builder, ignoreJsdoc);
    if (jsdoc == null) {
      return builder.buildDeclaration();
    }

    // Look at other annotations, eg, @constructor
    NominalType parentClass = getMaybeParentClass(
        jsdoc, functionName, funNode, typeParameters, registry);
    ImmutableSet<NominalType> implementedIntfs = getImplementedInterfaces(
        jsdoc, registry, typeParameters);
    if (constructorType == null && jsdoc.isConstructorOrInterface()) {
      // Anonymous type, don't register it.
      return builder.buildDeclaration();
    } else if (jsdoc.isConstructor()) {
      handleConstructorAnnotation(functionName, funNode, constructorType,
          parentClass, implementedIntfs, registry, builder);
    } else if (jsdoc.isInterface()) {
      handleInterfaceAnnotation(jsdoc, functionName, funNode, constructorType,
          implementedIntfs, typeParameters, registry, builder);
    } else if (!implementedIntfs.isEmpty()) {
      warnings.add(JSError.make(
          funNode, IMPLEMENTS_WITHOUT_CONSTRUCTOR, functionName));
    }

    if (jsdoc.hasThisType() && ownerType == null) {
      Node thisRoot = jsdoc.getThisType().getRoot();
      Preconditions.checkState(thisRoot.getType() == Token.BANG);
      Node thisNode = thisRoot.getFirstChild();
      // JsDocInfoParser wraps @this types with !. But we warn when we see !T,
      // and we don't want to warn for a ! that was automatically inserted.
      // So, we bypass the ! here.
      JSType thisType = getMaybeTypeFromComment(thisNode, registry, typeParameters);
      if (thisType != null) {
        thisType = thisType.removeType(JSType.NULL);
      }
      // TODO(dimvar): thisType may be non-null but have a null
      // thisTypeAsNominal.
      // We currently only support nominal types for the receiver type, but
      // people use other types as well: unions, records, etc.
      // For now, we just use the generic Object for these.
      NominalType nt = thisType == null ? null : thisType.getNominalTypeIfSingletonObj();
      NominalType builtinObject = registry.getCommonTypes().getObjectType();
      builder.addReceiverType(nt == null ? builtinObject : nt);
    }

    return builder.buildDeclaration();
  }

  private void fillInFormalParameterTypes(
      JSDocInfo jsdoc, Node funNode,
      ImmutableList<String> typeParameters,
      DeclaredTypeRegistry registry, FunctionTypeBuilder builder,
      boolean ignoreJsdoc /* for when the jsdoc is malformed */) {
    boolean ignoreFunNode  = !funNode.isFunction();
    Node params = ignoreFunNode ? null : funNode.getFirstChild().getNext();
    ParamIterator iterator = new ParamIterator(params, jsdoc);
    while (iterator.hasNext()) {
      String pname = iterator.nextString();
      Node param = iterator.getNode();
      ParameterKind p = ParameterKind.REQUIRED;
      if (param != null && convention.isOptionalParameter(param)) {
        p = ParameterKind.OPTIONAL;
      } else if (param != null && convention.isVarArgsParameter(param)) {
        p = ParameterKind.REST;
      }
      ParameterType inlineParamType = (ignoreJsdoc || ignoreFunNode || param.getJSDocInfo() == null)
          ? null : parseParameter(param.getJSDocInfo().getType(), p, registry, typeParameters);
      ParameterType fnParamType = inlineParamType;
      JSTypeExpression jsdocExp = jsdoc == null ? null : jsdoc.getParameterType(pname);
      if (jsdocExp != null) {
        if (inlineParamType == null) {
          fnParamType = parseParameter(jsdocExp, p, registry, typeParameters);
        } else {
          warn("Found two JsDoc comments for formal parameter " + pname, param);
        }
      }
      JSType t  = null;
      if (fnParamType != null) {
        p = fnParamType.kind;
        t = fnParamType.type;
      }
      switch (p) {
          case REQUIRED:
            builder.addReqFormal(t);
            break;
          case OPTIONAL:
            builder.addOptFormal(t);
            break;
          case REST:
            builder.addRestFormals(t != null ? t : JSType.UNKNOWN);
            break;
      }
    }
  }

  private void fillInReturnType(
      JSDocInfo jsdoc, Node funNode, Node parent,
      ImmutableList<String> typeParameters,
      DeclaredTypeRegistry registry, FunctionTypeBuilder builder,
      boolean ignoreJsdoc /* for when the jsdoc is malformed */) {
    JSDocInfo inlineRetJsdoc =
        ignoreJsdoc ? null : funNode.getFirstChild().getJSDocInfo();
    JSTypeExpression retTypeExp = jsdoc == null ? null : jsdoc.getReturnType();
    if (parent.isSetterDef() && retTypeExp == null) {
      // inline returns for getters/setters are not parsed
      builder.addRetType(JSType.UNDEFINED);
    } else if (inlineRetJsdoc != null) {
      builder.addRetType(
          getDeclaredTypeOfNode(inlineRetJsdoc, registry, typeParameters));
      if (retTypeExp != null) {
        warn("Found two JsDoc comments for the return type", funNode);
      }
    } else {
      builder.addRetType(
          getTypeFromJSTypeExpression(retTypeExp, registry, typeParameters));
    }
  }

  private NominalType getMaybeParentClass(
      JSDocInfo jsdoc, String functionName, Node funNode,
      ImmutableList<String> typeParameters, DeclaredTypeRegistry registry) {
    if (!jsdoc.hasBaseType()) {
      return null;
    }
    if (!jsdoc.isConstructor()) {
      warnings.add(JSError.make(
          funNode, EXTENDS_NOT_ON_CTOR_OR_INTERF, functionName));
      return null;
    }
    Node docNode = jsdoc.getBaseType().getRoot();
    JSType extendedType =
        getMaybeTypeFromComment(docNode, registry, typeParameters);
    if (extendedType == null) {
      return null;
    }
    NominalType parentClass = extendedType.getNominalTypeIfSingletonObj();
    if (parentClass != null && parentClass.isClass()) {
      return parentClass;
    }
    if (parentClass == null) {
      warnings.add(JSError.make(funNode, EXTENDS_NON_OBJECT,
              functionName, extendedType.toString()));
    } else {
      Preconditions.checkState(parentClass.isInterface());
      warnings.add(JSError.make(funNode, CONFLICTING_EXTENDED_TYPE,
              "constructor", functionName));
    }
    return null;
  }

  private void handleConstructorAnnotation(
      String functionName, Node funNode, RawNominalType constructorType,
      NominalType parentClass, ImmutableSet<NominalType> implementedIntfs,
      DeclaredTypeRegistry registry, FunctionTypeBuilder builder) {
    String className = constructorType.toString();
    NominalType builtinObject = registry.getCommonTypes().getObjectType();
    if (parentClass == null && !functionName.equals("Object")) {
      parentClass = builtinObject;
    }
    if (parentClass != null) {
      if (!constructorType.addSuperClass(parentClass)) {
        warnings.add(JSError.make(funNode, INHERITANCE_CYCLE, className));
      } else if (parentClass != builtinObject) {
        if (constructorType.isStruct() && !parentClass.isStruct()) {
          warnings.add(JSError.make(
              funNode, CONFLICTING_SHAPE_TYPE, "struct", className));
        } else if (constructorType.isDict() && !parentClass.isDict()) {
          warnings.add(JSError.make(
              funNode, CONFLICTING_SHAPE_TYPE, "dict", className));
        }
      }
    }
    if (constructorType.isDict() && !implementedIntfs.isEmpty()) {
      warnings.add(JSError.make(funNode, DICT_IMPLEMENTS_INTERF, className));
    }
    boolean noCycles = constructorType.addInterfaces(implementedIntfs);
    Preconditions.checkState(noCycles);
    builder.addNominalType(constructorType.getAsNominalType());
  }

  private void handleInterfaceAnnotation(
      JSDocInfo jsdoc, String functionName, Node funNode,
      RawNominalType constructorType,
      ImmutableSet<NominalType> implementedIntfs,
      ImmutableList<String> typeParameters,
      DeclaredTypeRegistry registry, FunctionTypeBuilder builder) {
    if (!implementedIntfs.isEmpty()) {
      warnings.add(JSError.make(
          funNode, CONFLICTING_IMPLEMENTED_TYPE, functionName));
    }
    boolean noCycles = constructorType.addInterfaces(
        getExtendedInterfaces(jsdoc, registry, typeParameters));
    if (!noCycles) {
      warnings.add(JSError.make(
          funNode, INHERITANCE_CYCLE, constructorType.toString()));
    }
    builder.addNominalType(constructorType.getAsNominalType());
  }

  // /** @param {...?} var_args */ function f(var_args) { ... }
  // var_args shouldn't be used in the body of f
  public static boolean isRestArg(JSDocInfo funJsdoc, String formalParamName) {
    if (funJsdoc == null) {
      return false;
    }
    JSTypeExpression texp = funJsdoc.getParameterType(formalParamName);
    Node jsdocNode = texp == null ? null : texp.getRoot();
    return jsdocNode != null && jsdocNode.getType() == Token.ELLIPSIS;
  }

  // TODO(blickly): Add more DiagnosticTypes and remove this method
  void warn(String msg, Node faultyNode) {
    warnings.add(JSError.make(faultyNode, BAD_JSDOC_ANNOTATION, msg));
  }

  private ParameterType parseParameter(
      JSTypeExpression jsdoc, ParameterKind p,
      DeclaredTypeRegistry registry, ImmutableList<String> typeParameters) {
    if (jsdoc == null) {
      return null;
    }
    return parseParameter(jsdoc.getRoot(), p, registry, typeParameters);
  }

  private ParameterType parseParameter(
      Node jsdoc, ParameterKind p,
      DeclaredTypeRegistry registry, ImmutableList<String> typeParameters) {
    if (jsdoc == null) {
      return null;
    }
    switch (jsdoc.getType()) {
      case Token.EQUALS:
        p = ParameterKind.OPTIONAL;
        jsdoc = jsdoc.getFirstChild();
        break;
      case Token.ELLIPSIS:
        p = ParameterKind.REST;
        jsdoc = jsdoc.getFirstChild();
        break;
    }
    JSType t = getMaybeTypeFromComment(jsdoc, registry, typeParameters);
    return new ParameterType(t, p);
  }

  private static class ParameterType {
    private JSType type;
    private ParameterKind kind;

    ParameterType(JSType type, ParameterKind kind) {
      this.type = type;
      this.kind = kind;
    }
  }

  private static enum ParameterKind {
    REQUIRED,
    OPTIONAL,
    REST,
  }
}
