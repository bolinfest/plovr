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

package com.google.javascript.jscomp.parsing.parser;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents various aspects of language version and support.
 *
 * <p>This is somewhat redundant with LanguageMode, but is separate
 * for two reasons: (1) it's used for parsing, which cannot
 * depend on LanguageMode, and (2) it's concerned with slightly
 * different nuances: implemented features and modules rather
 * than strictness.
 *
 * <p>In the long term, it would be good to disentangle all these
 * concerns and pull out a single LanguageSyntax enum with a
 * separate strict mode flag, and then these could possibly be
 * unified.
 *
 * <p>Instances of this class are immutable.
 */
public final class FeatureSet implements Serializable {

  /** The number of the language version: 3, 5, or 6. */
  private final int number;
  /** Whether this includes features not supported in current stable browsers. */
  private final boolean unsupported;
  /** Whether ES6 modules are included. */
  private final boolean es6Modules;
  /** Whether TypeScript syntax is included (for .d.ts support). */
  private final boolean typeScript;

  /** The bare minimum set of features in ES3. */
  public static final FeatureSet ES3 = new FeatureSet(3, false, false, false);
  /** Features from ES5 only. */
  public static final FeatureSet ES5 = new FeatureSet(5, false, false, false);
  /** The subset of ES6 features that are implemented in stable Chrome, Firefox, and Edge. */
  public static final FeatureSet ES6_IMPL = new FeatureSet(6, false, false, false);
  /** The full set of ES6 features, not including modules. */
  public static final FeatureSet ES6 = new FeatureSet(6, true, false, false);
  /** All ES6 features, including modules. */
  public static final FeatureSet ES6_MODULES = new FeatureSet(6, true, true, false);
  public static final FeatureSet ES7 = new FeatureSet(7, true, false, false);
  public static final FeatureSet ES7_MODULES = new FeatureSet(7, true, true, false);
  public static final FeatureSet ES8 = new FeatureSet(8, true, false, false);
  public static final FeatureSet ES8_MODULES = new FeatureSet(8, true, true, false);
  /** TypeScript syntax. */
  public static final FeatureSet TYPESCRIPT = new FeatureSet(8, true, true, true);

  /**
   * Specific features that can be included (indirectly) in a FeatureSet.
   * This primarily adds a name so that helper functions can simultaneously
   * update the detected features and also warn about unsupported features
   * by name.  Additionally, collecting these all in one place provides a
   * single file that needs to be edited to update the current browser support.
   */
  public enum Feature {
    // ES5 features
    ES3_KEYWORDS_AS_IDENTIFIERS("ES3 keywords as identifiers", ES5),
    GETTER("getters", ES5),
    KEYWORDS_AS_PROPERTIES("reserved words as properties", ES5),
    SETTER("setters", ES5),
    STRING_CONTINUATION("string continuation", ES5),
    TRAILING_COMMA("trailing comma", ES5),

    // ES6 features that are already implemented in modern stable browsers
    // (Chrome 50, Firefox 46, and Edge 13)
    ARROW_FUNCTIONS("arrow function", ES6_IMPL),
    BINARY_LITERALS("binary literal", ES6_IMPL),
    OCTAL_LITERALS("octal literal", ES6_IMPL),
    CLASSES("class", ES6_IMPL),
    COMPUTED_PROPERTIES("computed property", ES6_IMPL),
    EXTENDED_OBJECT_LITERALS("extended object literal", ES6_IMPL),
    FOR_OF("for-of loop", ES6_IMPL),
    GENERATORS("generator", ES6_IMPL),
    LET_DECLARATIONS("let declaration", ES6_IMPL),
    MEMBER_DECLARATIONS("member declaration", ES6_IMPL),
    REGEXP_FLAG_Y("RegExp flag 'y'", ES6_IMPL),
    REST_PARAMETERS("rest parameter", ES6_IMPL),
    SPREAD_EXPRESSIONS("spread expression", ES6_IMPL),
    SUPER("super", ES6_IMPL),
    TEMPLATE_LITERALS("template literal", ES6_IMPL),

    // ES6 features that are not yet implemented in all "modern" browsers
    // The following features will become stable once Edge 14 is released:
    CONST_DECLARATIONS("const declaration", ES6),
    DESTRUCTURING("destructuring", ES6),
    NEW_TARGET("new.target", ES6), // Not fully supported until Edge 14
    REGEXP_FLAG_U("RegExp flag 'u'", ES6), // (note: case folding still broken even in Edge 14)
    // The following features are still broken even in Firefox 49:
    // See https://bugzilla.mozilla.org/show_bug.cgi?id=1187502 for details
    DEFAULT_PARAMETERS("default parameter", ES6),

    // ES6 features that include modules
    MODULES("modules", ES6_MODULES),

