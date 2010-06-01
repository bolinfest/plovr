goog.provide('example.main');

goog.require('example.templates');

example.main = function() {
  var config = { meaningOfLife: 42 };
  document.body.innerHTML = example.templates.base(config);
};

example.main();
