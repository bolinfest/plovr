goog.provide('example.api');

goog.require('example.App');

/** @param {string} message */
example.api.setMessage = function(message) {
  var app = example.App.getInstance();
  app.setMessage(message);
};

goog.exportSymbol('example.api.setMessage', example.api.setMessage);
