/*
 * Copyright 2010 Google Inc.
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


package com.google.template.soy.shared;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An interface for a one-to-one string mapping function used to rename CSS selectors.
 * CSS renaming can be used for minimization, obfuscation, normalization, etc.
 *
 */
@ParametersAreNonnullByDefault
public interface SoyCssRenamingMap extends SoyIdRenamingMap {

  /** A renaming map that has no entries. */
  static final SoyCssRenamingMap EMPTY = new SoyCssRenamingMap() {
    @Override public String get(String key) {
      return null;
    }
  };

  /** A renaming map that maps every name to itself. */
  public static final SoyCssRenamingMap IDENTITY = new SoyCssRenamingMap() {

    @Override
    public String get(String key) {
      return key;
    }
  };
}
