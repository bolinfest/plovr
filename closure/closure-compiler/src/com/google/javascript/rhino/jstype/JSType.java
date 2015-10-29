/*
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Bob Jervis
 *   Google Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package com.google.javascript.rhino.jstype;

import static com.google.javascript.rhino.jstype.TernaryValue.UNKNOWN;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.google.javascript.rhino.ErrorReporter;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.TypeI;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

/**
 * Represents JavaScript value types.<p>
 *
 * Types are split into two separate families: value types and object types.
 *
 * A special {@link UnknownType} exists to represent a wildcard type on which
 * no information can be gathered. In particular, it can assign to everyone,
 * is a subtype of everyone (and everyone is a subtype of it).<p>
 *
 * If you remove the {@link UnknownType}, the set of types in the type system
 * forms a lattice with the {@link #isSubtype} relation defining the partial
 * order of types. All types are united at the top of the lattice by the
 * {@link AllType} and at the bottom by the {@link NoType}.<p>
 *
 */
public abstract class JSType implements TypeI, Serializable {
  private static final long serialVersionUID = 1L;

  private boolean resolved = false;
  private JSType resolveResult = null;
  protected TemplateTypeMap templateTypeMap;

  private boolean inTemplatedCheckVisit = false;
  private static final CanCastToVisitor CAN_CAST_TO_VISITOR =
      new CanCastToVisitor();

  /**
   * Total ordering on types based on their textual representation.
   * This is used to have a deterministic output of the toString
   * method of the union type since this output is used in tests.
   */
  static final Comparator<JSType> ALPHA = new Comparator<JSType>() {
    @Override
    public int compare(JSType t1, JSType t2) {
      return t1.toString().compareTo(t2.toString());
    }
  };

  final JSTypeRegistry registry;

  JSType(JSTypeRegistry registry) {
    this(registry, null);
  }

  JSType(JSTypeRegistry registry, TemplateTypeMap templateTypeMap) {
    this.registry = registry;

    this.templateTypeMap = templateTypeMap == null ?
        registry.createTemplateTypeMap(null, null) : templateTypeMap;
  }

  /**
   * Utility method for less verbose code.
   */
  JSType getNativeType(JSTypeNative typeId) {
    return registry.getNativeType(typeId);
  }

  /**
   * Gets the docInfo for this type. By default, documentation cannot be
   * attached to arbitrary types. This must be overridden for
   * programmer-defined types.
   */
  public JSDocInfo getJSDocInfo() {
    return null;
  }

  /**
   * Returns a user meaningful label for the JSType instance.  For example,
   * Functions and Enums will return their declaration name (if they have one).
   * Some types will not have a meaningful display name.  Calls to
   * hasDisplayName() will return true IFF getDisplayName() will return null
   * or a zero length string.
   *
   * @return the display name of the type, or null if one is not available
   */
  public String getDisplayName() {
    return null;
  }

  /**
   * @return true if the JSType has a user meaningful label.
   */
  public boolean hasDisplayName() {
    String displayName = getDisplayName();
    return displayName != null && !displayName.isEmpty();
  }

  /**
   * Checks whether the property is present on the object.
   * @param pname The property name.
   */
  public boolean hasProperty(String pname) {
    return false;
  }

  public boolean isNoType() {
    return false;
  }

  public boolean isNoResolvedType() {
    return false;
  }

  public boolean isNoObjectType() {
    return false;
  }

  public final boolean isEmptyType() {
    return isNoType() || isNoObjectType() || isNoResolvedType() ||
        (registry.getNativeFunctionType(
             JSTypeNative.LEAST_FUNCTION_TYPE) == this);
  }

  public boolean isNumberObjectType() {
    return false;
  }

  public boolean isNumberValueType() {
    return false;
  }

  /** Whether this is the prototype of a function. */
  public boolean isFunctionPrototypeType() {
    return false;
  }

  public boolean isStringObjectType() {
    return false;
  }

  boolean isTheObjectType() {
    return false;
  }

  public boolean isStringValueType() {
    return false;
  }

  /**
   * Tests whether the type is a string (value or Object).
   * @return <code>this &lt;: (String, string)</code>
   */
  public final boolean isString() {
    return isSubtype(
        getNativeType(JSTypeNative.STRING_VALUE_OR_OBJECT_TYPE));
  }

  /**
   * Tests whether the type is a number (value or Object).
   * @return <code>this &lt;: (Number, number)</code>
   */
  public final boolean isNumber() {
    return isSubtype(
        getNativeType(JSTypeNative.NUMBER_VALUE_OR_OBJECT_TYPE));
  }

  public boolean isArrayType() {
    return false;
  }

  public boolean isBooleanObjectType() {
    return false;
  }

  public boolean isBooleanValueType() {
    return false;
  }

  public boolean isRegexpType() {
    return false;
  }

  public boolean isDateType() {
    return false;
  }

  public boolean isNullType() {
    return false;
  }

  public boolean isVoidType() {
    return false;
  }

  public boolean isAllType() {
    return false;
  }

  public boolean isUnknownType() {
    return false;
  }

  public boolean isCheckedUnknownType() {
    return false;
  }

  public final boolean isUnionType() {
    return toMaybeUnionType() != null;
  }

  /**
   * Returns true iff {@code this} can be a {@code struct}.
   * UnionType overrides the method, assume {@code this} is not a union here.
   */
  public boolean isStruct() {
    if (isObject()) {
      ObjectType objType = toObjectType();
      ObjectType iproto = objType.getImplicitPrototype();
      // For the case when a @struct constructor is assigned to a function's
      // prototype property
      if (iproto != null && iproto.isStruct()) {
        return true;
      }
      FunctionType ctor = objType.getConstructor();
      // This test is true for object literals
      if (ctor == null) {
        JSDocInfo info = objType.getJSDocInfo();
        return info != null && info.makesStructs();
      } else {
        return ctor.makesStructs();
      }
    }
    return false;
  }

  /**
   * Returns true iff {@code this} can be a {@code dict}.
   * UnionType overrides the method, assume {@code this} is not a union here.
   */
  public boolean isDict() {
    if (isObject()) {
      ObjectType objType = toObjectType();
      ObjectType iproto = objType.getImplicitPrototype();
      // For the case when a @dict constructor is assigned to a function's
      // prototype property
      if (iproto != null && iproto.isDict()) {
        return true;
      }
      FunctionType ctor = objType.getConstructor();
      // This test is true for object literals
      if (ctor == null) {
        JSDocInfo info = objType.getJSDocInfo();
        return info != null && info.makesDicts();
      } else {
        return ctor.makesDicts();
      }
    }
    return false;
  }

  /**
   * Downcasts this to a UnionType, or returns null if this is not a UnionType.
   *
   * Named in honor of Haskell's Maybe type constructor.
   */
  public UnionType toMaybeUnionType() {
    return null;
  }

