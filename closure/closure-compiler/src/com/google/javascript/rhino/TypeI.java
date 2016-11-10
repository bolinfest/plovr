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
 *   Ben Lickly
 *   Dimitris Vardoulakis
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

package com.google.javascript.rhino;

/**
 * A common interface for types in the old type system and the new type system,
 * so that the other passes need not know which type system they are using.
 *
 * @author blickly@google.com (Ben Lickly)
 * @author dimvar@google.com (Dimitris Vardoulakis)
 */
public interface TypeI {

  boolean isBottom();

  boolean isTop();

  boolean isTypeVariable();

  boolean isUnresolved();

  // Hacky method to abstract away corner case handling of the way OTI
  // represents unresolved types.
  boolean isUnresolvedOrResolvedUnknown();

  boolean isConstructor();

  boolean isEquivalentTo(TypeI type);

  boolean isFunctionType();

  boolean isInterface();

  boolean isSubtypeOf(TypeI type);

  boolean containsArray();

  boolean isUnknownType();

  boolean isSomeUnknownType();

  boolean isUnionType();

  boolean isNullable();

  boolean isVoidable();

  boolean isNullType();

  boolean isVoidType();

  boolean isPrototypeObject();

  boolean isInstanceofObject();

  ObjectTypeI autoboxAndGetObject();

  JSDocInfo getJSDocInfo();

  /**
   * If this is a union type, returns a union type that does not include
   * the null or undefined type.
   */
  TypeI restrictByNotNullOrUndefined();

  /**
   * Downcasts this to a FunctionTypeI, or returns null if this is not
   * a function.
   */
  FunctionTypeI toMaybeFunctionType();

  /**
   * If this type is a single object, downcast it to ObjectTypeI.
   * If it is a non-object or a union of objects, return null.
   */
  ObjectTypeI toMaybeObjectType();

  /**
   * If this type is a union type, returns a list of its members. Otherwise
   * returns null.
   */
  Iterable<? extends TypeI> getUnionMembers();

  String getDisplayName();
}
