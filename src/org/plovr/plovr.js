(function() {

var plovr = plovr || {};

plovr.htmlEscape = function(str) {
  return str.replace(/[&<"]/g, function(ch) {
    switch (ch) {
      case '&': return '&amp;';
      case '<': return '&lt;';
      case '"': return '&quot;';
    }
  });
};

/**
 * @type {Array.<Object>}
 */
plovr.errors_ = [];

plovr.ERROR_STYLE =
  'color: #A00;' +
  'background-color: #FFF;' +
  'font-family: monospace;' +
  'font-size: 12px;' +
  'border: 1px solid #A00;' +
  'padding: 1px 3px;'

plovr.WARNING_STYLE =
  'color: #F29000;' +
  'background-color: #FFF;' +
  'font-family: monospace;' +
  'font-size: 12px;' +
  'border: 1px solid #F29000;' +
  'padding: 1px 3px;'

plovr.addErrors = function(errors) {
  for (var i = 0; i < errors.length; i++) plovr.errors_.push(errors[i]);
};

/**
 * @type {Array.<Object>}
 */
plovr.warnings_ = [];

plovr.addWarnings = function(warnings) {
  for (var i = 0; i < warnings.length; i++) plovr.warnings_.push(warnings[i]);
};

/**
 * Writes the errors into a DIV as the first child of the BODY, so the BODY must
 * be available when this is run.
 */
plovr.writeErrors = function() {
  var div = document.createElement('div');
  var html = [];

  for (var i = 0, len = plovr.errors_.length; i < len; i++) {
    var error = plovr.errors_[i];
    html.push('<div style="', plovr.ERROR_STYLE, '">',
        plovr.htmlEscape(error['message']), '</div>');
  }

  for (var i = 0, len = plovr.warnings_.length; i < len; i++) {
    var error = plovr.warnings_[i];
    html.push('<div>', plovr.htmlEscape(error['message']), '</div>');
  }

  div.innerHTML = html.join('');
  document.body.insertBefore(div, document.body.firstChild);
}

plovr.writeErrorsOnLoad = function() {
  if (document.body) {
    plovr.writeErrors();
  } else if (window.addEventListener) {
    window.addEventListener('load', plovr.writeErrors, false);
  } else if (window.attachEvent) {
    window.attachEvent('onload', plovr.writeErrors);
  }
};

window['plovr'] = plovr;

})();
