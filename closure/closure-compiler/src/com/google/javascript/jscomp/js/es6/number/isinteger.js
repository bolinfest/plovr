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

'require es6/number/isfinite';
'require util/polyfill';

$jscomp.polyfill('Number.isInteger', function(orig) {
  if (orig) return orig;

  /**
   * Returns whether the given argument is an integer.
   *
   * <p>Polyfills the static function Number.isInteger().
   *
   * @param {number} x Any value.
   * @return {boolean} True if x is an integer.
   */
  var polyfill = function(x) {
    if (!Number.isFinite(x)) return false;
    return x === Math.floor(x);
  };

  return polyfill;
}, 'es6-impl', 'es3');
