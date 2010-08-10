goog.provide('example.main');

goog.require('example.templates');
goog.require('goog.ui.Tooltip');
goog.require('soy');


example.main = function() {
  var config = { meaningOfLife: 42 };
  var fragment = soy.renderAsFragment(example.templates.base, config);
  document.body.appendChild(fragment);
  var tooltip = new goog.ui.Tooltip(fragment, 'Hello World!');
};

example.main();

goog.addDependency('/dev/null', ['goog.debug.ErrorHandler'], []);
