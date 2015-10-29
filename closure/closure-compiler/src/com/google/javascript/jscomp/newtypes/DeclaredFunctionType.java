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

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class represents the function types for functions that are defined
 * statically in the code.
 *
 * Since these types may have incomplete ATparam and ATreturns JSDoc, this
 * class needs to allow null JSTypes for undeclared formals/return.
 * Used in the Scope class.
 *
 * @author blickly@google.com (Ben Lickly)
 * @author dimvar@google.com (Dimitris Vardoulakis)
 */
public final class DeclaredFunctionType {
  private final List<JSType> requiredFormals;
  private final List<JSType> optionalFormals;
  private final JSType restFormals;
  private final JSType returnType;
  // Non-null iff this is a constructor/interface
  private final NominalType nominalType;
  // Non-null iff this is a prototype method
  private final NominalType receiverType;
  // Non-empty iff this function has an @template annotation
  private final ImmutableList<String> typeParameters;

  private DeclaredFunctionType(
      List<JSType> requiredFormals,
      List<JSType> optionalFormals,
      JSType restFormals,
      JSType retType,
      NominalType nominalType,
      NominalType receiverType,
      ImmutableList<String> typeParameters) {
    this.requiredFormals = requiredFormals;
    this.optionalFormals = optionalFormals;
    this.restFormals = restFormals;
    this.returnType = retType;
    this.nominalType = nominalType;
    this.receiverType = receiverType;
    this.typeParameters = typeParameters;
  }

  public FunctionType toFunctionType() {
    FunctionTypeBuilder builder = new FunctionTypeBuilder();
    for (JSType formal : requiredFormals) {
      builder.addReqFormal(formal == null ? JSType.UNKNOWN : formal);
    }
    for (JSType formal : optionalFormals) {
      builder.addOptFormal(formal == null ? JSType.UNKNOWN : formal);
    }
    builder.addRestFormals(restFormals);
    builder.addRetType(returnType == null ? JSType.UNKNOWN : returnType);
    builder.addNominalType(nominalType);
    builder.addReceiverType(receiverType);
    builder.addTypeParameters(typeParameters);
    return builder.buildFunction();
  }

  static DeclaredFunctionType make(
      List<JSType> requiredFormals,
      List<JSType> optionalFormals,
      JSType restFormals,
      JSType retType,
      NominalType nominalType,
      NominalType receiverType,
      ImmutableList<String> typeParameters) {
    if (requiredFormals == null) {
      requiredFormals = new ArrayList<>();
    }
    if (optionalFormals == null) {
      optionalFormals = new ArrayList<>();
    }
    if (typeParameters == null) {
      typeParameters = ImmutableList.of();
    }
    return new DeclaredFunctionType(
        requiredFormals, optionalFormals, restFormals, retType,
        nominalType, receiverType, typeParameters);
  }

  // 0-indexed
  public JSType getFormalType(int argpos) {
    int numReqFormals = requiredFormals.size();
    if (argpos < numReqFormals) {
      return requiredFormals.get(argpos);
    } else if (argpos < numReqFormals + optionalFormals.size()) {
      return optionalFormals.get(argpos - numReqFormals);
    } else {
      // TODO(blickly): Distinguish between undeclared varargs and no varargs.
      return restFormals;
    }
  }

  public int getRequiredArity() {
    return requiredFormals.size();
  }

  public int getOptionalArity() {
    return requiredFormals.size() + optionalFormals.size();
  }

  public boolean hasRestFormals() {
    return restFormals != null;
  }

  public JSType getRestFormalsType() {
    Preconditions.checkState(restFormals != null);
    return restFormals;
  }

  public JSType getReturnType() {
    return returnType;
  }

  public NominalType getThisType() {
    if (nominalType != null) {
      return nominalType;
    } else {
      return receiverType;
    }
  }

  public NominalType getNominalType() {
    return nominalType;
  }

  public NominalType getReceiverType() {
    return receiverType;
  }

  public boolean isGeneric() {
    return !typeParameters.isEmpty();
  }

  public ImmutableList<String> getTypeParameters() {
    return typeParameters;
  }

  public boolean isTypeVariableDefinedLocally(String tvar) {
    if (typeParameters.contains(tvar)) {
      return true;
    }
    // We don't look at this.nominalType, b/c if this function is a generic
    // constructor, then typeParameters contains the relevant type variables.
    if (receiverType != null && receiverType.isUninstantiatedGenericType()) {
      RawNominalType rawType = receiverType.getRawNominalType();
      if (rawType.getTypeParameters().contains(tvar)) {
        return true;
      }
    }
    return false;
  }

  public DeclaredFunctionType withReceiverType(NominalType newReceiver) {
    return new DeclaredFunctionType(
        this.requiredFormals, this.optionalFormals,
        this.restFormals, this.returnType, this.nominalType,
        newReceiver,
        this.typeParameters);
  }

