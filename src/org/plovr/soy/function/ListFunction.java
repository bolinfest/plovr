package org.plovr.soy.function;

import java.util.Iterator;
import java.util.List;

import com.google.inject.Inject;
import com.google.template.soy.data.SoyData;
import com.google.template.soy.data.SoyListData;
import com.google.template.soy.jssrc.restricted.JsExpr;
import com.google.template.soy.jssrc.restricted.SoyJsSrcFunction;
import com.google.template.soy.tofu.restricted.SoyTofuFunction;

/**
 * A function that makes it possible to define a list on the fly inside a
 * template.
 *
 * Ideally, this would be unnecessary if Closure Templates allowed any type of
 * JSON literals for expressions, rather than the more subset described at:
 * http://code.google.com/closure/templates/docs/concepts.html#expressions
 *
 * Example:
 * <pre>
 * {template .base}
 *   {call .listTemplate}
 *     {param items: list('Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat') /}
 *   {/call}
 * {/template}
 *
 * /** @param items *&#47;
 * {template .listTemplate}
 *   &lt;ul>
 *   {foreach $item in $items}
 *     &lt;li>{$item}
 *   {/foreach}
 *   &lt;/ul>
 * {/template}
 * </pre>
 *
 * <b>Important Caveat:</b>
 * Although you would expect the following to work, currently it does not:
 * <pre>
 * /** @param items *&#47;
 * {template .listTemplate}
 *   &lt;ul>
 *   {foreach $item in list('Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat')}
 *     &lt;li>{$item}
 *   {/foreach}
 *   &lt;/ul>
 * {/template}
 * </pre>
 *
 * @author bolinfest@gmail.com (Michael Bolin)
 */
public class ListFunction implements SoyJsSrcFunction, SoyTofuFunction {

  @Inject
  ListFunction() {}

  @Override
  public String getName() {
    return "list";
  }

  @Override
  public boolean isValidArgsSize(int numArgs) {
    return numArgs >= 0;
  }

  @Override
  public JsExpr computeForJsSrc(List<JsExpr> args) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    for (Iterator<JsExpr> iter = args.iterator(); iter.hasNext(); ) {
      JsExpr expr = iter.next();
      builder.append(expr.getText());
      if (iter.hasNext()) {
        builder.append(",");
      }
    }
    builder.append("]");
    return new JsExpr(builder.toString(), 0);
  }

  @Override
  public SoyData computeForTofu(List<SoyData> args) {
    SoyListData list = new SoyListData();
    for (SoyData datum : args) {
      list.add(datum);
    }
    return list;
  }

}
