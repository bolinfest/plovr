goog.provide('example.main');

goog.require('example.templates');
goog.require('soy');

example.main = function() {
  var config = { meaningOfLife: 42 };
  var div = document.createElement('div');
  soy.renderElement(div, example.templates.base, config);
  document.body.appendChild(div);
};

example.main();