  /** Returns true if this is a global this type. */
  public final boolean isGlobalThisType() {
    return this == registry.getNativeType(JSTypeNative.GLOBAL_THIS);
  }

  /** Returns true if toMaybeFunctionType returns a non-null FunctionType. */
  public final boolean isFunctionType() {
    return toMaybeFunctionType() != null;
  }

  /**
   * Downcasts this to a FunctionType, or returns null if this is not
   * a function.
   *
   * For the purposes of this function, we define a MaybeFunctionType as any
   * type in the sub-lattice
   * { x | LEAST_FUNCTION_TYPE <= x <= GREATEST_FUNCTION_TYPE }
   * This definition excludes bottom types like NoType and NoObjectType.
   *
   * This definition is somewhat arbitrary and axiomatic, but this is the
   * definition that makes the most sense for the most callers.
   */
  public FunctionType toMaybeFunctionType() {
    return null;
  }

  /**
   * Null-safe version of toMaybeFunctionType().
   */
  public static FunctionType toMaybeFunctionType(JSType type) {
    return type == null ? null : type.toMaybeFunctionType();
  }

  public final boolean isEnumElementType() {
    return toMaybeEnumElementType() != null;
  }

  /**
   * Downcasts this to an EnumElementType, or returns null if this is not an EnumElementType.
   */
  public EnumElementType toMaybeEnumElementType() {
    return null;
  }

  public boolean isEnumType() {
    return toMaybeEnumType() != null;
  }

  /**
   * Downcasts this to an EnumType, or returns null if this is not an EnumType.
   */
  public EnumType toMaybeEnumType() {
    return null;
  }

  public boolean isNamedType() {
    return toMaybeNamedType() != null;
  }

  public NamedType toMaybeNamedType() {
    return null;
  }

  public boolean isRecordType() {
    return toMaybeRecordType() != null;
  }

  public boolean isStructuralInterface() {
    return false;
  }

  public boolean isStructuralType() {
    return false;
  }

  /**
   * Downcasts this to a RecordType, or returns null if this is not
   * a RecordType.
   */
  public RecordType toMaybeRecordType() {
    return null;
  }

  public final boolean isTemplatizedType() {
    return toMaybeTemplatizedType() != null;
  }

  /**
   * Downcasts this to a TemplatizedType, or returns null if this is not
   * a function.
   */
  public TemplatizedType toMaybeTemplatizedType() {
    return null;
  }

  public final boolean isTemplateType() {
    return toMaybeTemplateType() != null;
  }

  /**
   * Downcasts this to a TemplateType, or returns null if this is not
   * a function.
   */
  public TemplateType toMaybeTemplateType() {
    return null;
  }

  public boolean hasAnyTemplateTypes() {
    if (!this.inTemplatedCheckVisit) {
      this.inTemplatedCheckVisit = true;
      boolean result = hasAnyTemplateTypesInternal();
      this.inTemplatedCheckVisit = false;
      return result;
    } else {
      // prevent infinite recursion, this is "not yet".
      return false;
    }
  }

  boolean hasAnyTemplateTypesInternal() {
    return templateTypeMap.hasAnyTemplateTypesInternal();
  }

  /**
   * Returns the template type map associated with this type.
   */
  public TemplateTypeMap getTemplateTypeMap() {
    return templateTypeMap;
  }

  /**
   * Extends the template type map associated with this type, merging in the
   * keys and values of the specified map.
   */
  public void extendTemplateTypeMap(TemplateTypeMap otherMap) {
    templateTypeMap = templateTypeMap.extend(otherMap);
  }

  /**
   * Tests whether this type is an {@code Object}, or any subtype thereof.
   * @return <code>this &lt;: Object</code>
   */
  public boolean isObject() {
    return false;
  }

  /**
   * Whether this type is a {@link FunctionType} that is a constructor or a
   * named type that points to such a type.
   */
  public boolean isConstructor() {
    return false;
  }

  /**
   * Whether this type is a nominal type (a named instance object or
   * a named enum).
   */
  public boolean isNominalType() {
    return false;
  }

  /**
   * Whether this type is the original constructor of a nominal type.
   * Does not include structural constructors.
   */
  public final boolean isNominalConstructor() {
    if (isConstructor() || isInterface()) {
      FunctionType fn = toMaybeFunctionType();
      if (fn == null) {
        return false;
      }

      // Programmer-defined constructors will have a link
      // back to the original function in the source tree.
      // Structural constructors will not.
      if (fn.getSource() != null) {
        return true;
      }

      // Native constructors are always nominal.
      return fn.isNativeObjectType();
    }
    return false;
  }

  /**
   * Whether this type is an Instance object of some constructor.
   * Does not necessarily mean this is an {@link InstanceObjectType}.
   */
  public boolean isInstanceType() {
    return false;
  }

  /**
   * Whether this type is a {@link FunctionType} that is an interface or a named
   * type that points to such a type.
   */
  public boolean isInterface() {
    return false;
  }

  /**
   * Whether this type is a {@link FunctionType} that is an ordinary function or
   * a named type that points to such a type.
   */
  public boolean isOrdinaryFunction() {
    return false;
  }

  /**
   * Checks if two types are equivalent.
   */
  @Override
  public final boolean isEquivalentTo(TypeI that) {
    return checkEquivalenceHelper((JSType) that, EquivalenceMethod.IDENTITY);
  }

  public final boolean isEquivalentTo(TypeI that, boolean isStructural) {
    EqCache eqCache = isStructural ? EqCache.create()
        : EqCache.createWithoutStructuralTyping();
    return checkEquivalenceHelper((JSType) that,
        EquivalenceMethod.IDENTITY, eqCache);
  }

  /**
   * Whether this type is meaningfully different from {@code that} type for
   * the purposes of data flow analysis.
   *
   * This is a trickier check than pure equality, because it has to properly
   * handle unknown types. See {@code EquivalenceMethod} for more info.
   *
   * @see <a href="http://www.youtube.com/watch?v=_RpSv3HjpEw">Unknown unknowns</a>
   */
  public final boolean differsFrom(JSType that) {
    return !checkEquivalenceHelper(that, EquivalenceMethod.DATA_FLOW);
  }

  /**
   * An equivalence visitor.
   */
  boolean checkEquivalenceHelper(
      final JSType that, EquivalenceMethod eqMethod) {
    return checkEquivalenceHelper(that, eqMethod, EqCache.create());
  }

