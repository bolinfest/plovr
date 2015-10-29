/*
 * Copyright 2009 Google Inc.
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

package com.google.template.soy.tofu.restricted;

import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.restricted.StringData;


/**
 * Utilities for implementing {@link SoyTofuFunction}s.
 *
 * <p> Important: This class may only be used in implementing plugins (e.g. functions, directives).
 *
 * @deprecated Simply use {@code *Data.forValue()} directly.
 *
 */
@Deprecated
public class SoyTofuFunctionUtils {

  private SoyTofuFunctionUtils() {}

  /**
   * Converts a String value to a SoyData (specifically a StringData).
   * @param value The String value to convert.
   */
  public static SoyData toSoyData(String value) {
    return StringData.forValue(value);
  }

}
