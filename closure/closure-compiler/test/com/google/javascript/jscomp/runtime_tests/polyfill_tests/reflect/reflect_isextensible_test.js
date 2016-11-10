/*
 * Copyright 2016 The Closure Compiler Authors.
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

goog.module('jscomp.runtime_tests.polyfill_tests.reflect_isextensible_test');
goog.setTestOnly();

const testSuite = goog.require('goog.testing.testSuite');

const LEGACY = typeof Object.defineProperties !== 'function';

testSuite({
  testIsExtensible() {
    let obj = {};
    assertTrue(Reflect.isExtensible(obj));

    if (LEGACY) return; // Legacy browsers can go no farther.

    Object.preventExtensions(obj);
    assertFalse(Reflect.isExtensible(obj));

    obj = {};
    Object.seal(obj);
    assertFalse(Reflect.isExtensible(obj));

    obj = {};
    Object.freeze(obj);
    assertFalse(Reflect.isExtensible(obj));

    const sub = Object.create(obj);
    assertTrue(Reflect.isExtensible(sub));
  },
});
