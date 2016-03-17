goog.provide('translation.main');

goog.require('translation.templates');

goog.require('goog.ui.Tooltip');
goog.require('soy');


translation.main = function() {
  /** @desc Name */
  var MSG_TESTER = goog.getMsg('Tester');
  var soyData = {
    userName: MSG_TESTER
  };
  var fragment = soy.renderAsFragment(translation.templates.base, soyData);
  document.body.appendChild(fragment);
  /** @desc Greeting */
  var MSG_HELLO_WORLD = goog.getMsg('Hello World!');
  var tooltip = new goog.ui.Tooltip(fragment, MSG_HELLO_WORLD);
};

translation.main();

goog.addDependency('/dev/null', ['goog.debug.ErrorHandler'], []);
