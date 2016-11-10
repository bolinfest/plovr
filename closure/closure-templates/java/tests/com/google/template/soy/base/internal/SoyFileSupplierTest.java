/*
 * Copyright 2016 Google Inc.
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

package com.google.template.soy.base.internal;

import com.google.common.truth.Truth;
import com.google.template.soy.error.SoyCompilationException;

import junit.framework.TestCase;

import java.net.URL;

public final class SoyFileSupplierTest extends TestCase {

  public void testPercentEncodingInFileUrl() throws Exception {
    URL url = new URL("file:///foo/bar%20baz");
    SoyFileSupplier sfs = SoyFileSupplier.Factory.create(url, SoyFileKind.SRC, "/test/path");
    Truth.assertThat(sfs.getFilePath()).isEqualTo("/foo/bar baz");
  }

  public void testMalformedFileURL() throws Exception {
    URL url = new URL("file:///foo/bar|baz");
    boolean failedWithSoyCompilationException = false;
    try {
      SoyFileSupplier.Factory.create(url, SoyFileKind.SRC, "/test/path");
    } catch (SoyCompilationException ex) {
      failedWithSoyCompilationException = true;
    }
    Truth.assertWithMessage(url + " should not be treated as a valid URL")
        .that(failedWithSoyCompilationException)
        .isTrue();
  }

}
