goog.provide('example.main');

goog.require('example.templates');

// Pull this in as a regression test for
// http://code.google.com/p/plovr/issues/detail?id=11
goog.require('goog.i18n.DateTimeSymbols_es');

goog.require('goog.ui.Tooltip');
goog.require('soy');


example.main = function() {
  var soyData = {
    meaningOfLife: 42,
    weekdays: goog.i18n.DateTimeSymbols_es.SHORTWEEKDAYS
  };
  var fragment = soy.renderAsFragment(example.templates.base, soyData);
  document.body.appendChild(fragment);
  var tooltip = new goog.ui.Tooltip(fragment, 'Hello World!');
};

example.main();

goog.addDependency('/dev/null', ['goog.debug.ErrorHandler'], []);
