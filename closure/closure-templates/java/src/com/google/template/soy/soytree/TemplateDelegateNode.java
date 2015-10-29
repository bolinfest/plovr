/*
 * Copyright 2011 Google Inc.
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

package com.google.template.soy.soytree;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.base.internal.BaseUtils;
import com.google.template.soy.basetree.CopyState;
import com.google.template.soy.exprtree.ExprNode;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.exprtree.GlobalNode;
import com.google.template.soy.exprtree.IntegerNode;
import com.google.template.soy.exprtree.StringNode;
import com.google.template.soy.soytree.SoyNode.ExprHolderNode;
import com.google.template.soy.soytree.defn.TemplateParam;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Node representing a delegate template.
 *
 * <p>Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
public final class TemplateDelegateNode extends TemplateNode implements ExprHolderNode {

  /**
   * Value class for a delegate template key (name and variant).
   *
   * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
   */
  @AutoValue public abstract static class DelTemplateKey {

    public static DelTemplateKey create(String name, String variant) {
      return create(name, checkNotNull(variant), null);
    }

    // TODO(lukes): we should be able to remove this now that the substitute globals pass is always
    // run prior to when the first TemplateRegistry is created.  furthermore we can assert that
    // all globals in variant expressions are always substituted.
    /**
     * This constructor adds support a temporary solution to using globals as deltemplate variants.
     * During parsing, TemplateRegistry instances must be built for validation, but the expression
     * values are not yet available at that stage. Maintaining the expression as a temporary key
     * allows a partial validation, but this should be removed once TemplateRegistry is refactored
     * to support this.
     */
    private static DelTemplateKey create(
        String name, @Nullable String variant, @Nullable String variantExpr) {
      return new AutoValue_TemplateDelegateNode_DelTemplateKey(name, variant, variantExpr);
    }

    DelTemplateKey() {}

    public abstract String name();
    @Nullable public abstract String variant();
    @Nullable public abstract String variantExpr();

    @Override public String toString() {
      return name() + ((variant() == null || variant().length() == 0) ? "" : ":" + variant())
          + ((variantExpr() == null || variantExpr().length() == 0) ? "" : ":" + variantExpr());
      }
  }

  /** The delegate template name. */
  private final String delTemplateName;

  /** The delegate template variant. */
  private String delTemplateVariant;

  /** An expression that defines a delegate template variant. */
  private final ExprRootNode delTemplateVariantExpr;

  /** The delegate template key (name and variant). */
  private DelTemplateKey delTemplateKey;

  /** The delegate priority. */
  private final Priority delPriority;

  /**
   * Main constructor. This is package-private because TemplateDelegateNode instances should be
   * built using TemplateDelegateNodeBuilder.
   *
   * @param nodeBuilder Builder containing template initialization params.
   * @param soyFileHeaderInfo Info from the containing Soy file's header declarations.
   * @param delTemplateName The delegate template name.
   * @param delTemplateVariant The delegate template variant.
   * @param delTemplateVariantExpr An expression that references a delegate template variant.
   * @param delTemplateKey The delegate template key (name and variant).
   * @param delPriority The delegate priority.
   * @param params The params from template header or SoyDoc. Null if no decls and no SoyDoc.
   */
  TemplateDelegateNode(
      TemplateDelegateNodeBuilder nodeBuilder,
      SoyFileHeaderInfo soyFileHeaderInfo, String delTemplateName, String delTemplateVariant,
      ExprRootNode delTemplateVariantExpr, DelTemplateKey delTemplateKey, Priority delPriority,
      ImmutableList<TemplateParam> params) {

    super(nodeBuilder, "deltemplate", soyFileHeaderInfo,
        Visibility.PUBLIC /* deltemplate always has public visibility */,
        params);
    this.delTemplateName = delTemplateName;
    this.delTemplateVariant = delTemplateVariant;
    this.delTemplateVariantExpr = delTemplateVariantExpr;
    this.delTemplateKey = delTemplateKey;
    this.delPriority = delPriority;
  }

  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  private TemplateDelegateNode(TemplateDelegateNode orig, CopyState copyState) {
    super(orig, copyState);
    this.delTemplateName = orig.delTemplateName;
    this.delTemplateVariant = orig.delTemplateVariant;
    this.delTemplateVariantExpr = orig.delTemplateVariantExpr;
    this.delTemplateKey = orig.delTemplateKey;
    this.delPriority = orig.delPriority;
  }

  static void verifyVariantName(String delTemplateVariant) {
    if (delTemplateVariant.length() > 0 && !(BaseUtils.isIdentifier(delTemplateVariant))) {
      throw SoySyntaxException.createWithoutMetaInfo(
          "Invalid variant \"" + delTemplateVariant + "\" in 'deltemplate'" +
          " (when a string literal is used, value must be an identifier).");
    }
  }

  @Override public Kind getKind() {
    return Kind.TEMPLATE_DELEGATE_NODE;
  }


  /** Returns the delegate template name. */
  public String getDelTemplateName() {
    return delTemplateName;
  }


  /** Returns the delegate template variant. */
  public String getDelTemplateVariant() {
    if (delTemplateVariant != null) {
      return delTemplateVariant;
    }
    return resolveVariantExpression().variant();
  }


  /** Returns the delegate template key (name and variant). */
  public DelTemplateKey getDelTemplateKey() {
    if (delTemplateKey != null) {
      return delTemplateKey;
    }
    return resolveVariantExpression();
  }


  /** Returns the delegate priority. */
  public Priority getDelPriority() {
    return delPriority;
  }


  @Override public TemplateDelegateNode copy(CopyState copyState) {
    return new TemplateDelegateNode(this, copyState);
  }


  @Override
  public List<ExprUnion> getAllExprUnions() {
    if (delTemplateVariantExpr == null) {
      return ImmutableList.of();
    }
    return ImmutableList.of(new ExprUnion(delTemplateVariantExpr));
  }

  /**
   * When a variant value is not defined at parsing time (e.g. when a global constant is used) the
   * deltemplate variant and deltemplate key fields in this node have null value. To fetch their
   * values, we must lazily resolve the expression, after globals are substituted.
   */
  private DelTemplateKey resolveVariantExpression() {
    if (delTemplateVariantExpr == null || delTemplateVariantExpr.numChildren() != 1) {
      throw invalidExpressionError();
    }
    ExprNode exprNode = delTemplateVariantExpr.getRoot();
    if (exprNode instanceof IntegerNode) {
      // Globals were already substituted: We may now create the definitive variant and key fields
      // on this node.
      int variantValue = ((IntegerNode) exprNode).getValue();
      Preconditions.checkArgument(
          variantValue >= 0,
          "Globals used as deltemplate variants must not evaluate to negative numbers.");
      delTemplateVariant = String.valueOf(variantValue);
      delTemplateKey = DelTemplateKey.create(delTemplateName, delTemplateVariant);
      return delTemplateKey;
    } else if (exprNode instanceof StringNode) {
      // Globals were already substituted: We may now create the definitive variant and key fields
      // on this node.
      delTemplateVariant = ((StringNode) exprNode).getValue();
      TemplateDelegateNode.verifyVariantName(delTemplateVariant);
      delTemplateKey = DelTemplateKey.create(delTemplateName, delTemplateVariant);
      return delTemplateKey;
    } else if (exprNode instanceof GlobalNode) {
      // Globals were not yet substituted, but the variant value or deltemplate key was requested.
      // This happens when a TemplateRegistry must be built during the template parsing phase, for
      // instance. To address that, we can temporarily create a key that uses the expression literal
      // as variant value. This allows us to catch conflicts of variant values if the expressions
      // match during parsing, but not if we have value conflicts. If two different globals with the
      // same values are used, will only able to catch that on later stages of the template
      // processing.
      return DelTemplateKey.create(delTemplateName, null, ((GlobalNode) exprNode).getName());
    } else {
      throw invalidExpressionError();
    }
  }

  private AssertionError invalidExpressionError() {
    return new AssertionError("Invalid expression for deltemplate variant for " + delTemplateName
        + " template");
  }
}
