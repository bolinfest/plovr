// Copyright 2014 The Closure Library Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview Unit tests for goog.html.SafeScript and its builders.
 */

goog.provide('goog.html.safeScriptTest');

goog.require('goog.html.SafeScript');
goog.require('goog.html.trustedtypes');
goog.require('goog.object');
goog.require('goog.string.Const');
goog.require('goog.testing.PropertyReplacer');
goog.require('goog.testing.jsunit');

goog.setTestOnly('goog.html.safeScriptTest');


var stubs = new goog.testing.PropertyReplacer();
var policy = goog.createTrustedTypesPolicy('closure_test');

function tearDown() {
  stubs.reset();
}


function testSafeScript() {
  var script = 'var string = \'hello\';';
  var safeScript =
      goog.html.SafeScript.fromConstant(goog.string.Const.from(script));
  var extracted = goog.html.SafeScript.unwrap(safeScript);
  assertEquals(script, extracted);
  assertEquals(script, safeScript.getTypedStringValue());
  assertEquals('SafeScript{' + script + '}', String(safeScript));

  // Interface marker is present.
  assertTrue(safeScript.implementsGoogStringTypedString);
}


/** @suppress {checkTypes} */
function testUnwrap() {
  var privateFieldName = 'privateDoNotAccessOrElseSafeScriptWrappedValue_';
  var markerFieldName = 'SAFE_SCRIPT_TYPE_MARKER_GOOG_HTML_SECURITY_PRIVATE_';
  var propNames = goog.object.getKeys(
      goog.html.SafeScript.fromConstant(goog.string.Const.from('')));
  assertContains(privateFieldName, propNames);
  assertContains(markerFieldName, propNames);
  var evil = {};
  evil[privateFieldName] = 'var string = \'evil\';';
  evil[markerFieldName] = {};

  var exception =
      assertThrows(function() { goog.html.SafeScript.unwrap(evil); });
  assertContains('expected object of type SafeScript', exception.message);
}


function testUnwrapTrustedScript() {
  var safeValue =
      goog.html.SafeScript.fromConstant(goog.string.Const.from('script'));
  var trustedValue = goog.html.SafeScript.unwrapTrustedScript(safeValue);
  assertEquals(safeValue.getTypedStringValue(), trustedValue);
  stubs.set(
      goog.html.trustedtypes, 'PRIVATE_DO_NOT_ACCESS_OR_ELSE_POLICY', policy);
  safeValue =
      goog.html.SafeScript.fromConstant(goog.string.Const.from('script'));
  trustedValue = goog.html.SafeScript.unwrapTrustedScript(safeValue);
  assertEquals(safeValue.getTypedStringValue(), trustedValue.toString());
  assertTrue(
      goog.global.TrustedScript ? trustedValue instanceof TrustedScript :
                                  goog.isString(trustedValue));
}


function testFromConstant_allowsEmptyString() {
  assertEquals(
      goog.html.SafeScript.EMPTY,
      goog.html.SafeScript.fromConstant(goog.string.Const.from('')));
}


function testEmpty() {
  assertEquals('', goog.html.SafeScript.unwrap(goog.html.SafeScript.EMPTY));
}


function testFromConstantAndArgs() {
  var script = goog.html.SafeScript.fromConstantAndArgs(
      goog.string.Const.from(
          'function(str, num, nul, json) { foo(str, num, nul, json); }'),
      'hello world', 42, null, {'foo': 'bar'});
  assertEquals(
      '(function(str, num, nul, json) { foo(str, num, nul, json); })' +
          '("hello world", 42, null, {"foo":"bar"});',
      goog.html.SafeScript.unwrap(script));
}


function testFromConstantAndArgs_escaping() {
  var script = goog.html.SafeScript.fromConstantAndArgs(
      goog.string.Const.from('function(str) { alert(str); }'),
      '</script</script');
  assertEquals(
      '(function(str) { alert(str); })' +
          '("\\x3c/script\\x3c/script");',
      goog.html.SafeScript.unwrap(script));
}


function testFromConstantAndArgs_eval() {
  var script = goog.html.SafeScript.fromConstantAndArgs(
      goog.string.Const.from('function(arg1, arg2) { return arg1 * arg2; }'),
      21, 2);
  var result = eval(goog.html.SafeScript.unwrap(script));
  assertEquals(42, result);
}


function testFromJson() {
  var json = goog.html.SafeScript.fromJson({'a': 1, 'b': testFromJson});
  assertEquals('{"a":1}', goog.html.SafeScript.unwrap(json));
}