    // '**' operator
    EXPONENT_OP("exponent operator (**)", ES7),

    // http://tc39.github.io/ecmascript-asyncawait/
    ASYNC_FUNCTIONS("async function", ES8),

    // ES6 typed features that are not at all implemented in browsers
    AMBIENT_DECLARATION("ambient declaration", TYPESCRIPT),
    CALL_SIGNATURE("call signature", TYPESCRIPT),
    CONSTRUCTOR_SIGNATURE("constructor signature", TYPESCRIPT),
    ENUM("enum", TYPESCRIPT),
    GENERICS("generics", TYPESCRIPT),
    IMPLEMENTS("implements", TYPESCRIPT),
    INDEX_SIGNATURE("index signature", TYPESCRIPT),
    INTERFACE("interface", TYPESCRIPT),
    MEMBER_VARIABLE_IN_CLASS("member variable in class", TYPESCRIPT),
    NAMESPACE_DECLARATION("namespace declaration", TYPESCRIPT),
    OPTIONAL_PARAMETER("optional parameter", TYPESCRIPT),
    TYPE_ALIAS("type alias", TYPESCRIPT),
    TYPE_ANNOTATION("type annotation", TYPESCRIPT);

    private final String name;
    private final FeatureSet features;

    private Feature(String name, FeatureSet features) {
      this.name = name;
      this.features = features;
    }

    public FeatureSet features() {
      return features;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private FeatureSet(int number, boolean unsupported, boolean es6Modules, boolean typeScript) {
    this.number = number;
    this.unsupported = unsupported;
    this.es6Modules = es6Modules;
    this.typeScript = typeScript;
  }

  /** Returns a string representation suitable for encoding in depgraph and deps.js files. */
  public String version() {
    if (typeScript) {
      return "ts";
    } else if (number > 6) {
      return "es" + number;
    } else if (unsupported || es6Modules) {
      return "es6";
    } else if (number > 5) {
      return "es6-impl";
    } else if (number > 3) {
      return "es5";
    }
    return "es3";
  }

  /** Returns whether this feature set includes ES6 modules. */
  public boolean hasEs6Modules() {
    return es6Modules;
  }

  /** Returns whether this feature set includes typescript features. */
  public boolean isTypeScript() {
    return typeScript;
  }

  /** Returns a feature set combining all the features from {@code this} and {@code other}. */
  public FeatureSet require(FeatureSet other) {
    return this.contains(other) ? this : this.union(other);
  }

  /**
   * Returns a new {@link FeatureSet} including all features of both {@code this} and {@code other}.
   */
  public FeatureSet union(FeatureSet other) {
    return new FeatureSet(
        Math.max(number, other.number),
        unsupported || other.unsupported,
        es6Modules || other.es6Modules,
        typeScript || other.typeScript);
  }

  /**
   * Does this {@link FeatureSet} contain all of the features of {@code other}?
   */
  public boolean contains(FeatureSet other) {
    return this.number >= other.number
        && (this.unsupported || !other.unsupported)
        && (this.es6Modules || !other.es6Modules)
        && (this.typeScript || !other.typeScript);
  }

  /** Returns a feature set combining all the features from {@code this} and {@code feature}. */
  public FeatureSet require(Feature feature) {
    return require(feature.features);
  }

  /**
   * Does this {@link FeatureSet} include {@code feature}?
   */
  public boolean contains(Feature feature) {
    return contains(feature.features());
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof FeatureSet
        && ((FeatureSet) other).number == number
        && ((FeatureSet) other).unsupported == unsupported
        && ((FeatureSet) other).es6Modules == es6Modules
        && ((FeatureSet) other).typeScript == typeScript;
  }

  @Override
  public int hashCode() {
    return Objects.hash(number, unsupported, es6Modules, typeScript);
  }

  @Override
  public String toString() {
    return "FeatureSet{number=" + number
        + (unsupported ? ", unsupported" : "")
        + (es6Modules ? ", es6Modules" : "")
        + (typeScript ? ", typeScript" : "")
        + "}";
  }

  /** Returns a the name of a corresponding LanguageMode enum element. */
  public String toLanguageModeString() {
    return "ECMASCRIPT" + number;
  }

  /** Parses known strings into feature sets. */
  public static FeatureSet valueOf(String name) {
    switch (name) {
      case "es3":
        return ES3;
      case "es5":
        return ES5;
      case "es6-impl":
        return ES6_IMPL;
      case "es6":
        return ES6;
      case "ts":
        return TYPESCRIPT;
      default:
        throw new IllegalArgumentException("No such FeatureSet: " + name);
    }
  }

  public boolean isEs6ImplOrHigher() {
    String version = version();
    return !version.equals("es3") && !version.equals("es5");
  }
}
