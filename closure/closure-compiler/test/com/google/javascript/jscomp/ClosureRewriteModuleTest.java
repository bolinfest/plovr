/*
 * Copyright 2014 The Closure Compiler Authors.
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
package com.google.javascript.jscomp;

import static com.google.javascript.jscomp.ClosureRewriteModule.INVALID_GET_ALIAS;
import static com.google.javascript.jscomp.ClosureRewriteModule.INVALID_GET_CALL_SCOPE;
import static com.google.javascript.jscomp.ClosureRewriteModule.INVALID_GET_IDENTIFIER;
import static com.google.javascript.jscomp.ClosureRewriteModule.INVALID_MODULE_IDENTIFIER;
import static com.google.javascript.jscomp.ClosureRewriteModule.INVALID_REQUIRE_IDENTIFIER;
import static com.google.javascript.jscomp.deps.JsFileLineParser.PARSE_ERROR;

/**
 * Unit tests for ClosureRewriteModule
 * @author johnlenz@google.com (John Lenz)
 */
public final class ClosureRewriteModuleTest extends Es6CompilerTestCase {

  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new ClosureRewriteModule(compiler);
  }

  @Override
  protected int getNumRepetitions() {
    return 1;
  }

  public void testBasic0() {
    testSame("");
    testSame("goog.provide('a');");
  }

  public void testBasic1() {
    test(
        "goog.module('a');",

        "goog.provide('a');" +
        "goog.scope(function(){});");
  }

  public void testBasic2() {
    test(
        "goog.module('ns.a');" +
        "var b = goog.require('ns.b');",

        "goog.provide('ns.a');" +
        "goog.require('ns.b');" +
        "goog.scope(function(){" +
        "  var b = ns.b;" +
        "});");
  }

  public void testDestructuring() {
    testEs6(
        LINE_JOINER.join(
            "goog.module('ns.a');",
            "var {foo, bar} = goog.require('ns.b');"),
        LINE_JOINER.join(
            "goog.provide('ns.a');",
            "goog.require('ns.b');",
            "goog.scope(function(){",
            "  var {foo, bar} = ns.b;",
            "});"));

    testEs6(
        LINE_JOINER.join(
            "goog.module('ns.a');",
            "const {foo, bar} = goog.require('ns.b');"),
        LINE_JOINER.join(
            "goog.provide('ns.a');",
            "goog.require('ns.b');",
            "goog.scope(function(){",
            "  const {foo, bar} = ns.b;",
            "});"));
  }

  public void testDeclareLegacyNamespace() {
    test(
        "goog.module('ns.a');"
        + "goog.module.declareLegacyNamespace();"
        + "var b = goog.require('ns.b');",

        "goog.provide('ns.a');"
        + "goog.require('ns.b');"
        + "goog.scope(function(){"
        + "  var b = ns.b;"
        + "});");
  }

  public void testBundle1() {
    test(
        LINE_JOINER.join(
            "goog.loadModule(function(exports) {",
            "  goog.module('ns.a');",
            "  var b = goog.require('ns.b');",
            "  return exports;",
            "});"),

        LINE_JOINER.join(
            "goog.provide('ns.a');",
            "goog.require('ns.b');",
            "goog.scope(function() {",
            "  var b = ns.b;",
            "});"));
  }

  public void testBundle2() {
    test(
        LINE_JOINER.join(
            "goog.loadModule(function(exports) {",
            "  goog.module('ns.a');",
            "  var b = goog.require('ns.b');",
            "  return exports;",
            "});",
            "goog.loadModule(function(exports) {",
            "  goog.module('ns.c');",
            "  var b = goog.require('ns.b');",
            "  return exports;",
            "});"),

        LINE_JOINER.join(
            "goog.provide('ns.a');",
            "goog.require('ns.b');",
            "goog.scope(function() {",
            "  var b = ns.b;",
            "});",
            "goog.provide('ns.c');",
            "goog.require('ns.b');",
            "goog.scope(function() {",
            "  var b = ns.b;",
            "});"));
  }

  public void testBundle3() {
    test(
        LINE_JOINER.join(
            "goog.loadModule(function(exports) {",
            "  'use strict';",
            "  goog.module('ns.a');",
            "  goog.module.declareLegacyNamespace();",
            "  var b = goog.require('ns.b');",
            "  return exports;",
            "});"),

        LINE_JOINER.join(
            "'use strict';",
            "goog.provide('ns.a');",
            "goog.require('ns.b');",
            "goog.scope(function() {",
            "  var b = ns.b;",
            "});"));
  }

  public void testBundle4() {
    test(
        LINE_JOINER.join(
            "goog.loadModule(function(exports) {",
            "  'use strict';",
            "  goog.module('ns.a');",
            "  var b = goog.require('goog.asserts');",
            "  return exports;",
            "});"),

        LINE_JOINER.join(
            "'use strict';",
            "goog.provide('ns.a');",
            "goog.require('goog.asserts');",
            "goog.scope(function() {",
            "  var b = goog.asserts;",
            "});"));
  }

  public void testBundle5() {
    test(
        LINE_JOINER.join(
            "goog.loadModule(function(exports) {",
            "  'use strict';",
            "  goog.module('xid');",
            "  goog.module.declareLegacyNamespace();",
            "  var asserts = goog.require('goog.asserts');",
            "  exports = function(id) {",
            "    return xid.internal_(id);",
            "  };",
            "  var xid = exports;",
            "  return exports;",
            "});"),

        LINE_JOINER.join(
            "goog.provide('xid');",
            "goog.require('goog.asserts');",
            "goog.scope(function() {",
            "  var asserts = goog.asserts;",
            "  /** @const */ xid = function(id) {",
            "    return xid_module.internal_(id);",
            "  };",
            "  var xid_module = xid;",
            "});"));
  }

  public void testAliasShadowsGlobal1() {
    // If locals shadow globals they need to be renamed.
    test(
        "goog.module('a'); var b = goog.require('b');",

        "goog.provide('a');"
        + "goog.require('b');"
        + "goog.scope(function(){var b_module = b});");
  }

  public void testAliasShadowsGlobal2() {
    // If locals shadow globals they need to be renamed.
    test(
        "goog.module('a'); goog.require('b'); var a,b,c;",

        "goog.provide('a');"
        + "goog.require('b');"
        + "goog.scope(function(){b;var a_module,b_module,c});");
  }

  public void testAliasShadowsGlobal3() {
    // If locals shadow globals they need to be renamed.
    test(
        "goog.module('a.c'); goog.require('b.c'); var a,b,c;",

        "goog.provide('a.c');"
        + "goog.require('b.c');"
        + "goog.scope(function(){b.c;var a_module,b_module,c});");
  }

  public void testThis() {
    // global "this" is retained.
    test(
        "goog.module('a'); this;",

        "goog.provide('a');"
        + "goog.scope(function(){this});");
  }

  public void testInvalidModule() {
    // The ES6 path turns on DependencyOptions.needsManagement() which leads to JsFileLineParser
    // execution that throws a different exception on some invalid goog.provide()s.
    testError("goog.module(a);", INVALID_MODULE_IDENTIFIER, PARSE_ERROR);
  }

  public void testInvalidRequire() {
    // The ES6 path turns on DependencyOptions.needsManagement() which leads to JsFileLineParser
    // execution that throws a different exception on some invalid goog.provide()s.
    testError("goog.module('ns.a');" + "goog.require(a);", INVALID_REQUIRE_IDENTIFIER, PARSE_ERROR);
  }

  public void testGoogModuleGet1() {
    test(
        "function f() { var x = goog.module.get('a'); }",
        "function f() { var x = a; }");
  }

  public void testGoogModuleGet2() {
    test(
        "function f() { var x = goog.module.get('a.b.c'); }",
        "function f() { var x = a.b.c; }");
  }

  public void testAliasedGoogModuleGet1() {
    test(
        LINE_JOINER.join(
          "goog.module('a');",
          "",
          "var x = goog.forwardDeclare('b');",
          "function f() { x = goog.module.get('b'); }"),
        LINE_JOINER.join(
          "goog.provide('a'); goog.scope(function(){",
          "  var x = b;",
          "  function f() {}",
          "});"));
  }

  public void testAliasedGoogModuleGet2() {
    test(
        LINE_JOINER.join(
          "goog.module('a');",
          "",
          "var x = goog.forwardDeclare('x.y.z');",
          "function f() { x = goog.module.get('x.y.z'); }"),
        LINE_JOINER.join(
          "goog.provide('a'); goog.scope(function(){",
          "  var x = x.y.z;",
          "  function f() {}",
          "});"));
  }

  public void testInvalidGoogModeuleGetAlias() {
    testError("goog.module('a'); x = goog.module.get('g');", INVALID_GET_ALIAS);

    testError("goog.module('a'); var x; x = goog.module.get('g');", INVALID_GET_ALIAS);

    testError(
        "goog.module('a'); var x = goog.forwardDeclare(); x = goog.module.get('g');",
        INVALID_GET_ALIAS);

    testError(
        "goog.module('a'); var x = goog.forwardDeclare('z'); x = goog.module.get('g');",
        INVALID_GET_ALIAS);
  }


  public void testInvalidGoogModuleGet1() {
    testError("function f() {" + "goog.module.get(a);" + "}", INVALID_GET_IDENTIFIER);
  }

  public void testInvalidGoogModuleGet2() {

    testError("goog.module.get('a');", INVALID_GET_CALL_SCOPE);
  }

  public void testExport1() {
    test(
        "goog.module('ns.a');" +
        "exports = {};",

        "goog.provide('ns.a');" +
        "goog.scope(function(){" +
        "  /** @const */ ns.a = {};" +
        "});");
  }

  public void testExport2() {
    test(
        "goog.module('ns.a');" +
        "exports.x = 1;",

        "goog.provide('ns.a');" +
        "goog.scope(function(){" +
        "  /** @const */ ns.a.x = 1;" +
        "});");
  }

  public void testExport3() {
    test(
        "goog.module('xid');" +
        "var xid = function() {};" +
        "exports = xid;",

        "goog.provide('xid');" +
        "goog.scope(function(){" +
        "  var xid_module = function() {};" +
        "  /** @const */ xid = xid_module;" +
        "});");
  }

  public void testExport4() {
    test(
        "goog.module('ns.a');" +
        "exports = { something: 1 };",

        "goog.provide('ns.a');" +
        "goog.scope(function(){" +
        "  /** @const */ ns.a = { /** @const */ something: 1 };" +
        "});");
  }

  public void testExport5() {
    test(
        "goog.module('ns.a');"
        + "/** @typedef {string} */ var x;"
        + "exports.x = x;",

        "goog.provide('ns.a');"
        + "goog.scope(function(){"
        + "  /** @typedef {string} */ var x;"
        + "  /** @typedef {string} */ ns.a.x = x;"
        + "});");
  }

  public void testExport6() {
    test(
        "goog.module('ns.a');"
        + "/** @typedef {string} */ var x;"
        + "exports = { something: x };",

        "goog.provide('ns.a');"
        + "goog.scope(function(){"
        + "  /** @typedef {string} */ var x;"
        + "  /** @const */ ns.a = { /** @typedef {string} */ something: x };"
        + "});");
  }

  public void testExport7() {
    test(
        "goog.module('ns.a');"
        + "/** @constructor */"
        + "exports = function() {};",

        "goog.provide('ns.a');"
        + "goog.scope(function(){"
        + "  /** @constructor */ ns.a = function() {};"
        + "});");
  }

  public void testExport8() {
    test(
        "goog.module('ns.a');"
        + "exports = goog.defineClass({});",

        "goog.provide('ns.a');"
        + "goog.scope(function(){"
        + "  ns.a = goog.defineClass({});"
        + "});");
  }

  public void testExportEnhancedObjectLiteral() {
    testEs6(
        "goog.module('ns.a');" +
        "exports = { something };",

        "goog.provide('ns.a');" +
        "goog.scope(function(){" +
        "  /** @const */ ns.a = { /** @const */ something };" +
        "});");

    testErrorEs6(
        "goog.module('ns.a');" +
        "exports = { [something]: 3 };",
        ClosureRewriteModule.INVALID_EXPORT_COMPUTED_PROPERTY);
  }

  public void testRequiresRetainOrder() {
    test(
        "goog.module('ns.a');" +
        "var b = goog.require('ns.b');" +
        "var c = goog.require('ns.c');",

        "goog.provide('ns.a');" +
        "goog.require('ns.b');" +
        "goog.require('ns.c');" +
        "goog.scope(function(){" +
        "  var b = ns.b;" +
        "  var c = ns.c;" +
        "});");
  }

  public void testSetTestOnly() {
    test(
        "goog.module('ns.a');" +
        "goog.setTestOnly();" +
        "var b = goog.require('ns.b');",

        "goog.provide('ns.a');" +
        "goog.setTestOnly();" +
        "goog.require('ns.b');" +
        "goog.scope(function(){" +
        "  var b = ns.b;" +
        "});");
  }
}
