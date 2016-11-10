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

goog.module('jscomp.runtime_tests.polyfill_tests.reflect_getprototypeof_test');
goog.setTestOnly();

const testSuite = goog.require('goog.testing.testSuite');
const testing = goog.require('jscomp.runtime_tests.polyfill_tests.testing');
const userAgent = goog.require('goog.userAgent');

const noCheck = testing.noCheck;

testSuite({
  shouldRunTests() {
    // Not polyfilled to ES3
    return !userAgent.IE || userAgent.isVersionOrHigher(9);
  },

  testGetPrototypeOf() {
    assertEquals(Function.prototype, Reflect.getPrototypeOf(Function));
    assertEquals(Function.prototype, Reflect.getPrototypeOf(Object));
    assertEquals(Object.prototype, Reflect.getPrototypeOf({}));
    assertEquals(Object.prototype, Reflect.getPrototypeOf(Function.prototype));
    assertNull(Reflect.getPrototypeOf(noCheck(Object.prototype)));

    const obj = {};
    const sub = Object.create(obj);
    const subsub = Object.create(sub);
    assertEquals(obj, Reflect.getPrototypeOf(sub));
    assertEquals(sub, Reflect.getPrototypeOf(subsub));
  },
});
