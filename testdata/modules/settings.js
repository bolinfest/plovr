goog.provide("example.Settings");

goog.require("goog.ui.Component");

/**
 * @param {goog.dom.DomHelper} dom
 * @constructor
 * @extends {goog.ui.Component}
 */
example.Settings = function(dom) {
  goog.ui.Component.call(this, dom);
};
goog.inherits(example.Settings, goog.ui.Component);

/** @inheritDoc */
example.Settings.prototype.createDom = function() {
  example.Settings.superClass_.createDom.call(this);
  this.getElement().innerHTML = "This is the settings component.";
};
