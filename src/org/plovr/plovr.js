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

/** @return {number} */
plovr.getPort = function() {
  return 9810;
}

/** @type {string} */
plovr.configId_ = '';

/** @return {string} */
plovr.getConfigId = function() {
  return plovr.configId_;  
};

/** @param {string} */
plovr.setConfigId = function(configId) {
  plovr.configId_ = configId;  
};

plovr.getViewSourceUrl = function() {
  return 'http://localhost:' + plovr.getPort() + '/view';  
};

/**
 * @param {Array} errors
 * @param {Array.<string>} html
 * @param {string} style
 */
plovr.writeErrors_ = function(errors, html, style) {
  for (var i = 0, len = errors.length; i < len; i++) {
    var error = errors[i];
    var message = error['message'];

    // Check whether the message starts with the name followed by a line number,
    // and if so, hyperlink it.
    var prefix = error['input'] + ':' + error['lineNumber'] + ':';
    var anchor;
    if (message.indexOf(prefix) == 0) {
      message = message.substring(prefix.length);
      anchor = '<a href="' + plovr.getViewSourceUrl() +
          '?id=' + encodeURIComponent(plovr.getConfigId()) +
          '&name=' + encodeURIComponent(error['input']) +
          '&lineNumber=' + error['lineNumber'] + '">' +
          plovr.htmlEscape(prefix) +
          '</a>';
    }

    var htmlMessage = plovr.htmlEscape(message);
    htmlMessage = htmlMessage.replace(/\n/g, '<br>');

    if (anchor) {
      htmlMessage = anchor + htmlMessage;
    }
    html.push('<div style="', style, '">', htmlMessage, '</div>');
  }
};

/**
 * Writes the errors into a DIV as the first child of the BODY, so the BODY must
 * be available when this is run.
 */
plovr.writeErrors = function() {
  // TODO(bolinfest): Make it possible to expand and collapse errors.

  var div = document.createElement('div');
  var html = [];
  
  plovr.writeErrors_(plovr.errors_, html, plovr.ERROR_STYLE);
  plovr.writeErrors_(plovr.warnings_, html, plovr.WARNING_STYLE);

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
