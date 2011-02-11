package org.plovr.soy.function;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.template.soy.shared.restricted.SoyFunction;

/**
 * Guice module for Plovr Soy functions.
 */
public class PlovrModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder<SoyFunction> soyFunctionsSetBinder = Multibinder.newSetBinder(binder(), SoyFunction.class);
    soyFunctionsSetBinder.addBinding().to(SubstringFunction.class);
    soyFunctionsSetBinder.addBinding().to(ListFunction.class);
  }
}
