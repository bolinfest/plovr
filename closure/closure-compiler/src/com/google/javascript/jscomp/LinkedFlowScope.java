/*
 * Copyright 2008 The Closure Compiler Authors.
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

package com.google.javascript.jscomp;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.javascript.jscomp.type.FlowScope;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.SimpleSlot;
import com.google.javascript.rhino.jstype.StaticTypedScope;
import com.google.javascript.rhino.jstype.StaticTypedSlot;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A flow scope that tries to store as little symbol information as possible,
 * instead delegating to its parents. Optimized for low memory use.
 *
 * @author nicksantos@google.com (Nick Santos)
 */
class LinkedFlowScope implements FlowScope {
  // The closest flow scope cache.
  private final FlatFlowScopeCache cache;

  // The parent flow scope.
  private final LinkedFlowScope parent;

  // The distance between this flow scope and the closest flat flow scope.
  private int depth;

  static final int MAX_DEPTH = 250;

  // A FlatFlowScopeCache equivalent to this scope.
  private FlatFlowScopeCache flattened;

  // Flow scopes assume that all their ancestors are immutable.
  // So once a child scope is created, this flow scope may not be modified.
  private boolean frozen = false;

  // The last slot defined in this flow instruction, and the head of the
  // linked list of slots.
  private LinkedFlowSlot lastSlot;

  private LinkedFlowScope(FlatFlowScopeCache cache,
      LinkedFlowScope directParent) {
    this.cache = cache;
    if (directParent == null) {
      this.lastSlot = null;
      this.depth = 0;
      this.parent = cache.linkedEquivalent;
    } else {
      this.lastSlot = directParent.lastSlot;
      this.depth = directParent.depth + 1;
      this.parent = directParent;
    }
  }

  LinkedFlowScope(FlatFlowScopeCache cache) {
    this(cache, null);
  }

  LinkedFlowScope(LinkedFlowScope directParent) {
    this(directParent.cache, directParent);
  }

  /** Gets the function scope for this flow scope. */
  private TypedScope getFunctionScope() {
    return cache.functionScope;
  }

  /** Whether this flows from a bottom scope. */
  private boolean flowsFromBottom() {
    return getFunctionScope().isBottom();
  }

  /**
   * Creates an entry lattice for the flow.
   */
  public static LinkedFlowScope createEntryLattice(TypedScope scope) {
    return new LinkedFlowScope(new FlatFlowScopeCache(scope));
  }

  @Override
  public void inferSlotType(String symbol, JSType type) {
    Preconditions.checkState(!frozen);
    lastSlot = new LinkedFlowSlot(symbol, type, lastSlot);
    depth++;
    cache.dirtySymbols.add(symbol);
  }

  @Override
  public void inferQualifiedSlot(Node node, String symbol, JSType bottomType,
      JSType inferredType, boolean declared) {
    TypedScope functionScope = getFunctionScope();
    if (functionScope.isLocal()) {
      TypedVar v  = functionScope.getVar(symbol);
      if (v == null && !functionScope.isBottom()) {
        v = functionScope.declare(symbol, node, bottomType, null, !declared);
      }

      if (v != null && !v.isTypeInferred()) {
        JSType declaredType = v.getType();
        // Use the inferred type over the declared type only if the
        // inferred type is a strict subtype of the declared type.
        if (declaredType != null && inferredType.isSubtype(declaredType)
            && !declaredType.isSubtype(inferredType)
            && !inferredType.isEquivalentTo(declaredType)) {
          inferSlotType(symbol, inferredType);
        }
      } else {
        inferSlotType(symbol, inferredType);
      }
    }
  }

  @Override
  public JSType getTypeOfThis() {
    return cache.functionScope.getTypeOfThis();
  }

  @Override
  public Node getRootNode() {
    return getFunctionScope().getRootNode();
  }

  @Override
  public StaticTypedScope<JSType> getParentScope() {
    return getFunctionScope().getParentScope();
  }

