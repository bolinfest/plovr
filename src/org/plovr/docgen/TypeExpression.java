package org.plovr.docgen;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.JSTypeRegistry;
import com.google.javascript.rhino.jstype.StaticScope;
import com.google.javascript.rhino.jstype.UnionType;

public class TypeExpression {

  private final String paramName;

  private TypeExpression(String paramName) {
    this.paramName = "undefined".equals(paramName) ? "void" : paramName;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * The "plain name" of this type, so even though a supertype may reference a
   * type expression which describes a non-null type (such as
   * "!goog.Disposable"), it is often useful to get the type without the
   * nullability modifier, so if this type expression represented
   * "!goog.Disposable", this method would return "goog.Disposable".
   */
  public String getDisplayName() {
    String displayName = paramName;
    if (paramName.startsWith("!") || paramName.startsWith("?")) {
      displayName = displayName.substring(1);
    }
    if (paramName.endsWith("=")) {
      displayName = displayName.substring(1, displayName.length());
    }
    return displayName;
  }

  /**
   * A "param name" is how this type should be displayed as a parameter,
   * which may include modifiers such as "?" or "=" to describe nullability
   * or optionality, respectively.
   */
  public String getParamName() {
    return paramName;
  }

  public static class Builder {

    private String paramName = null;

    private Builder() {}

    public TypeExpression build() {
      return new TypeExpression(paramName);
    }

    public Builder setType(JSTypeExpression type, AbstractCompiler compiler) {
      if (type != null) {
        StaticScope<JSType> scope = null;
        JSTypeRegistry registry = compiler.getTypeRegistry();
        JSType jsType = type.evaluate(scope, registry);
        paramName = jsType.getDisplayName();
        if (paramName == null) {
          if (jsType.isUnionType()) {
            paramName = formatUnionType(
                (UnionType)jsType,
                type.isOptionalArg(),
                jsType.isObject());
          } else {
            paramName = jsType.toString();
          }
        } else if (jsType.isObject() && !jsType.isUnionType()) {
          // If it is an object type that is not a union type, then it cannot be
          // of the form (Element|null), so it should be !Element.
          paramName = "!" + paramName;
        }
      }
      return this;
    }

    private static String formatUnionType(
        UnionType unionType, boolean isOptionalArg, boolean isObject) {
      String paramName = unionType.toString();
      // A union type that can be undefined because it is an optional
      // argument should display with a trailing "=" to indicate its
      // optionality rather than "undefined" as a union member.
      if (isOptionalArg && paramName.endsWith("|undefined)")) {
        paramName = paramName.replaceFirst("\\|undefined\\)$", "=)");
        if (!paramName.contains("|")) {
          // In this case, there is only one other valid value in the union,
          // which could be a primitive or an object. Therefore, the expression
          // should no longer be wrapped in parens, and if it is an object type,
          // it should be prefixed with a "!".
          paramName = paramName.substring(1, paramName.length() - 1);
          if (isObject) {
            paramName = "!" + paramName;
          }
          return paramName;
        }
      }

      if (!unionType.isNullable()) {
        return paramName;
      }

      List<JSType> nonNullTypes = Lists.newArrayList();
      for (JSType typeMember : unionType.getAlternates()) {
        if (!typeMember.isNullType() &&
            (!(isOptionalArg && typeMember.isVoidType()))) {
          nonNullTypes.add(typeMember);
        }
      }

      if (nonNullTypes.size() == 1) {
        return "?" + nonNullTypes.get(0).toString() +
            (isOptionalArg ? "=" : "");
      } else {
        return paramName;
      }
    }
  }
}
