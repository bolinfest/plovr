goog.provide('example.main');

goog.require('example.templates');

goog.require('goog.dom');
goog.require('soy');

example.main = function() {
  var context = {
    heading: 'Hello World!'
  };
  soy.renderElement(
      goog.dom.getElement('content'),
      example.templates.base,
      context)
};

example.main();
