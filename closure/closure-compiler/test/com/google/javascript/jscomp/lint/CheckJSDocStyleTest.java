/*
 * Copyright 2015 The Closure Compiler Authors.
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
package com.google.javascript.jscomp.lint;

import static com.google.javascript.jscomp.lint.CheckJSDocStyle.INCORRECT_PARAM_NAME;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.MIXED_PARAM_JSDOC_STYLES;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.MUST_BE_PRIVATE;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.OPTIONAL_PARAM_NOT_MARKED_OPTIONAL;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.OPTIONAL_TYPE_NOT_USING_OPTIONAL_NAME;
import static com.google.javascript.jscomp.lint.CheckJSDocStyle.WRONG_NUMBER_OF_PARAMS;

import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions.LanguageMode;
import com.google.javascript.jscomp.CompilerPass;
import com.google.javascript.jscomp.CompilerTestCase;

/**
 * Test case for {@link CheckJSDocStyle}.
 */
public final class CheckJSDocStyleTest extends CompilerTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    setAcceptedLanguage(LanguageMode.ECMASCRIPT6);
  }

  @Override
  public CompilerPass getProcessor(Compiler compiler) {
    return new CheckJSDocStyle(compiler);
  }

  public void testMissingParam_noWarning() {
    testSame(LINE_JOINER.join(
        "/**",
        " * @param {string} x",
        " * @param {string} y",
        " */",
        "function f(x, y) {}"));

    testSame(LINE_JOINER.join(
        "/** @override */",
        "Foo.bar = function(x, y) {}"));

    testSame(LINE_JOINER.join(
        "/**",
        " * @param {string=} x",
        " */",
        "function f(x = 1) {}"));

    testSame(LINE_JOINER.join(
        "/**",
        " * @param {...string} args",
        " */",
        "function f(...args) {}"));

    testSame("function f(/** string */ inlineArg) {}");
    testSame("/** @export */ function f(/** string */ inlineArg) {}");

    testSame("class Foo { constructor(/** string */ inlineArg) {} }");
    testSame("class Foo { method(/** string */ inlineArg) {} }");

    testSame("class Foo { /** @export */ constructor(/** string */ inlineArg) {} }");
    testSame("class Foo { /** @export */ method(/** string */ inlineArg) {} }");
  }

  public void testMissingParam() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} x",
            // No @param for y.
            " */",
            "function f(x, y) {}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} x",
            " */",
            "function f(x = 1) {}"),
        OPTIONAL_PARAM_NOT_MARKED_OPTIONAL);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} x",
            // No @param for y.
            " */",
            "function f(x, y = 1) {}"),
        WRONG_NUMBER_OF_PARAMS);
  }

  public void testMissingParamWithDestructuringPattern() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f(namedParam, {destructuring:pattern}) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f({destructuring:pattern}, namedParam) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f(namedParam, [pattern]) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f([pattern], namedParam) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);
  }

  public void testMissingParamWithDestructuringPatternWithDefault() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f(namedParam, {destructuring:pattern} = defaultValue) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);

    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {string} namedParam",
            " * @return {void}",
            " */",
            "function f(namedParam, [pattern] = defaultValue) {",
            "}"),
        WRONG_NUMBER_OF_PARAMS);
  }

  public void testMissingPrivate() {
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @return {number}",
            " */",
            "X.prototype.foo_ = function() { return 0; }"),
        MUST_BE_PRIVATE);

    testSame(
        LINE_JOINER.join(
            "/**",
            " * @return {number}",
            " * @private",
            " */",
            "X.prototype.foo_ = function() { return 0; }"));
  }

  public void testOptionalArgs() {
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @param {number=} n",
            " */",
            "function f(n) {}"),
        OPTIONAL_TYPE_NOT_USING_OPTIONAL_NAME);

    testSame(
        LINE_JOINER.join(
            "/**",
            " * @param {number} opt_n",
            " */",
            "function f(opt_n) {}"),
        OPTIONAL_PARAM_NOT_MARKED_OPTIONAL);

    testSame(LINE_JOINER.join(
        "/**",
        " * @param {number=} opt_n",
        " */",
        "function f(opt_n) {}"));
  }

  public void testParamsOutOfOrder() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {?} second",
            " * @param {?} first",
            " */",
            "function f(first, second) {}"),
        INCORRECT_PARAM_NAME);
  }

  public void testMixedStyles() {
    testWarning(
        LINE_JOINER.join(
            "/**",
            " * @param {?} first",
            " * @param {string} second",
            " */",
            "function f(first, /** string */ second) {}"),
        MIXED_PARAM_JSDOC_STYLES);
  }

  public void testDestructuring() {
    testSame(
        LINE_JOINER.join(
            "/**",
            " * @param {{x: number, y: number}} point",
            " */",
            "function getDistanceFromZero({x, y}) {}"));

    testSame("function getDistanceFromZero(/** {x: number, y: number} */ {x, y}) {}");
  }
}