  /**
   * Get the slot for the given symbol.
   */
  @Override
  public StaticTypedSlot<JSType> getSlot(String name) {
    if (cache.dirtySymbols.contains(name)) {
      for (LinkedFlowSlot slot = lastSlot;
           slot != null; slot = slot.parent) {
        if (slot.getName().equals(name)) {
          return slot;
        }
      }
    }
    return cache.getSlot(name);
  }

  @Override
  public StaticTypedSlot<JSType> getOwnSlot(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public FlowScope createChildFlowScope() {
    frozen = true;

    if (depth > MAX_DEPTH) {
      if (flattened == null) {
        flattened = new FlatFlowScopeCache(this);
      }
      return new LinkedFlowScope(flattened);
    }

    return new LinkedFlowScope(this);
  }

  /**
   * Iterate through all the linked flow scopes before this one.
   * If there's one and only one slot defined between this scope
   * and the blind scope, return it.
   */
  @Override
  public StaticTypedSlot<JSType> findUniqueRefinedSlot(FlowScope blindScope) {
    StaticTypedSlot<JSType> result = null;

    for (LinkedFlowScope currentScope = this;
         currentScope != blindScope;
         currentScope = currentScope.parent) {
      for (LinkedFlowSlot currentSlot = currentScope.lastSlot;
           currentSlot != null &&
           (currentScope.parent == null ||
            currentScope.parent.lastSlot != currentSlot);
           currentSlot = currentSlot.parent) {
        if (result == null) {
          result = currentSlot;
        } else if (!currentSlot.getName().equals(result.getName())) {
          return null;
        }
      }
    }

    return result;
  }

  /**
   * Look through the given scope, and try to find slots where it doesn't
   * have enough type information. Then fill in that type information
   * with stuff that we've inferred in the local flow.
   */
  @Override
  public void completeScope(StaticTypedScope<JSType> staticScope) {
    TypedScope scope = (TypedScope) staticScope;
    for (Iterator<TypedVar> it = scope.getVars(); it.hasNext();) {
      TypedVar var = it.next();
      if (var.isTypeInferred()) {
        JSType type = var.getType();
        if (type == null || type.isUnknownType()) {
          JSType flowType = getSlot(var.getName()).getType();
          var.setType(flowType);
        }
      }
    }
  }

  /**
   * Remove flow scopes that add nothing to the flow.
   */
  // NOTE(nicksantos): This function breaks findUniqueRefinedSlot, because
  // findUniqueRefinedSlot assumes that this scope is a direct descendant
  // of blindScope. This is not necessarily true if this scope has been
  // optimize()d and blindScope has not. This should be fixed. For now,
  // we only use optimize() where we know that we won't have to do
  // a findUniqueRefinedSlot on it.
  @Override
  public LinkedFlowScope optimize() {
    LinkedFlowScope current;
    for (current = this;
         current.parent != null &&
             current.lastSlot == current.parent.lastSlot;
         current = current.parent) {}
    return current;
  }

  /** Join the two FlowScopes. */
  static class FlowScopeJoinOp extends JoinOp.BinaryJoinOp<FlowScope> {
    @SuppressWarnings("unchecked")
    @Override
    public FlowScope apply(FlowScope a, FlowScope b) {
      // To join the two scopes, we have to
      LinkedFlowScope linkedA = (LinkedFlowScope) a;
      LinkedFlowScope linkedB = (LinkedFlowScope) b;
      linkedA.frozen = true;
      linkedB.frozen = true;
      if (linkedA.optimize() == linkedB.optimize()) {
        return linkedA.createChildFlowScope();
      }
      return new LinkedFlowScope(new FlatFlowScopeCache(linkedA, linkedB));
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof LinkedFlowScope) {
      LinkedFlowScope that = (LinkedFlowScope) other;
      if (this.optimize() == that.optimize()) {
        return true;
      }

      // If two flow scopes are in the same function, then they could have
      // two possible function scopes: the real one and the BOTTOM scope.
      // If they have different function scopes, we *should* iterate through all
      // the variables in each scope and compare. However, 99.9% of the time,
      // they're not equal. And the other .1% of the time, we can pretend
      // they're equal--this just means that data flow analysis will have
      // to propagate the entry lattice a little bit further than it
      // really needs to. Everything will still come out ok.
      if (this.getFunctionScope() != that.getFunctionScope()) {
        return false;
      }

      if (cache == that.cache) {
        // If the two flow scopes have the same cache, then we can check
        // equality a lot faster: by just looking at the "dirty" elements
        // in the cache, and comparing them in both scopes.
        for (String name : cache.dirtySymbols) {
          if (diffSlots(getSlot(name), that.getSlot(name))) {
            return false;
          }
        }

        return true;
      }

      Map<String, StaticTypedSlot<JSType>> myFlowSlots = allFlowSlots();
      Map<String, StaticTypedSlot<JSType>> otherFlowSlots = that.allFlowSlots();

      for (StaticTypedSlot<JSType> slot : myFlowSlots.values()) {
        if (diffSlots(slot, otherFlowSlots.get(slot.getName()))) {
          return false;
        }
        otherFlowSlots.remove(slot.getName());
      }
      for (StaticTypedSlot<JSType> slot : otherFlowSlots.values()) {
        if (diffSlots(slot, myFlowSlots.get(slot.getName()))) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Determines whether two slots are meaningfully different for the
   * purposes of data flow analysis.
   */
  private static boolean diffSlots(StaticTypedSlot<JSType> slotA,
                                   StaticTypedSlot<JSType> slotB) {
    boolean aIsNull = slotA == null || slotA.getType() == null;
    boolean bIsNull = slotB == null || slotB.getType() == null;
    if (aIsNull && bIsNull) {
      return false;
    } else if (aIsNull ^ bIsNull) {
      return true;
    }

    // Both slots and types must be non-null.
    return slotA.getType().differsFrom(slotB.getType());
  }

  /**
   * Gets all the symbols that have been defined before this point
   * in the current flow. Does not return slots that have not changed during
   * the flow.
   *
   * For example, consider the code:
   * <code>
   * var x = 3;
   * function f() {
   *   var y = 5;
   *   y = 6; // FLOW POINT
   *   var z = y;
   *   return z;
   * }
   * </code>
   * A FlowScope at FLOW POINT will return a slot for y, but not
   * a slot for x or z.
   */
  private Map<String, StaticTypedSlot<JSType>> allFlowSlots() {
    Map<String, StaticTypedSlot<JSType>> slots = new LinkedHashMap<>();
    for (LinkedFlowSlot slot = lastSlot;
         slot != null; slot = slot.parent) {
      if (!slots.containsKey(slot.getName())) {
        slots.put(slot.getName(), slot);
      }
    }

    for (Map.Entry<String, StaticTypedSlot<JSType>> symbolEntry : cache.symbols.entrySet()) {
      if (!slots.containsKey(symbolEntry.getKey())) {
        slots.put(symbolEntry.getKey(), symbolEntry.getValue());
      }
    }

    return slots;
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  /**
   * A static slot that can be used in a linked list.
   */
  private static class LinkedFlowSlot extends SimpleSlot {
    final LinkedFlowSlot parent;

    LinkedFlowSlot(String name, JSType type, LinkedFlowSlot parent) {
      super(name, type, true);
      this.parent = parent;
    }
  }

  /**
   * A map that tries to cache as much symbol table information
   * as possible in a map. Optimized for fast lookup.
   */
  private static class FlatFlowScopeCache {
    // The TypedScope for the entire function or for the global scope.
    private final TypedScope functionScope;

    // The linked flow scope that this cache represents.
    private final LinkedFlowScope linkedEquivalent;

    // All the symbols defined before this point in the local flow.
    // May not include lazily declared qualified names.
    private Map<String, StaticTypedSlot<JSType>> symbols = new LinkedHashMap<>();

    // Used to help make lookup faster for LinkedFlowScopes by recording
    // symbols that may be redefined "soon", for an arbitrary definition
    // of "soon". ;)
    //
    // More rigorously, if a symbol is redefined in a LinkedFlowScope,
    // and this is the closest FlatFlowScopeCache, then that symbol is marked
    // "dirty". In this way, we don't waste time looking in the LinkedFlowScope
    // list for symbols that aren't defined anywhere nearby.
    final Set<String> dirtySymbols = new LinkedHashSet<>();

    // The cache at the bottom of the lattice.
    FlatFlowScopeCache(TypedScope functionScope) {
      this.functionScope = functionScope;
      symbols = ImmutableMap.of();
      linkedEquivalent = null;
    }

    // A cache in the middle of a long scope chain.
    FlatFlowScopeCache(LinkedFlowScope directParent) {
      FlatFlowScopeCache cache = directParent.cache;

      functionScope = cache.functionScope;
      symbols = directParent.allFlowSlots();
      linkedEquivalent = directParent;
    }

    // A cache at the join of two scope chains.
    FlatFlowScopeCache(LinkedFlowScope joinedScopeA,
        LinkedFlowScope joinedScopeB) {
      linkedEquivalent = null;

      // Always prefer the "real" function scope to the faked-out
      // bottom scope.
      functionScope = joinedScopeA.flowsFromBottom() ?
          joinedScopeB.getFunctionScope() : joinedScopeA.getFunctionScope();

      Map<String, StaticTypedSlot<JSType>> slotsA = joinedScopeA.allFlowSlots();
      Map<String, StaticTypedSlot<JSType>> slotsB = joinedScopeB.allFlowSlots();

      symbols = slotsA;

      // There are 5 different join cases:
      // 1) The type is declared in joinedScopeA, not in joinedScopeB,
      //    and not in functionScope. Just use the one in A.
      // 2) The type is declared in joinedScopeB, not in joinedScopeA,
      //    and not in functionScope. Just use the one in B.
      // 3) The type is declared in functionScope and joinedScopeA, but
      //    not in joinedScopeB. Join the two types.
      // 4) The type is declared in functionScope and joinedScopeB, but
      //    not in joinedScopeA. Join the two types.
      // 5) The type is declared in joinedScopeA and joinedScopeB. Join
      //    the two types.

      for (String name : Iterables.concat(symbols.keySet(), slotsB.keySet())) {
        StaticTypedSlot<JSType> slotA = slotsA.get(name);
        StaticTypedSlot<JSType> slotB = slotsB.get(name);

        JSType joinedType = null;
        if (slotB == null || slotB.getType() == null) {
          StaticTypedSlot<JSType> fnSlot
              = joinedScopeB.getFunctionScope().getSlot(name);
          JSType fnSlotType = fnSlot == null ? null : fnSlot.getType();
          if (fnSlotType == null) {
            // Case #1 -- already inserted.
          } else {
            // Case #3
            joinedType = slotA.getType().getLeastSupertype(fnSlotType);
          }
        } else if (slotA == null || slotA.getType() == null) {
          StaticTypedSlot<JSType> fnSlot
              = joinedScopeA.getFunctionScope().getSlot(name);
          JSType fnSlotType = fnSlot == null ? null : fnSlot.getType();
          if (fnSlotType == null) {
            // Case #2
            symbols.put(name, slotB);
          } else {
            // Case #4
            joinedType = slotB.getType().getLeastSupertype(fnSlotType);
          }
        } else {
          // Case #5
          joinedType =
              slotA.getType().getLeastSupertype(slotB.getType());
        }

        if (joinedType != null) {
          symbols.put(name, new SimpleSlot(name, joinedType, true));
        }
      }
    }

    /**
     * Get the slot for the given symbol.
     */
    public StaticTypedSlot<JSType> getSlot(String name) {
      if (symbols.containsKey(name)) {
        return symbols.get(name);
      } else {
        return functionScope.getSlot(name);
      }
    }
  }
}
