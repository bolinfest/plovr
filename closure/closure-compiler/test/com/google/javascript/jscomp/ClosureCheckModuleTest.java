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
package com.google.javascript.jscomp;

import static com.google.javascript.jscomp.ClosureCheckModule.EXPORT_NOT_A_MODULE_LEVEL_STATEMENT;
import static com.google.javascript.jscomp.ClosureCheckModule.EXPORT_REPEATED_ERROR;
import static com.google.javascript.jscomp.ClosureCheckModule.GOOG_MODULE_REFERENCES_THIS;
import static com.google.javascript.jscomp.ClosureCheckModule.GOOG_MODULE_USES_GOOG_MODULE_GET;
import static com.google.javascript.jscomp.ClosureCheckModule.GOOG_MODULE_USES_THROW;
import static com.google.javascript.jscomp.ClosureCheckModule.INVALID_DESTRUCTURING_REQUIRE;
import static com.google.javascript.jscomp.ClosureCheckModule.JSDOC_REFERENCE_TO_SHORT_IMPORT_BY_LONG_NAME_INCLUDING_SHORT_NAME;
import static com.google.javascript.jscomp.ClosureCheckModule.LET_GOOG_REQUIRE;
import static com.google.javascript.jscomp.ClosureCheckModule.MODULE_AND_PROVIDES;
import static com.google.javascript.jscomp.ClosureCheckModule.MULTIPLE_MODULES_IN_FILE;
import static com.google.javascript.jscomp.ClosureCheckModule.ONE_REQUIRE_PER_DECLARATION;
import static com.google.javascript.jscomp.ClosureCheckModule.REFERENCE_TO_FULLY_QUALIFIED_IMPORT_NAME;
import static com.google.javascript.jscomp.ClosureCheckModule.REFERENCE_TO_MODULE_GLOBAL_NAME;
import static com.google.javascript.jscomp.ClosureCheckModule.REFERENCE_TO_SHORT_IMPORT_BY_LONG_NAME_INCLUDING_SHORT_NAME;
import static com.google.javascript.jscomp.ClosureCheckModule.REQUIRE_NOT_AT_TOP_LEVEL;

public final class ClosureCheckModuleTest extends Es6CompilerTestCase {
  @Override
  protected CompilerPass getProcessor(Compiler compiler) {
    return new ClosureCheckModule(compiler);
  }

  @Override
  protected CompilerOptions getOptions() {
    CompilerOptions options = super.getOptions();
    options.setWarningLevel(DiagnosticGroups.LINT_CHECKS, CheckLevel.ERROR);
    return options;
  }

