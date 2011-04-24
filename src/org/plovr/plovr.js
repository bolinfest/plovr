(function() {

var plovr = plovr || {};

/** @typedef {input:string,message:string,isError:boolean,lineNumber:number} */
plovr.CompilationError;

plovr.htmlEscape = function(str) {
  return str.replace(/[&<"]/g, function(ch) {
    switch (ch) {
      case '&': return '&amp;';
      case '<': return '&lt;';
      case '"': return '&quot;';
    }
  });
};

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

/**
 * @type {Array.<plovr.CompilationError>}
 */
plovr.errors_ = [];

/**
 * @param {Array.<plovr.CompilationError>} errors
 */
plovr.addErrors = function(errors) {
  for (var i = 0; i < errors.length; i++) plovr.errors_.push(errors[i]);
};

/**
 * @type {Array.<plovr.CompilationError>}
 */
plovr.warnings_ = [];

/**
 * @param {Array.<plovr.CompilationError>} errors
 */
plovr.addWarnings = function(warnings) {
  for (var i = 0; i < warnings.length; i++) plovr.warnings_.push(warnings[i]);
};

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

/** @type {string} */
plovr.viewSourceUrl_ = '';

/** @param {string} viewSourceUrl */
plovr.setViewSourceUrl = function(viewSourceUrl) {
  plovr.viewSourceUrl_ = viewSourceUrl;
};

/** @return {string} */
plovr.getViewSourceUrl = function() {
  return plovr.viewSourceUrl_;
};

/**
 * @param {Array.<plovr.CompilationError>} errors
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
      anchor = '<a target="_blank" ' +
          'href="' + plovr.getViewSourceUrl() +
          '?id=' + encodeURIComponent(plovr.getConfigId()) +
          '&name=' + encodeURIComponent(error['input']) +
          '#' + error['lineNumber'] + '">' +
          plovr.htmlEscape(prefix) +
          '</a>';
    } else if (error['input']) {
      // This is likely a Soy error, which is less likely to include line
      // numbers or the clear ERROR/WARNING text provided by the Compiler.
      anchor = '<a target="_blank" ' +
          'href="' + plovr.getViewSourceUrl() +
          '?id=' + encodeURIComponent(plovr.getConfigId()) +
          '&name=' + encodeURIComponent(error['input']) + '">' +
          plovr.htmlEscape(error['input']) +
          ':</a> ' +
          (style == plovr.ERROR_STYLE ? 'ERROR' : 'WARNING') +
          ' - ';
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

  // Give div its own CSS class (plovr-error-report) so that users can style it.
  // For example, they may need to change the position or the z-index:
  //
  // http://code.google.com/p/plovr/issues/detail?id=34
  //
  // A class is used rather than an id in case there are multiple plovr configs
  // loaded on the same page. An additional CSS class is included (parameterized
  // by config id) so that multiple config error boxes can be styled separately.
  var configId = plovr.getConfigId();
  div.className = 'plovr-error-report plovr-error-report-config-id-' + configId;

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
