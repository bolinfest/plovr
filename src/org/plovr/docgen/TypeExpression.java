package org.plovr.docgen;

import com.google.javascript.jscomp.AbstractCompiler;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.JSTypeRegistry;
import com.google.javascript.rhino.jstype.StaticScope;

public class TypeExpression {

  private final String displayName;

  private TypeExpression(String displayName) {
    this.displayName = "undefined".equals(displayName) ? "void" : displayName;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getDisplayName() {
    return displayName;
  }

  public static class Builder {

    private String displayName = null;

    private Builder() {}

    public TypeExpression build() {
      return new TypeExpression(displayName);
    }

    public Builder setType(JSTypeExpression type, AbstractCompiler compiler) {
      if (type !=  null) {
        StaticScope<JSType> scope = null;
        JSTypeRegistry registry = compiler.getTypeRegistry();
        JSType jsType = type.evaluate(scope, registry);
        this.displayName = jsType.getDisplayName();
      }
      return this;
    }
  }
}
