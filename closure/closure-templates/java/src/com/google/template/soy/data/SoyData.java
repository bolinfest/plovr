/*
 * Copyright 2008 Google Inc.
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

package com.google.template.soy.data;

import com.google.template.soy.data.restricted.BooleanData;
import com.google.template.soy.data.restricted.FloatData;
import com.google.template.soy.data.restricted.IntegerData;
import com.google.template.soy.data.restricted.NullData;
import com.google.template.soy.data.restricted.StringData;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * Abstract base class for all nodes in a Soy data tree.
 *
 * <p> Important: Even though this class is not marked 'final', do not extend this class.
 *
 */
public abstract class SoyData extends SoyAbstractValue {


  /**
   * Creates deprecated SoyData objects from standard Java data structures.
   *
   * @param obj The existing object or data structure to convert.
   * @return A SoyData object or tree that corresponds to the given object.
   * @throws SoyDataException If the given object cannot be converted to SoyData.
   * @deprecated It's best to pass whatever object you have directly to the Soy templates you're
   *     using -- Soy understands primitives, lists, and maps natively, and if you install runtime
   *     support you can also pass protocol buffers. If you're interacting directly with the Soy
   *     runtime and need SoyValue objects, use SoyValueHelper instead.
   */
  @Deprecated
  public static SoyData createFromExistingData(Object obj) {

    // Important: This is frozen for backwards compatibility, For future changes (pun not intended),
    // use SoyValueHelper, which works with the new interfaces SoyValue and SoyValueProvider.

    if (obj == null) {
      return NullData.INSTANCE;
    } else if (obj instanceof SoyData) {
      return (SoyData) obj;
    } else if (obj instanceof String) {
      return StringData.forValue((String) obj);
    } else if (obj instanceof Boolean) {
      return BooleanData.forValue((Boolean) obj);
    } else if (obj instanceof Integer) {
      return IntegerData.forValue((Integer) obj);
    } else if (obj instanceof Long) {
      return IntegerData.forValue((Long) obj);
    } else if (obj instanceof Map<?, ?>) {
      @SuppressWarnings("unchecked")
      Map<String, ?> objCast = (Map<String, ?>) obj;
      return new SoyMapData(objCast);
    } else if (obj instanceof Iterable<?>) {
      return new SoyListData((Iterable<?>) obj);
    } else if (obj instanceof Double) {
      return FloatData.forValue((Double) obj);
    } else if (obj instanceof Float) {
      // Automatically convert float to double.
      return FloatData.forValue((Float) obj);
    } else if (obj instanceof Future<?>) {
      // Note: In the old SoyData, we don't support late-resolution of Futures. We immediately
      // resolve the Future object here. For late-resolution, use SoyValueHelper.convert().
      try {
        return createFromExistingData(((Future<?>) obj).get());
      } catch (InterruptedException e) {
        throw new SoyDataException(
            "Encountered InterruptedException when resolving Future object.", e);
      } catch (ExecutionException e) {
        throw new SoyDataException(
            "Encountered ExecutionException when resolving Future object.", e);
      }
    } else {
      throw new SoyDataException(
          "Attempting to convert unrecognized object to Soy data (object type " +
          obj.getClass().getName() + ").");
    }
  }


  // -----------------------------------------------------------------------------------------------
  // Adapting old implementations to the new interface.


  // The old SoyData has method toBoolean(), while the new SoyValue has method coerceToBoolean().
  // This adapts old implementations to the new interface.
  @SuppressWarnings("deprecation")
  @Override public boolean coerceToBoolean() {
   return toBoolean();
  }


  /**
   * This was a required method in the old SoyData interface. For new data classes, please use
   * interface SoyValue, which has the method coerceToBoolean().
   *
   * Converts this data object into a boolean (e.g. when used in a boolean context). In other words,
   * this method tells whether this object is truthy.
   * @return The value of this data object if coerced into a boolean. I.e. true if this object is
   *     truthy, false if this object is falsy.
   */
  @Deprecated
  public abstract boolean toBoolean();


  // The old SoyData has method toString(), while the new SoyValue has method coerceToString().
  // This adapts old implementations to the new interface.
  @SuppressWarnings("deprecation")
  @Override public String coerceToString() {
    return toString();
  }


  /**
   * This was a required method in the old SoyData interface. For new data classes, please use
   * interface SoyValue, which has the method coerceToString().
   *
   * Converts this data object into a string (e.g. when used in a string context).
   * @return The value of this data object if coerced into a string.
   */
  // TODO: Maybe deprecate this method (even though it's a standard method).
  @Override public abstract String toString();

}