  public DeclaredFunctionType withTypeInfoFromSuper(
      DeclaredFunctionType superType, boolean getsTypeInfoFromParentMethod) {
    // getsTypeInfoFromParentMethod is true when a method without jsdoc overrides
    // a parent method. In this case, the parent may be declaring some formals
    // as optional and we want to preserve that type information here.
    if (getsTypeInfoFromParentMethod) {
      return new DeclaredFunctionType(
          superType.requiredFormals, superType.optionalFormals,
          superType.restFormals, superType.returnType, superType.nominalType,
          this.receiverType, // only keep this from the current type
          superType.typeParameters);
    }

    FunctionTypeBuilder builder = new FunctionTypeBuilder();
    int i = 0;
    for (JSType formal : requiredFormals) {
      builder.addReqFormal(formal != null ? formal : superType.getFormalType(i));
      i++;
    }
    for (JSType formal : optionalFormals) {
      builder.addOptFormal(formal != null ? formal : superType.getFormalType(i));
      i++;
    }
    if (restFormals != null) {
      builder.addRestFormals(restFormals);
    } else if (superType.hasRestFormals()) {
      builder.addRestFormals(superType.restFormals);
    }
    builder.addRetType(returnType != null ? returnType : superType.returnType);
    builder.addNominalType(nominalType);
    builder.addReceiverType(receiverType);
    if (!typeParameters.isEmpty()) {
      builder.addTypeParameters(typeParameters);
    } else if (!superType.typeParameters.isEmpty()) {
      builder.addTypeParameters(superType.typeParameters);
    }
    return builder.buildDeclaration();
  }

  // Analogous to FunctionType#substituteNominalGenerics
  public DeclaredFunctionType substituteNominalGenerics(NominalType nt) {
    if (!nt.isGeneric()) {
      return this;
    }
    Map<String, JSType> typeMap = nt.getTypeMap();
    Preconditions.checkState(!typeMap.isEmpty());
    Map<String, JSType> reducedMap = typeMap;
    boolean foundShadowedTypeParam = false;
    for (String typeParam : typeParameters) {
      if (typeMap.containsKey(typeParam)) {
        foundShadowedTypeParam = true;
        break;
      }
    }
    if (foundShadowedTypeParam) {
      ImmutableMap.Builder<String, JSType> builder = ImmutableMap.builder();
      for (Map.Entry<String, JSType> entry : typeMap.entrySet()) {
        if (!typeParameters.contains(entry.getKey())) {
          builder.put(entry);
        }
      }
      reducedMap = builder.build();
    }
    FunctionTypeBuilder builder = new FunctionTypeBuilder();
    for (JSType reqFormal : requiredFormals) {
      builder.addReqFormal(reqFormal == null ? null : reqFormal.substituteGenerics(reducedMap));
    }
    for (JSType optFormal : optionalFormals) {
      builder.addOptFormal(optFormal == null ? null : optFormal.substituteGenerics(reducedMap));
    }
    if (restFormals != null) {
      builder.addRestFormals(restFormals.substituteGenerics(reducedMap));
    }
    if (returnType != null) {
      builder.addRetType(returnType.substituteGenerics(reducedMap));
    }
    // Explicitly forget nominalType and receiverType. This method is used when
    // calculating the declared type of a method using the inherited types.
    // In withTypeInfoFromSuper, we ignore super's nominalType and receiverType.
    builder.addTypeParameters(this.typeParameters);
    return builder.buildDeclaration();
  }

  public static DeclaredFunctionType meet(
      Collection<DeclaredFunctionType> toMeet) {
    DeclaredFunctionType result = null;
    for (DeclaredFunctionType declType : toMeet) {
      if (result == null) {
        result = declType;
      } else {
        result = DeclaredFunctionType.meet(result, declType);
      }
    }
    return result;
  }

  private static DeclaredFunctionType meet(
      DeclaredFunctionType f1, DeclaredFunctionType f2) {
    if (f1.equals(f2)) {
      return f1;
    }

    FunctionTypeBuilder builder = new FunctionTypeBuilder();
    int minRequiredArity = Math.min(
        f1.requiredFormals.size(), f2.requiredFormals.size());
    for (int i = 0; i < minRequiredArity; i++) {
      builder.addReqFormal(nullAcceptingJoin(
          f1.getFormalType(i), f2.getFormalType(i)));
    }
    int maxTotalArity = Math.max(
        f1.requiredFormals.size() + f1.optionalFormals.size(),
        f2.requiredFormals.size() + f2.optionalFormals.size());
    for (int i = minRequiredArity; i < maxTotalArity; i++) {
      builder.addOptFormal(nullAcceptingJoin(
          f1.getFormalType(i), f2.getFormalType(i)));
    }
    if (f1.restFormals != null || f2.restFormals != null) {
      builder.addRestFormals(
          nullAcceptingJoin(f1.restFormals, f2.restFormals));
    }
    builder.addRetType(
        nullAcceptingMeet(f1.returnType, f2.returnType));
    return builder.buildDeclaration();
  }

  // Returns possibly-null JSType
  private static JSType nullAcceptingJoin(JSType t1, JSType t2) {
    if (t1 == null) {
      return t2;
    } else if (t2 == null) {
      return t1;
    }
    return JSType.join(t1, t2);
  }

  // Returns possibly-null JSType
  private static JSType nullAcceptingMeet(JSType t1, JSType t2) {
    if (t1 == null) {
      return t2;
    } else if (t2 == null) {
      return t1;
    }
    return JSType.meet(t1, t2);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("Required formals", requiredFormals)
        .add("Optional formals", optionalFormals)
        .add("Varargs formals", restFormals)
        .add("Return", returnType)
        .add("Nominal type", nominalType).toString();
  }
}