  boolean checkEquivalenceHelper(final JSType that, EquivalenceMethod eqMethod,
      EqCache eqCache) {
    if (this == that) {
      return true;
    }

    boolean thisUnknown = isUnknownType();
    boolean thatUnknown = that.isUnknownType();
    if (thisUnknown || thatUnknown) {
      if (eqMethod == EquivalenceMethod.INVARIANT) {
        // If we're checking for invariance, the unknown type is invariant
        // with everyone.
        return true;
      } else if (eqMethod == EquivalenceMethod.DATA_FLOW) {
        // If we're checking data flow, then two types are the same if they're
        // both unknown.
        return thisUnknown && thatUnknown;
      } else if (thisUnknown && thatUnknown &&
          (isNominalType() ^ that.isNominalType())) {
        // If they're both unknown, but one is a nominal type and the other
        // is not, then we should fail out immediately. This ensures that
        // we won't unbox the unknowns further down.
        return false;
      }
    }

    if (isUnionType() && that.isUnionType()) {
      return toMaybeUnionType().checkUnionEquivalenceHelper(
          that.toMaybeUnionType(), eqMethod, eqCache);
    }

    if (isFunctionType() && that.isFunctionType()) {
      return toMaybeFunctionType().checkFunctionEquivalenceHelper(
          that.toMaybeFunctionType(), eqMethod, eqCache);
    }

    if (isRecordType() && that.isRecordType()) {
      return toMaybeRecordType().checkRecordEquivalenceHelper(
          that.toMaybeRecordType(), eqMethod, eqCache);
    }

    if (!getTemplateTypeMap().checkEquivalenceHelper(
        that.getTemplateTypeMap(), eqMethod, eqCache)) {
      return false;
    }

    if (eqCache.isStructuralTyping()
        && this.isStructuralType() && that.isStructuralType()) {
      // TODO: handle template type equivalence, see:
      // testDuplicateVariableDefinition8_7 in TypeCheckTest.java
      return checkStructuralEquivalenceHelper(that, eqMethod, eqCache);
    }

    if (isNominalType() && that.isNominalType()) {
      // TODO(johnlenz): is this valid across scopes?
      return getConcreteNominalTypeName(this.toObjectType())
          .equals(getConcreteNominalTypeName(that.toObjectType()));
    }

    if (isTemplateType() && that.isTemplateType()) {
      // TemplateType are they same only if they are object identical,
      // which we check at the start of this function.
      return false;
    }

    // Unbox other proxies.
    if (this instanceof ProxyObjectType) {
      return ((ProxyObjectType) this)
          .getReferencedTypeInternal().checkEquivalenceHelper(
              that, eqMethod, eqCache);
    }

    if (that instanceof ProxyObjectType) {
      return checkEquivalenceHelper(
          ((ProxyObjectType) that).getReferencedTypeInternal(),
          eqMethod, eqCache);
    }

    // Relies on the fact that for the base {@link JSType}, only one
    // instance of each sub-type will ever be created in a given registry, so
    // there is no need to verify members. If the object pointers are not
    // identical, then the type member must be different.
    return false;
  }

  private boolean checkStructuralEquivalenceHelper(final JSType that,
      EquivalenceMethod eqMethod, EqCache eqCache) {
    if (this.isFunctionType() || that.isFunctionType()) {
      return false;
    }
    Preconditions.checkState(eqCache.isStructuralTyping());
    Preconditions.checkState((isRecordType() || that.isRecordType())
        || isNominalType() && that.isNominalType());

    if (isNominalType() && that.isNominalType()) {
      if (getConcreteNominalTypeName(this.toObjectType())
          .equals(getConcreteNominalTypeName(that.toObjectType()))) {
        return true;
      }
      FunctionType thatConstructor = that.toObjectType().getConstructor();
      FunctionType thisConstructor = this.toObjectType().getConstructor();
      return thisConstructor.checkStructuralInterfaceEquivalenceHelper(
          thatConstructor, eqMethod, eqCache);
    }
    return checkObjectRecordEquivalenceHelper(that, eqMethod, eqCache);
  }

  /**
   * do structural equivalence check for a record type and
   * an instance of a structural interface type
   */
  private boolean checkObjectRecordEquivalenceHelper(final JSType that,
      EquivalenceMethod eqMethod, EqCache eqCache) {
    Preconditions.checkState((isInstanceType() && that.isRecordType())
        || (isRecordType() && that.isInstanceType()));

    FunctionType constructor = isInstanceType() ?
        toObjectType().getConstructor()
        : that.toObjectType().getConstructor();
    RecordType recordType = isRecordType() ? toMaybeRecordType()
        : that.toMaybeRecordType();
    return constructor.checkStructuralInterfaceEquivalenceHelper(
        recordType, eqMethod, eqCache);
  }

  // Named types may be proxies of concrete types.
  private String getConcreteNominalTypeName(ObjectType objType) {
    if (objType instanceof ProxyObjectType) {
      ObjectType internal = ((ProxyObjectType) objType)
          .getReferencedObjTypeInternal();
      if (internal != null && internal.isNominalType()) {
        return getConcreteNominalTypeName(internal);
      }
    }
    return objType.getReferenceName();
  }

  public static boolean isEquivalent(JSType typeA, JSType typeB) {
    return (typeA == null || typeB == null)
        ? typeA == typeB : typeA.isEquivalentTo(typeB);
  }

