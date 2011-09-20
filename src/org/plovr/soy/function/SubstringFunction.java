package org.plovr.soy.function;

import static com.google.template.soy.shared.restricted.SoyJavaRuntimeFunctionUtils.toSoyData;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.restricted.IntegerData;
import com.google.template.soy.data.restricted.StringData;
import com.google.template.soy.jssrc.restricted.JsExpr;
import com.google.template.soy.jssrc.restricted.SoyJsSrcFunction;
import com.google.template.soy.tofu.restricted.SoyAbstractTofuFunction;
import com.google.template.soy.tofu.restricted.SoyTofuFunction;

/**
 * From "Defining a Custom Function" in "Closure: The Definitive Guide"
 */
@Singleton
public class SubstringFunction extends SoyAbstractTofuFunction
    implements SoyJsSrcFunction, SoyTofuFunction {

  @Inject
  SubstringFunction() {}


  @Override
  public String getName() {
    return "substring";
  }


  @Override
  public Set<Integer> getValidArgsSizes() {
    return ImmutableSet.of(2, 3);
  }


  @Override
  public JsExpr computeForJsSrc(final List<JsExpr> args) {
    JsExpr stringArg = args.get(0);
    JsExpr startArg = args.get(1);

    if (args.size() == 2) {
      return new JsExpr("String(" + stringArg.getText() + ")" +
          ".substring(" + startArg.getText() + ")", Integer.MAX_VALUE);
    } else {
      JsExpr endArg = args.get(2);
      return new JsExpr("String(" + stringArg.getText() + ")" +
          ".substring(" + startArg.getText() + "," + endArg.getText() + ")",
          Integer.MAX_VALUE);
    }
  }


  @Override
  public SoyData compute(final List<SoyData> args) {
    StringData str = (StringData) args.get(0);
    IntegerData start = (IntegerData) args.get(1);

    if (args.size() == 2) {
      return toSoyData(str.getValue().substring(start.getValue()));
    } else {
      IntegerData end = (IntegerData) args.get(2);
      return toSoyData(str.getValue().substring(start.getValue(), end.getValue()));
    }
  }
}
