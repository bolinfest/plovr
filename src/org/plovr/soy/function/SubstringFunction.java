package org.plovr.soy.function;

import static com.google.template.soy.tofu.restricted.SoyTofuFunctionUtils.toSoyData;

import java.util.List;

import com.google.inject.Inject;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.restricted.IntegerData;
import com.google.template.soy.data.restricted.StringData;
import com.google.template.soy.jssrc.restricted.JsExpr;
import com.google.template.soy.jssrc.restricted.SoyJsSrcFunction;
import com.google.template.soy.tofu.restricted.SoyTofuFunction;

/**
 * From "Defining a Custom Function" in "Closure: The Definitive Guide"
 */
public class SubstringFunction
    implements SoyJsSrcFunction, SoyTofuFunction {

  @Inject
  SubstringFunction() {}


  @Override
  public String getName() {
    return "substring";
  }


  @Override
  public boolean isValidArgsSize(int numArgs) {
    return 2 <= numArgs && numArgs <= 3;
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
  public SoyData computeForTofu(final List<SoyData> args) {
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
