// BEGIN_INCLUDE_APP
goog.provide('example.App');

goog.require('goog.dom');
goog.require('goog.string');
goog.require('goog.ui.Component');


/**
 * @param {!goog.dom.DomHelper} dom
 * @constructor
 * @extends {goog.ui.Component}
 */
example.App = function(dom) {
  goog.base(this, dom);
};
goog.inherits(example.App, goog.ui.Component);

/**
 * @type {example.Settings}
 * @private
 */
example.App.prototype.settings_;

/**
 * @type {function(goog.events.Event)}
 * @private
 */
example.App.buttonClickHandler_ = goog.nullFunction;

/** @inheritDoc */
example.App.prototype.createDom = function() {
  var dom = this.dom_;
  var el = dom.createDom('div', undefined /* opt_attributes */,
      dom.createDom('span', undefined /* opt_attributes */, 'Messages appear here'),
      dom.createDom('button', undefined /* opt_attributes */, 'Load Settings'));
  this.setElementInternal(el);
};

/** @inheritDoc */
example.App.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var button = this.dom_.getElementsByTagNameAndClass(
      'button', undefined /* className */, this.getElement())[0];
  this.getHandler().listen(button,
                           goog.events.EventType.CLICK,
                           this.onButtonClick_);
};

/**
 * @param {goog.events.Event} e
 * @private
 */
example.App.prototype.onButtonClick_ = function(e) {
  example.App.buttonClickHandler_.call(this, e);
};

/** @param {function(goog.events.Event)} handler */
example.App.setButtonClickHandler = function(handler) {
  example.App.buttonClickHandler_ = handler;
};

/** Invoke this method when example.Settings is available. */ 
example.App.prototype.onSettingsLoaded = function() {
  // The settings module adds the example.Settings component as the first and
  // only child of this component.
  this.settings_ = /** @type {example.Settings} */ (this.getChildAt(0));
};

/** @param {string} message */
example.App.prototype.setMessage = function(message) {
  var span = this.dom_.getElementsByTagNameAndClass(
      'span', undefined /* className */, this.getElement())[0];
  span.innerHTML = goog.string.htmlEscape(message);
};

/**
 * @type {example.App}
 * @private
 */
example.App.instance_;

/**
 * @param {string} id
 */
example.App.install = function(id) {
  if (example.App.instance_) return;
  var dom = new goog.dom.DomHelper();
  var app = new example.App(dom);
  app.render(dom.getElement(id));
  example.App.instance_ = app;  
};

/** @return {example.App} */
example.App.getInstance = function() {
  return example.App.instance_;
};
// END_INCLUDE_APP

// BEGIN_INCLUDE_EXPORT
goog.exportSymbol('example.App.install', example.App.install);
// END_INCLUDE_EXPORT
