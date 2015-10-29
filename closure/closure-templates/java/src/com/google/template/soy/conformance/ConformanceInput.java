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

package com.google.template.soy.conformance;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.template.soy.parsepasses.contextautoesc.SlicedRawTextNode;
import com.google.template.soy.soytree.SoyFileSetNode;

/**
 * All context needed to perform conformance checks.
 */
@AutoValue public abstract class ConformanceInput {

  public static ConformanceInput create(
      SoyFileSetNode soyTree, ImmutableList<SlicedRawTextNode> slicedRawTextNodes) {
    return new AutoValue_ConformanceInput(
        Preconditions.checkNotNull(soyTree),
        Preconditions.checkNotNull(slicedRawTextNodes));
  }

  public abstract SoyFileSetNode getSoyTree();

  public abstract ImmutableList<SlicedRawTextNode> getSlicedRawTextNodes();
}