  public void testGoogModuleReferencesThis() {
    testError("goog.module('xyz');\nfoo.call(this, 1, 2, 3);", GOOG_MODULE_REFERENCES_THIS);

    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var x = goog.require('other.x');",
            "",
            "if (x) {",
            "  alert(this);",
            "}"),
        GOOG_MODULE_REFERENCES_THIS);

    testSameEs6(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "class Foo {",
            "  constructor() {",
            "    this.x = 5;",
            "  }",
            "}",
            "",
            "exports = Foo;"));
  }

  public void testGoogModuleUsesThrow() {
    testError("goog.module('xyz');\nthrow 4;", GOOG_MODULE_USES_THROW);

    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var x = goog.require('other.x');",
            "",
            "if (x) {",
            "  throw 5;",
            "}"),
        GOOG_MODULE_USES_THROW);
  }

  public void testGoogModuleGetAtTopLevel() {
    testError("goog.module('xyz');\ngoog.module.get('abc');", GOOG_MODULE_USES_GOOG_MODULE_GET);

    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var x = goog.require('other.x');",
            "",
            "if (x) {",
            "  var y = goog.module.get('abc');",
            "}"),
        GOOG_MODULE_USES_GOOG_MODULE_GET);

    testSame(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var x = goog.require('other.x');",
            "",
            "function f() {",
            "  var y = goog.module.get('abc');",
            "}"));
  }

  public void testGoogModuleAndProvide() {
    testError("goog.module('xyz');\ngoog.provide('abc');", MODULE_AND_PROVIDES);
  }

  public void testMultipleGoogModules() {
    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "goog.module('abc');",
            "",
            "var x = goog.require('other.x');"),
        MULTIPLE_MODULES_IN_FILE);
  }

  public void testBundledGoogModules() {
    testError(
        LINE_JOINER.join(
            "goog.loadModule(function(exports){",
            "  'use strict';",
            "  goog.module('xyz');",
            "  foo.call(this, 1, 2, 3);",
            "  return exports;",
            "});"),
        GOOG_MODULE_REFERENCES_THIS);

    testError(
        LINE_JOINER.join(
            "goog.loadModule(function(exports){",
            "  'use strict';",
            "  goog.module('foo.example.ClassName');",
            "  /** @constructor @export */ function ClassName() {}",
            "  exports = ClassName;",
            "  return exports;",
            "});"),
        ClosureCheckModule.AT_EXPORT_IN_GOOG_MODULE);

    testSameEs6(
        LINE_JOINER.join(
            "goog.loadModule(function(exports){",
            "  'use strict';",
            "  goog.module('xyz');",
            "  exports = class {}",
            "  return exports;",
            "});",
            "goog.loadModule(function(exports){",
            "  goog.module('abc');",
            "  var Foo = goog.require('xyz');",
            "  var x = new Foo;",
            "  return exports;",
            "});"));

    testError(
        LINE_JOINER.join(
            "goog.loadModule(function(exports){",
            "  'use strict';",
            "  goog.module('xyz');",
            "  goog.module('abc');",
            "  var x = goog.require('other.x');",
            "  return exports;",
            "});"),
        MULTIPLE_MODULES_IN_FILE);
  }

  public void testGoogModuleReferencesGlobalName() {
    testError("goog.module('x.y.z');\nx.y.z = function() {};", REFERENCE_TO_MODULE_GLOBAL_NAME);
  }

  public void testIllegalAtExport() {
    testError(
        LINE_JOINER.join(
            "goog.module('foo.example.ClassName');",
            "",
            "/** @constructor @export */ function ClassName() {}",
            "",
            "exports = ClassName;"),
        ClosureCheckModule.AT_EXPORT_IN_GOOG_MODULE);

    testErrorEs6(
        LINE_JOINER.join(
            "goog.module('foo.example.ClassName');",
            "",
            "/** @export */ class ClassName {}",
            "",
            "exports = ClassName;"),
        ClosureCheckModule.AT_EXPORT_IN_GOOG_MODULE);

    testError(
        LINE_JOINER.join(
            "goog.module('foo.example.ClassName');",
            "",
            "/** @constructor */ function ClassName() {}",
            "",
            "/** @export */",
            "exports = ClassName;"),
        ClosureCheckModule.AT_EXPORT_IN_NON_LEGACY_GOOG_MODULE);

    testError(
        LINE_JOINER.join(
            "goog.module('foo.example.ns');",
            "",
            "/** @constructor */ function ClassName() {}",
            "",
            "/** @export */",
            "exports.ClassName = ClassName;"),
        ClosureCheckModule.AT_EXPORT_IN_NON_LEGACY_GOOG_MODULE);

    testError(
        LINE_JOINER.join(
            "goog.module('foo.example.ClassName');",
            "goog.module.declareLegacyNamespace();",
            "",
            "/** @constructor @export */ function ClassName() {}",
            "",
            "exports = ClassName;"),
        ClosureCheckModule.AT_EXPORT_IN_GOOG_MODULE);
  }

  public void testLegalAtExport() {
    testSameEs6(
        LINE_JOINER.join(
            "goog.module('foo.example.ClassName');",
            "",
            "class ClassName {",
            "  constructor() {",
            "    /** @export */",
            "    this.prop;",
            "    /** @export */",
            "    this.anotherProp = false;",
            "  }",
            "}",
            "",
            "exports = ClassName;"));

    testSameEs6(
        LINE_JOINER.join(
            "goog.module('foo.example.ClassName');",
            "",
            "var ClassName = class {",
            "  constructor() {",
            "    /** @export */",
            "    this.prop;",
            "    /** @export */",
            "    this.anotherProp = false;",
            "  }",
            "}",
            "",
            "exports = ClassName;"));

    testSame(
        LINE_JOINER.join(
            "goog.module('foo.example.ClassName');",
            "",
            "/** @constructor */",
            "function ClassName() {",
            "  /** @export */",
            "  this.prop;",
            "  /** @export */",
            "  this.anotherProp = false;",
            "}",
            "",
            "exports = ClassName;"));

    testSame(
        LINE_JOINER.join(
            "goog.module('foo.example.ClassName');",
            "",
            "/** @constructor */",
            "var ClassName = function() {",
            "  /** @export */",
            "  this.prop;",
            "  /** @export */",
            "  this.anotherProp = false;",
            "};",
            "",
            "exports = ClassName;"));

    testSame(
        LINE_JOINER.join(
            "goog.module('foo.example.ClassName');",
            "goog.module.declareLegacyNamespace();",
            "",
            "/** @constructor */ function ClassName() {}",
            "",
            "/** @export */",
            "exports = ClassName;"));

    testSame(
        LINE_JOINER.join(
            "goog.module('foo.example.ns');",
            "goog.module.declareLegacyNamespace();",
            "",
            "/** @constructor */ function ClassName() {}",
            "",
            "/** @export */",
            "exports.ClassName = ClassName;"));

    testSame(
        LINE_JOINER.join(
            "goog.module('foo.example.ClassName');",
            "",
            "/** @constructor */ var exports = function() {}",
            "",
            "/** @export */",
            "exports.prototype.fly = function() {};"));
  }

  public void testIllegalGoogRequires() {
    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var foo = goog.require('other.x').foo;"),
        REQUIRE_NOT_AT_TOP_LEVEL);

    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var x = goog.require('other.x').foo.toString();"),
        REQUIRE_NOT_AT_TOP_LEVEL);

    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var moduleNames = [goog.require('other.x').name];"),
        REQUIRE_NOT_AT_TOP_LEVEL);

    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "exports = [goog.require('other.x').name];"),
        REQUIRE_NOT_AT_TOP_LEVEL);

    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var a = goog.require('foo.a'), b = goog.require('foo.b');"),
        ONE_REQUIRE_PER_DECLARATION);

    testErrorEs6(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var [foo, bar] = goog.require('other.x');"),
        INVALID_DESTRUCTURING_REQUIRE);

    testErrorEs6(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var {foo, bar = 'str'} = goog.require('other.x');"),
        INVALID_DESTRUCTURING_REQUIRE);

    testErrorEs6(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var {foo, bar: {name}} = goog.require('other.x');"),
        INVALID_DESTRUCTURING_REQUIRE);
  }

  public void testIllegalShortImportReferencedByLongName() {
    testError(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var A = goog.require('foo.A');",
            "",
            "exports = function() { return new foo.A; };"),
        REFERENCE_TO_SHORT_IMPORT_BY_LONG_NAME_INCLUDING_SHORT_NAME);
  }

  public void testIllegalShortImportReferencedByLongName_extends() {
    testError(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var A = goog.require('foo.A');",
            "",
            "/** @constructor @implements {foo.A} */ function B() {}"),
        JSDOC_REFERENCE_TO_SHORT_IMPORT_BY_LONG_NAME_INCLUDING_SHORT_NAME);

    testError(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var A = goog.require('foo.A');",
            "",
            "/** @type {foo.A} */ var a;"),
        JSDOC_REFERENCE_TO_SHORT_IMPORT_BY_LONG_NAME_INCLUDING_SHORT_NAME);

    testSame(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var A = goog.require('foo.A');",
            "",
            "/** @type {A} */ var a;"));

    testSame(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var Foo = goog.require('Foo');",
            "",
            "/** @type {Foo} */ var a;"));

    testError(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var ns = goog.require('some.namespace');",
            "",
            "/** @type {some.namespace.Foo} */ var foo;"),
        JSDOC_REFERENCE_TO_SHORT_IMPORT_BY_LONG_NAME_INCLUDING_SHORT_NAME);

    testError(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var ns = goog.require('some.namespace');",
            "",
            "/** @type {Array<some.namespace.Foo>} */ var foos;"),
        JSDOC_REFERENCE_TO_SHORT_IMPORT_BY_LONG_NAME_INCLUDING_SHORT_NAME);
  }

  public void testIllegalShortImportDestructuring() {
    testErrorEs6(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var {doThing} = goog.require('foo.utils');",
            "",
            "exports = function() { return foo.utils.doThing(''); };"),
        REFERENCE_TO_FULLY_QUALIFIED_IMPORT_NAME);
  }

  public void testIllegalImportNoAlias() {
    testErrorEs6(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "goog.require('foo.utils');",
            "",
            "exports = function() { return foo.utils.doThing(''); };"),
        REFERENCE_TO_FULLY_QUALIFIED_IMPORT_NAME);
  }

  // TODO(johnlenz): Re-enable these tests (they are a bit tricky).
  public void disable_testSingleNameImportNoAlias1() {
    testErrorEs6(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "goog.require('foo');",
            "",
            "exports = function() { return foo.doThing(''); };"),
        REFERENCE_TO_FULLY_QUALIFIED_IMPORT_NAME);
  }

  public void disable_testSingleNameImportWithAlias() {
    testErrorEs6(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var bar = goog.require('foo');",
            "",
            "exports = function() { return foo.doThing(''); };"),
        REFERENCE_TO_FULLY_QUALIFIED_IMPORT_NAME);
  }

  public void testSingleNameImportCrossAlias() {
    testSame(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var bar = goog.require('foo');",
            "var foo = goog.require('bar');",
            "",
            "exports = function() { return foo.doThing(''); };"));
  }

  public void testLegalSingleNameImport() {
    testSame(
        LINE_JOINER.join(
            "goog.module('x.y.z');",
            "",
            "var foo = goog.require('foo');",
            "",
            "exports = function() { return foo.doThing(''); };"));
  }

  public void testIllegalLetShortRequire() {
    testErrorEs6(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "let a = goog.require('foo.a');"),
        LET_GOOG_REQUIRE);
  }

  public void testLegalGoogRequires() {
    testSameEs6(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "var {assert} = goog.require('goog.asserts');"));

    testSameEs6(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "const {assert} = goog.require('goog.asserts');"));

    testSameEs6(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "const {assert, fail} = goog.require('goog.asserts');"));
  }

  public void testIllegalExports() {
    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "if (window.exportMe) { exports = 5; }"),
            EXPORT_NOT_A_MODULE_LEVEL_STATEMENT);

    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "window.exportMe && (exports = 5);"),
            EXPORT_NOT_A_MODULE_LEVEL_STATEMENT);

    testError(
        LINE_JOINER.join(
            "goog.module('xyz');",
            "",
            "exports = 5;",
            "exports = 'str';"),
            EXPORT_REPEATED_ERROR);
  }
}