  @Override
  public boolean equals(Object jsType) {
    return (jsType instanceof JSType) && isEquivalentTo((JSType) jsType);
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  /**
   * This predicate is used to test whether a given type can appear in a
   * 'Int32' context.  This context includes, for example, the operands of a
   * bitwise or operator.  Since we do not currently support integer types,
   * this is a synonym for {@code Number}.
   */
  public final boolean matchesInt32Context() {
    return matchesNumberContext();
  }

  /**
   * This predicate is used to test whether a given type can appear in a
   * 'Uint32' context.  This context includes the right-hand operand of a shift
   * operator.
   */
  public final boolean matchesUint32Context() {
    return matchesNumberContext();
  }

  /**
   * This predicate is used to test whether a given type can appear in a
   * numeric context, such as an operand of a multiply operator.
   */
  public boolean matchesNumberContext() {
    return false;
  }

  /**
   * This predicate is used to test whether a given type can appear in a
   * {@code String} context, such as an operand of a string concat (+) operator.
   *
   * All types have at least the potential for converting to {@code String}.
   * When we add externally defined types, such as a browser OM, we may choose
   * to add types that do not automatically convert to {@code String}.
   */
  public boolean matchesStringContext() {
    return false;
  }

  /**
   * This predicate is used to test whether a given type can appear in an
   * {@code Object} context, such as the expression in a with statement.
   *
   * Most types we will encounter, except notably {@code null}, have at least
   * the potential for converting to {@code Object}.  Host defined objects can
   * get peculiar.
   */
  public boolean matchesObjectContext() {
    return false;
  }

  /**
   * Coerces this type to an Object type, then gets the type of the property
   * whose name is given.
   *
   * Unlike {@link ObjectType#getPropertyType}, returns null if the property
   * is not found.
   *
   * @return The property's type. {@code null} if the current type cannot
   *     have properties, or if the type is not found.
   */
  public JSType findPropertyType(String propertyName) {
    ObjectType autoboxObjType = ObjectType.cast(autoboxesTo());
    if (autoboxObjType != null) {
      return autoboxObjType.findPropertyType(propertyName);
    }

    return null;
  }

  /**
   * This predicate is used to test whether a given type can be used as the
   * 'function' in a function call.
   *
   * @return {@code true} if this type might be callable.
   */
  public boolean canBeCalled() {
    return false;
  }

  /**
   * Tests whether values of {@code this} type can be safely assigned
   * to values of {@code that} type.<p>
   *
   * The default implementation verifies that {@code this} is a subtype
   * of {@code that}.<p>
   */
  public boolean canCastTo(JSType that) {
    return this.visit(CAN_CAST_TO_VISITOR, that);
  }

  /**
   * Turn a scalar type to the corresponding object type.
   *
   * @return the auto-boxed type or {@code null} if this type is not a scalar.
   */
  public JSType autoboxesTo() {
    return null;
  }

  /**
   * Turn an object type to its corresponding scalar type.
   *
   * @return the unboxed type or {@code null} if this type does not unbox.
   */
  public JSType unboxesTo() {
    return null;
  }

  /**
   * Casts this to an ObjectType, or returns null if this is not an ObjectType.
   * If this is a scalar type, it will *not* be converted to an object type.
   * If you want to simulate JS autoboxing or dereferencing, you should use
   * autoboxesTo() or dereference().
   */
  public ObjectType toObjectType() {
    return this instanceof ObjectType ? (ObjectType) this : null;
  }

  /**
   * Dereference a type for property access.
   *
   * Filters null/undefined and autoboxes the resulting type.
   * Never returns null.
   */
  public JSType autobox() {
    JSType restricted = restrictByNotNullOrUndefined();
    JSType autobox = restricted.autoboxesTo();
    return autobox == null ? restricted : autobox;
  }

  /**
   * Dereference a type for property access.
   *
   * Filters null/undefined, autoboxes the resulting type, and returns it
   * iff it's an object.
   */
  public final ObjectType dereference() {
    return autobox().toObjectType();
  }

  /**
   * Tests whether {@code this} and {@code that} are meaningfully
   * comparable. By meaningfully, we mean compatible types that do not lead
   * to step 22 of the definition of the Abstract Equality Comparison
   * Algorithm (11.9.3, page 55&ndash;56) of the ECMA-262 specification.<p>
   */
  public final boolean canTestForEqualityWith(JSType that) {
    return testForEquality(that).equals(UNKNOWN);
  }

  /**
   * Compares {@code this} and {@code that}.
   * @return <ul>
   * <li>{@link TernaryValue#TRUE} if the comparison of values of
   *   {@code this} type and {@code that} always succeed (such as
   *   {@code undefined} compared to {@code null})</li>
   * <li>{@link TernaryValue#FALSE} if the comparison of values of
   *   {@code this} type and {@code that} always fails (such as
   *   {@code undefined} compared to {@code number})</li>
   * <li>{@link TernaryValue#UNKNOWN} if the comparison can succeed or
   *   fail depending on the concrete values</li>
   * </ul>
   */
  public TernaryValue testForEquality(JSType that) {
    return testForEqualityHelper(this, that);
  }

  TernaryValue testForEqualityHelper(JSType aType, JSType bType) {
    if (bType.isAllType() || bType.isUnknownType() ||
        bType.isNoResolvedType() ||
        aType.isAllType() || aType.isUnknownType() ||
        aType.isNoResolvedType()) {
      return UNKNOWN;
    }

    boolean aIsEmpty = aType.isEmptyType();
    boolean bIsEmpty = bType.isEmptyType();
    if (aIsEmpty || bIsEmpty) {
      if (aIsEmpty && bIsEmpty) {
        return TernaryValue.TRUE;
      } else {
        return UNKNOWN;
      }
    }

    if (aType.isFunctionType() || bType.isFunctionType()) {
      JSType otherType = aType.isFunctionType() ? bType : aType;
      // In theory, functions are comparable to anything except
      // null/undefined. For example, on FF3:
      // function() {} == 'function () {\n}'
      // In practice, how a function serializes to a string is
      // implementation-dependent, so it does not really make sense to test
      // for equality with a string.
      JSType meet = otherType.getGreatestSubtype(
          getNativeType(JSTypeNative.OBJECT_TYPE));
      if (meet.isNoType() || meet.isNoObjectType()) {
        return TernaryValue.FALSE;
      } else {
        return TernaryValue.UNKNOWN;
      }
    }
    if (bType.isEnumElementType() || bType.isUnionType()) {
      return bType.testForEquality(aType);
    }
    return null;
  }

  /**
   * Tests whether {@code this} and {@code that} are meaningfully
   * comparable using shallow comparison. By meaningfully, we mean compatible
   * types that are not rejected by step 1 of the definition of the Strict
   * Equality Comparison Algorithm (11.9.6, page 56&ndash;57) of the
   * ECMA-262 specification.<p>
   */
  public final boolean canTestForShallowEqualityWith(JSType that) {
    if (isEmptyType() || that.isEmptyType()) {
      return isSubtype(that) || that.isSubtype(this);
    }

    JSType inf = getGreatestSubtype(that);
    return !inf.isEmptyType() ||
        // Our getGreatestSubtype relation on functions is pretty bad.
        // Let's just say it's always ok to compare two functions.
        // Once the TODO in FunctionType is fixed, we should be able to
        // remove this.
        inf == registry.getNativeType(JSTypeNative.LEAST_FUNCTION_TYPE);
  }

  /**
   * Tests whether this type is nullable.
   */
  public boolean isNullable() {
    return isSubtype(getNativeType(JSTypeNative.NULL_TYPE));
  }

  /**
   * Tests whether this type is voidable.
   */
  public boolean isVoidable() {
    return isSubtype(getNativeType(JSTypeNative.VOID_TYPE));
  }

  /**
   * Gets the least supertype of this that's not a union.
   */
  public JSType collapseUnion() {
    return this;
  }

  /**
   * Gets the least supertype of {@code this} and {@code that}.
   * The least supertype is the join (&#8744;) or supremum of both types in the
   * type lattice.<p>
   * Examples:
   * <ul>
   * <li><code>number &#8744; *</code> = {@code *}</li>
   * <li><code>number &#8744; Object</code> = {@code (number, Object)}</li>
   * <li><code>Number &#8744; Object</code> = {@code Object}</li>
   * </ul>
   * @return <code>this &#8744; that</code>
   */
  public JSType getLeastSupertype(JSType that) {
    if (that.isUnionType()) {
      // Union types have their own implementation of getLeastSupertype.
      return that.toMaybeUnionType().getLeastSupertype(this);
    }
    return getLeastSupertype(this, that);
  }

  /**
   * A generic implementation meant to be used as a helper for common
   * getLeastSupertype implementations.
   */
  static JSType getLeastSupertype(JSType thisType, JSType thatType) {
    boolean areEquivalent = thisType.isEquivalentTo(thatType);
    return areEquivalent ? thisType :
        filterNoResolvedType(
            thisType.registry.createUnionType(thisType, thatType));
  }

  /**
   * Gets the greatest subtype of {@code this} and {@code that}.
   * The greatest subtype is the meet (&#8743;) or infimum of both types in the
   * type lattice.<p>
   * Examples
   * <ul>
   * <li><code>Number &#8743; Any</code> = {@code Any}</li>
   * <li><code>number &#8743; Object</code> = {@code Any}</li>
   * <li><code>Number &#8743; Object</code> = {@code Number}</li>
   * </ul>
   * @return <code>this &#8744; that</code>
   */
  public JSType getGreatestSubtype(JSType that) {
    return getGreatestSubtype(this, that);
  }

  /**
   * A generic implementation meant to be used as a helper for common
   * getGreatestSubtype implementations.
   */
  static JSType getGreatestSubtype(JSType thisType, JSType thatType) {
    if (thisType.isFunctionType() && thatType.isFunctionType()) {
      // The FunctionType sub-lattice is not well-defined. i.e., the
      // proposition
      // A < B => sup(A, B) == B
      // does not hold because of unknown parameters and return types.
      // See the comment in supAndInfHelper for more info on this.
      return thisType.toMaybeFunctionType().supAndInfHelper(
          thatType.toMaybeFunctionType(), false);
    } else if (thisType.isEquivalentTo(thatType)) {
      return thisType;
    } else if (thisType.isUnknownType() || thatType.isUnknownType()) {
      // The greatest subtype with any unknown type is the universal
      // unknown type, unless the two types are equal.
      return thisType.isEquivalentTo(thatType) ? thisType :
          thisType.getNativeType(JSTypeNative.UNKNOWN_TYPE);
    } else if (thisType.isUnionType()) {
      return thisType.toMaybeUnionType().meet(thatType);
    } else if (thatType.isUnionType()) {
      return thatType.toMaybeUnionType().meet(thisType);
    } else if (thisType.isTemplatizedType()) {
      return thisType.toMaybeTemplatizedType().getGreatestSubtypeHelper(
          thatType);
    }  else if (thatType.isTemplatizedType()) {
      return thatType.toMaybeTemplatizedType().getGreatestSubtypeHelper(
          thisType);
    } else if (thisType.isSubtype(thatType)) {
      return filterNoResolvedType(thisType);
    } else if (thatType.isSubtype(thisType)) {
      return filterNoResolvedType(thatType);
    } else if (thisType.isRecordType()) {
      return thisType.toMaybeRecordType().getGreatestSubtypeHelper(thatType);
    } else if (thatType.isRecordType()) {
      return thatType.toMaybeRecordType().getGreatestSubtypeHelper(thisType);
    }

    if (thisType.isEnumElementType()) {
      JSType inf = thisType.toMaybeEnumElementType().meet(thatType);
      if (inf != null) {
        return inf;
      }
    } else if (thatType.isEnumElementType()) {
      JSType inf = thatType.toMaybeEnumElementType().meet(thisType);
      if (inf != null) {
        return inf;
      }
    }

    if (thisType.isObject() && thatType.isObject()) {
      return thisType.getNativeType(JSTypeNative.NO_OBJECT_TYPE);
    }
    return thisType.getNativeType(JSTypeNative.NO_TYPE);
  }

  /**
   * When computing infima, we may get a situation like
   * inf(Type1, Type2)
   * where both types are unresolved, so they're technically
   * subtypes of one another.
   *
   * If this happens, filter them down to NoResolvedType.
   */
  static JSType filterNoResolvedType(JSType type) {
    if (type.isNoResolvedType()) {
      // inf(UnresolvedType1, UnresolvedType2) needs to resolve
      // to the base unresolved type, so that the relation is symmetric.
      return type.getNativeType(JSTypeNative.NO_RESOLVED_TYPE);
    } else if (type.isUnionType()) {
      UnionType unionType = type.toMaybeUnionType();
      boolean needsFiltering = false;
      for (JSType alt : unionType.getAlternates()) {
        if (alt.isNoResolvedType()) {
          needsFiltering = true;
          break;
        }
      }

      if (needsFiltering) {
        UnionTypeBuilder builder = new UnionTypeBuilder(type.registry);
        builder.addAlternate(type.getNativeType(JSTypeNative.NO_RESOLVED_TYPE));
        for (JSType alt : unionType.getAlternates()) {
          if (!alt.isNoResolvedType()) {
            builder.addAlternate(alt);
          }
        }
        return builder.build();
      }
    }
    return type;
  }

  /**
   * Computes the restricted type of this type knowing that the
   * {@code ToBoolean} predicate has a specific value. For more information
   * about the {@code ToBoolean} predicate, see
   * {@link #getPossibleToBooleanOutcomes}.
   *
   * @param outcome the value of the {@code ToBoolean} predicate
   *
   * @return the restricted type, or the Any Type if the underlying type could
   *         not have yielded this ToBoolean value
   *
   * TODO(user): Move this method to the SemanticRAI and use the visit
   * method of types to get the restricted type.
   */
  public JSType getRestrictedTypeGivenToBooleanOutcome(boolean outcome) {
    if (outcome && this == getNativeType(JSTypeNative.UNKNOWN_TYPE)) {
      return getNativeType(JSTypeNative.CHECKED_UNKNOWN_TYPE);
    }

    BooleanLiteralSet literals = getPossibleToBooleanOutcomes();
    if (literals.contains(outcome)) {
      return this;
    } else {
      return getNativeType(JSTypeNative.NO_TYPE);
    }
  }

  /**
   * Computes the set of possible outcomes of the {@code ToBoolean} predicate
   * for this type. The {@code ToBoolean} predicate is defined by the ECMA-262
   * standard, 3<sup>rd</sup> edition. Its behavior for simple types can be
   * summarized by the following table:
   * <table>
   * <tr><th>type</th><th>result</th></tr>
   * <tr><td>{@code undefined}</td><td>{false}</td></tr>
   * <tr><td>{@code null}</td><td>{false}</td></tr>
   * <tr><td>{@code boolean}</td><td>{true, false}</td></tr>
   * <tr><td>{@code number}</td><td>{true, false}</td></tr>
   * <tr><td>{@code string}</td><td>{true, false}</td></tr>
   * <tr><td>{@code Object}</td><td>{true}</td></tr>
   * </table>
   * @return the set of boolean literals for this type
   */
  public abstract BooleanLiteralSet getPossibleToBooleanOutcomes();

  /**
   * Computes the subset of {@code this} and {@code that} types if equality
   * is observed. If a value {@code v1} of type {@code null} is equal to a value
   * {@code v2} of type {@code (undefined,number)}, we can infer that the
   * type of {@code v1} is {@code null} and the type of {@code v2} is
   * {@code undefined}.
   *
   * @return a pair containing the restricted type of {@code this} as the first
   *         component and the restricted type of {@code that} as the second
   *         element. The returned pair is never {@code null} even though its
   *         components may be {@code null}
   */
  public TypePair getTypesUnderEquality(JSType that) {
    // unions types
    if (that.isUnionType()) {
      TypePair p = that.toMaybeUnionType().getTypesUnderEquality(this);
      return new TypePair(p.typeB, p.typeA);
    }

    // other types
    switch (testForEquality(that)) {
      case FALSE:
        return new TypePair(null, null);

      case TRUE:
      case UNKNOWN:
        return new TypePair(this, that);
    }

    // switch case is exhaustive
    throw new IllegalStateException();
  }

  /**
   * Computes the subset of {@code this} and {@code that} types if inequality
   * is observed. If a value {@code v1} of type {@code number} is not equal to a
   * value {@code v2} of type {@code (undefined,number)}, we can infer that the
   * type of {@code v1} is {@code number} and the type of {@code v2} is
   * {@code number} as well.
   *
   * @return a pair containing the restricted type of {@code this} as the first
   *         component and the restricted type of {@code that} as the second
   *         element. The returned pair is never {@code null} even though its
   *         components may be {@code null}
   */
  public TypePair getTypesUnderInequality(JSType that) {
    // unions types
    if (that.isUnionType()) {
      TypePair p = that.toMaybeUnionType().getTypesUnderInequality(this);
      return new TypePair(p.typeB, p.typeA);
    }

    // other types
    switch (testForEquality(that)) {
      case TRUE:
        JSType noType = getNativeType(JSTypeNative.NO_TYPE);
        return new TypePair(noType, noType);

      case FALSE:
      case UNKNOWN:
        return new TypePair(this, that);
    }

    // switch case is exhaustive
    throw new IllegalStateException();
  }

  /**
   * Computes the subset of {@code this} and {@code that} types under shallow
   * equality.
   *
   * @return a pair containing the restricted type of {@code this} as the first
   *         component and the restricted type of {@code that} as the second
   *         element. The returned pair is never {@code null} even though its
   *         components may be {@code null}.
   */
  public TypePair getTypesUnderShallowEquality(JSType that) {
    JSType commonType = getGreatestSubtype(that);
    return new TypePair(commonType, commonType);
  }

  /**
   * Computes the subset of {@code this} and {@code that} types under
   * shallow inequality.
   *
   * @return A pair containing the restricted type of {@code this} as the first
   *         component and the restricted type of {@code that} as the second
   *         element. The returned pair is never {@code null} even though its
   *         components may be {@code null}
   */
  public TypePair getTypesUnderShallowInequality(JSType that) {
    // union types
    if (that.isUnionType()) {
      TypePair p = that.toMaybeUnionType().getTypesUnderShallowInequality(this);
      return new TypePair(p.typeB, p.typeA);
    }

    // Other types.
    // There are only two types whose shallow inequality is deterministically
    // true -- null and undefined. We can just enumerate them.
    if (isNullType() && that.isNullType() ||
        isVoidType() && that.isVoidType()) {
      return new TypePair(null, null);
    } else {
      return new TypePair(this, that);
    }
  }

  /**
   * If this is a union type, returns a union type that does not include
   * the null or undefined type.
   */
  public JSType restrictByNotNullOrUndefined() {
    return this;
  }

  /**
   * the logic of this method is similar to isSubtype,
   * except that it does not perform structural interface matching
   *
   * This function is added for disambiguate properties,
   * and is deprecated for the other use cases.
   */
  public boolean isSubtypeWithoutStructuralTyping(JSType that) {
    return isSubtype(that, ImplCache.createWithoutStructuralTyping());
  }

  /**
   * Checks whether {@code this} is a subtype of {@code that}.<p>
   * Note this function also returns true if this type structurally
   * matches the protocol define by that type (if that type is an
   * interface function type)
   *
   * Subtyping rules:
   * <ul>
   * <li>(unknown) &mdash; every type is a subtype of the Unknown type.</li>
   * <li>(no) &mdash; the No type is a subtype of every type.</li>
   * <li>(no-object) &mdash; the NoObject type is a subtype of every object
   * type (i.e. subtypes of the Object type).</li>
   * <li>(ref) &mdash; a type is a subtype of itself.</li>
   * <li>(union-l) &mdash; A union type is a subtype of a type U if all the
   * union type's constituents are a subtype of U. Formally<br>
   * <code>(T<sub>1</sub>, &hellip;, T<sub>n</sub>) &lt;: U</code> if and only
   * <code>T<sub>k</sub> &lt;: U</code> for all <code>k &isin; 1..n</code>.</li>
   * <li>(union-r) &mdash; A type U is a subtype of a union type if it is a
   * subtype of one of the union type's constituents. Formally<br>
   * <code>U &lt;: (T<sub>1</sub>, &hellip;, T<sub>n</sub>)</code> if and only
   * if <code>U &lt;: T<sub>k</sub></code> for some index {@code k}.</li>
   * <li>(objects) &mdash; an Object <code>O<sub>1</sub></code> is a subtype
   * of an object <code>O<sub>2</sub></code> if it has more properties
   * than <code>O<sub>2</sub></code> and all common properties are
   * pairwise subtypes.</li>
   * </ul>
   *
   * @return <code>this &lt;: that</code>
   */
  public boolean isSubtype(JSType that) {
    return isSubtypeHelper(this, that,
        ImplCache.create());
  }

  /**
   * checking isSubtype with structural interface matching
   *
   * @param implicitImplCache a cache that records the checked
   * or currently checking type pairs, for example, if previous
   * checking found that constructor C is a subtype of interface I,
   * then in the cache, table key <I,C> maps to IMPLEMENT status.
   * @param that
   */
  protected boolean isSubtype(JSType that,
      ImplCache implicitImplCache) {
    return isSubtypeHelper(this, that, implicitImplCache);
  }

  /**
   * if implicitImplCache is null, then there will
   * be no structural interface matching
   */
  static boolean isSubtypeHelper(JSType thisType, JSType thatType,
      ImplCache implicitImplCache) {
    // unknown
    if (thatType.isUnknownType()) {
      return true;
    }
    // all type
    if (thatType.isAllType()) {
      return true;
    }
    // equality
    if (thisType.isEquivalentTo(thatType, implicitImplCache.isStructuralTyping())) {
      return true;
    }
    // unions
    if (thatType.isUnionType()) {
      UnionType union = thatType.toMaybeUnionType();
      for (JSType element : union.alternatesWithoutStucturalTyping) {
        if (thisType.isSubtype(element, implicitImplCache)) {
          return true;
        }
      }
      return false;
    }

    // TemplateTypeMaps. This check only returns false if the TemplateTypeMaps
    // are not equivalent.
    TemplateTypeMap thisTypeParams = thisType.getTemplateTypeMap();
    TemplateTypeMap thatTypeParams = thatType.getTemplateTypeMap();
    boolean templateMatch = true;
    if (isExemptFromTemplateTypeInvariance(thatType)) {
      // Array and Object are exempt from template type invariance; their
      // template types maps are considered a match only if the ObjectElementKey
      // values are subtypes/supertypes of one another.
      TemplateType key = thisType.registry.getObjectElementKey();
      JSType thisElement = thisTypeParams.getResolvedTemplateType(key);
      JSType thatElement = thatTypeParams.getResolvedTemplateType(key);

      templateMatch = thisElement.isSubtype(thatElement, implicitImplCache)
          || thatElement.isSubtype(thisElement, implicitImplCache);
    } else {
      templateMatch = thisTypeParams.checkEquivalenceHelper(
          thatTypeParams, EquivalenceMethod.INVARIANT);
    }
    if (!templateMatch) {
      return implicitImplCache.isStructuralTyping()
          && implicitMatch(thisType, thatType, implicitImplCache);
    }

    // Templatized types. The above check guarantees TemplateTypeMap
    // equivalence; check if the base type is a subtype.
    if (thisType.isTemplatizedType()) {
      return thisType.toMaybeTemplatizedType().getReferencedType().isSubtype(
          thatType, implicitImplCache);
    }

    // proxy types
    if (thatType instanceof ProxyObjectType) {
      return thisType.isSubtype(
          ((ProxyObjectType) thatType).getReferencedTypeInternal(), implicitImplCache);
    }
    return implicitImplCache.isStructuralTyping()
        && implicitMatch(thisType, thatType, implicitImplCache);
  }

  /**
   * perform structural interface matching to see if the second argument type
   * implicitly implements the first argument type. Consider this example:
   * varLeft = varRight;
   *
   * (this is an internal method that uses cache to speedup and
   * prevent infinite recursive invocation.
   * As an example of possible infinite recursive invocation,
   * see testStructuralInterfaceMatching13 in TypeCheckTest.java)
   *
   * @param rightType the type of varRight
   * @param leftType the type of varLeft
   * @param implicitImplCache
   * @return true if the second argument type structurally matches the
   * first argument type
   */
  protected static boolean implicitMatch(JSType rightType, JSType leftType,
      ImplCache implicitImplCache) {

    // union type should be handled by isSubtype already
    if (rightType.isUnionType() || leftType.isUnionType()) {
      return false;
    }
    // anything other than two object types was already handled
    if (!rightType.isObject() || !leftType.isObject()) {
      return false;
    }
    // currently the structural interface matching does not support
    // implicit matching for templatized type
    if (leftType.isTemplatizedType()) {
      return false;
    }
    // if both are functionTypes
    if (leftType.isStructuralInterface() && rightType.isFunctionType()) {
      return checkConstructorImplicitMatch(rightType.toMaybeFunctionType(),
          leftType.toMaybeFunctionType(), implicitImplCache);
    }
    // matches a record type and an interface
    if (leftType.isStructuralInterface() && rightType.isRecordType()) {
      return checkObjectImplicitMatch(rightType.toMaybeObjectType(),
          leftType.toMaybeFunctionType(), implicitImplCache);
    }
    // then both are object instance types
    FunctionType leftConstructor = leftType.toMaybeObjectType().getConstructor();
    FunctionType rightConstructor = rightType.toMaybeObjectType().getConstructor();
    if (leftConstructor == null || !leftConstructor.isStructuralInterface()) {
      return false;
    }
    if (rightConstructor != null) {
      return checkConstructorImplicitMatch(rightConstructor, leftConstructor,
          implicitImplCache);
    }
    return checkObjectImplicitMatch(
        rightType.toMaybeObjectType(), leftConstructor, implicitImplCache);
  }

  /**
   * check if the rightType (an object type)
   * is compatible with the leftType (must be an interface function type)
   *
   * @param rightType must be either a function type or a record type
   * @param leftType must be an interface function type
   * @param implicitImplCache
   * @return true if the rightType structurally matches the leftType
   */
  protected static boolean checkObjectImplicitMatch(
      ObjectType rightType, FunctionType leftType,
      ImplCache implicitImplCache) {
    Preconditions.checkArgument(leftType.isStructuralInterface());
    Map<String, JSType> leftPropList = getPropertyTypeMap(leftType);
    Map<String, JSType> rightPropList = getPropertyTypeMap(rightType);
    // structural interface matching
    for (String propName : leftPropList.keySet()) {
      JSType leftPropType = leftPropList.get(propName);
      JSType rightPropType = rightPropList.get(propName);
      if (rightPropType == null) {
        return false;
      }
      // apply structural interface matching on properties
      if (!rightPropType.isSubtype(leftPropType, implicitImplCache)) {
        return false;
      }
    }
    return true;
  }

  /**
   * check if functionType implicitly implements interfaceType
   *
   * @param rightType must be a function type
   * @param leftType (must be an interface function type)
   * @return true if all properties required by interfaceType
   * are in functionType
   */
  protected static boolean checkConstructorImplicitMatch(FunctionType rightType,
      FunctionType leftType,
      ImplCache implicitImplCache) {
    // non-interfaces have been handled by isSubtype
    Preconditions.checkArgument(leftType.isStructuralInterface());
    // classes that have a explicitly declared relationship
    // should be handled by isSubType method.
    if (rightType.explicitlyImplOrExtInterface(leftType)) {
      return true;
    }
    MatchStatus result = implicitImplCache.checkCache(rightType, leftType);
    if (result != null) {
      return result.subtypeValue();
    }
    // the following case should not type check
    // var1 : IArrayLike<string> = var2 : IArrayLike<number>
    // the template types mismatch
    if (leftType.hasAnyTemplateTypes()) {
      implicitImplCache.updateCache(leftType, rightType, MatchStatus.NOT_MATCH);
      return false;
    }
    Map<String, JSType> interfacePropList = getPropertyTypeMap(leftType);
    Map<String, JSType> functionPropList = getPropertyTypeMap(rightType);
    for (String propName : interfacePropList.keySet()) {
      JSType typeInInterface = interfacePropList.get(propName);
      JSType typeInFunction = functionPropList.get(propName);
      if (typeInFunction == null
          || !typeInFunction.isSubtype(typeInInterface, implicitImplCache)) {
        implicitImplCache.updateCache(leftType, rightType, MatchStatus.NOT_MATCH);
        return false;
      }
    }
    // if all properties required by the interface
    // have compatible property in the function,
    // consider that the function implements the interface
    implicitImplCache.updateCache(leftType, rightType, MatchStatus.MATCH);
    return true;
  }

  /**
   * get the map of properties to types covered in an object type
   * @param type object type
   * @return a Map that maps the property's name to the property's type
   */
  protected static Map<String, JSType> getPropertyTypeMap(ObjectType type) {
    if (type == null) { return ImmutableMap.of(); }
    return type.getPropertyTypeMap();
  }

  /**
   * Determines if the specified type is exempt from standard invariant
   * templatized typing rules.
   */
  static boolean isExemptFromTemplateTypeInvariance(JSType type) {
    ObjectType objType = type.toObjectType();
    return objType == null ||
        "Array".equals(objType.getReferenceName()) ||
        "Object".equals(objType.getReferenceName());
  }

  /**
   * Visit this type with the given visitor.
   * @see com.google.javascript.rhino.jstype.Visitor
   * @return the value returned by the visitor
   */
  public abstract <T> T visit(Visitor<T> visitor);

  /**
   * Visit the types with the given visitor.
   * @see com.google.javascript.rhino.jstype.RelationshipVisitor
   * @return the value returned by the visitor
   */
  abstract <T> T visit(RelationshipVisitor<T> visitor, JSType that);

  /**
   * Resolve this type in the given scope.
   *
   * The returned value must be equal to {@code this}, as defined by
   * {@link #isEquivalentTo}. It may or may not be the same object. This method
   * may modify the internal state of {@code this}, as long as it does
   * so in a way that preserves Object equality.
   *
   * For efficiency, we should only resolve a type once per compilation job.
   * For incremental compilations, one compilation job may need the
   * artifacts from a previous generation, so we will eventually need
   * a generational flag instead of a boolean one.
   */
  public final JSType resolve(ErrorReporter t, StaticTypedScope<JSType> scope) {
    if (resolved) {
      // TODO(nicksantos): Check to see if resolve() looped back on itself.
      // Preconditions.checkNotNull(resolveResult);
      if (resolveResult == null) {
        return registry.getNativeType(JSTypeNative.UNKNOWN_TYPE);
      }
      return resolveResult;
    }
    resolved = true;
    resolveResult = resolveInternal(t, scope);
    resolveResult.setResolvedTypeInternal(resolveResult);
    return resolveResult;
  }

  /**
   * @see #resolve
   */
  abstract JSType resolveInternal(ErrorReporter t, StaticTypedScope<JSType> scope);

  void setResolvedTypeInternal(JSType type) {
    resolveResult = type;
    resolved = true;
  }

  /** Whether the type has been resolved. */
  public final boolean isResolved() {
    return resolved;
  }

  /**
   * A null-safe resolve.
   * @see #resolve
   */
  static final JSType safeResolve(
      JSType type, ErrorReporter t, StaticTypedScope<JSType> scope) {
    return type == null ? null : type.resolve(t, scope);
  }

  /**
   * Certain types have constraints on them at resolution-time.
   * For example, a type in an {@code @extends} annotation must be an
   * object. Clients should inject a validator that emits a warning
   * if the type does not validate, and return false.
   */
  public boolean setValidator(Predicate<JSType> validator) {
    return validator.apply(this);
  }

  /**
   * a data structure that represents a pair of types
   */
  public static class TypePair {
    public final JSType typeA;
    public final JSType typeB;

    public TypePair(JSType typeA, JSType typeB) {
      this.typeA = typeA;
      this.typeB = typeB;
    }
  }

  /**
   * A string representation of this type, suitable for printing
   * in warnings.
   */
  @Override
  public String toString() {
    return toStringHelper(false);
  }

  /**
   * A hash code function for diagnosing complicated issues
   * around type-identity.
   */
  public String toDebugHashCodeString() {
    return "{" + hashCode() + "}";
  }

  /**
   * A string representation of this type, suitable for printing
   * in type annotations at code generation time.
   */
  public final String toAnnotationString() {
    return toStringHelper(true);
  }

  /**
   * @param forAnnotations Whether this is for use in code generator
   *     annotations. Otherwise, it's for warnings.
   */
  abstract String toStringHelper(boolean forAnnotations);

  /**
   * Modify this type so that it matches the specified type.
   *
   * This is useful for reverse type-inference, where we want to
   * infer that an object literal matches its constraint (much like
   * how the java compiler does reverse-inference to figure out generics).
   * @param constraint
   */
  public void matchConstraint(JSType constraint) {}

  @Override
  public boolean isSubtypeOf(TypeI other) {
    return isSubtype((JSType) other);
  }

  @Override
  public boolean isBottom() {
    return isNoType() || isNoResolvedType() || isNoObjectType();
  }

  @Override
  public ObjectType toMaybeObjectType() {
    return toObjectType();
  }

  /**
   * describe the status of checking that a function
   * implicitly implements an interface.
   *
   * it also be used to describe the status of checking
   * that a record type structurally matches another
   * record type
   *
   * A function implicitly implements an interface if
   * the function does not use @implements to declare
   * that it implements the interface, but its class
   * structure complies with the protocol defined
   * by the interface
   */
  protected static enum MatchStatus {
    /**
     * indicate that a function implicitly
     * implements an interface (i.e., the function
     * structurally complies with the protocol
     * defined in interface)
     *
     * or a record type matches another record type
     */
    MATCH(true),
    /**
     * indicate that a function does not implicitly
     * implements an interface (i.e., the function
     * does not structurally comply with the protocol
     * defined in interface)
     *
     * or a record type does not match another
     * record type
     */
    NOT_MATCH(false),
    /**
     * indicate that the interface and function
     * relationship is under processing
     */
    PROCESSING(true);

    MatchStatus(boolean isSubtype) {
      this.isSubtype = isSubtype;
    }

    private boolean isSubtype;
    boolean subtypeValue() {
      return this.isSubtype;
    }
  }

  /**
   * base cache data structure
   */
  protected abstract static class MatchCache {
    protected Table<JSType, JSType, MatchStatus> matchCache;
    protected boolean isStructuralTyping;

    protected MatchCache(boolean isStructuralTyping) {
      this.isStructuralTyping = isStructuralTyping;
      this.matchCache = null;
    }

    protected boolean isStructuralTyping() {
      return isStructuralTyping;
    }

    void updateCache(JSType leftType,
        JSType rightType, MatchStatus isMatch) {
      this.matchCache.put(leftType, rightType, isMatch);
    }

    MatchStatus checkCache(JSType rightType, JSType leftType) {
      if (this.matchCache == null) {
        this.matchCache = HashBasedTable.create();
      }
      // check the cache
      if (this.matchCache.contains(leftType, rightType)) {
        return this.matchCache.get(leftType, rightType);
      } else {
        this.updateCache(leftType, rightType, MatchStatus.PROCESSING);
        return null;
      }
    }
  }

  /**
   * cache used by equivalence check logic
   */
  protected static class EqCache extends MatchCache {
    static EqCache create() {
      return new EqCache(true);
    }

    static EqCache createWithoutStructuralTyping() {
      return new EqCache(false);
    }

    private EqCache(boolean isStructuralTyping) {
      super(isStructuralTyping);
    }
  }

  /**
   * cache used by check sub-type logic
   */
  protected static class ImplCache extends MatchCache {
    static ImplCache create() {
      return new ImplCache(true);
    }

    static ImplCache createWithoutStructuralTyping() {
      return new ImplCache(false);
    }

    private ImplCache(boolean isStructuralTyping) {
      super(isStructuralTyping);
    }
  }
}
